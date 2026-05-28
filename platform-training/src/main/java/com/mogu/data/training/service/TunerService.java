package com.mogu.data.training.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.dto.ExperimentDTO;
import com.mogu.data.common.entity.Dataset;
import com.mogu.data.common.entity.DatasetVersion;
import com.mogu.data.common.entity.HyperparameterTrial;
import com.mogu.data.common.entity.HyperparameterTuningJob;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.registry.MlflowRegistryService;
import com.mogu.data.common.repository.DatasetRepository;
import com.mogu.data.common.repository.DatasetVersionRepository;
import com.mogu.data.common.repository.HyperparameterTrialRepository;
import com.mogu.data.common.repository.HyperparameterTuningJobRepository;
import com.mogu.data.common.storage.MinioService;
import com.mogu.data.deployment.service.K8sOperations;
import com.mogu.data.training.entity.TrainingConfig;
import io.kubernetes.client.openapi.models.V1Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 超参数调优服务
 * 支持状态持久化和实时进度追踪
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TunerService {

    private final LightGBMTrainer lightGBMTrainer;
    private final XGBoostTrainer xgBoostTrainer;
    private final MlflowRegistryService mlflowRegistryService;
    private final K8sOperations k8sOperations;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final MinioService minioService;
    private final HyperparameterTuningJobRepository tuningJobRepository;
    private final HyperparameterTrialRepository trialRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 内存缓存（用于前端快速轮询）
    private final ConcurrentHashMap<Long, HyperparameterTuningJob> tuningJobCache = new ConcurrentHashMap<>();

    // K8s 训练配置
    @Value("${training.k8s.enabled:true}")
    private boolean k8sTrainingEnabled;

    @Value("${training.k8s.namespace:mmodelx-training}")
    private String k8sNamespace;

    @Value("${training.k8s.image:mmodelx/trainer:latest}")
    private String k8sTrainingImage;

    @Value("${training.k8s.cpu-request:1}")
    private String k8sCpuRequest;

    @Value("${training.k8s.memory-request:2Gi}")
    private String k8sMemoryRequest;

    @Value("${training.k8s.cpu-limit:2}")
    private String k8sCpuLimit;

    @Value("${training.k8s.memory-limit:4Gi}")
    private String k8sMemoryLimit;

    @Value("${training.k8s.active-deadline-seconds:3600}")
    private int k8sActiveDeadlineSeconds;

    @Value("${training.k8s.minio-endpoint:http://host.docker.internal:9002}")
    private String k8sMinioEndpoint;

    @Value("${minio.access-key:minioadmin}")
    private String minioAccessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String minioSecretKey;

    // ========== 调优任务管理 ==========

    /**
     * 提交调优任务（同步，立即返回任务ID）
     */
    @Transactional
    public Long submitTuningJob(TrainingConfig config) {
        if (config.getOptunaConfig() == null) {
            throw new BusinessException("缺少调优配置 (optunaConfig)");
        }

        TrainingConfig.OptunaConfig optunaConfig = config.getOptunaConfig();

        HyperparameterTuningJob job = new HyperparameterTuningJob();
        job.setExperimentName(config.getExperimentName());
        job.setDatasetName(config.getDatasetName());
        job.setDatasetVersion(config.getDatasetVersion());
        job.setModelType(config.getModel().getType());
        job.setStatus(HyperparameterTuningJob.JobStatus.PENDING);
        job.setNTrials(optunaConfig.getNTrials());
        job.setCurrentTrial(0);
        job.setMetric(optunaConfig.getMetric());
        job.setDirection(optunaConfig.getDirection());

        HyperparameterTuningJob saved = tuningJobRepository.save(job);
        tuningJobCache.put(saved.getId(), saved);

        log.info("调优任务已提交, ID: {}, experiment: {}", saved.getId(), config.getExperimentName());
        return saved.getId();
    }

    /**
     * 执行调优（异步）
     */
    @Async
    public void executeTuning(Long tuningJobId, TrainingConfig config) {
        log.info("开始异步调优, 任务ID: {}", tuningJobId);
        long totalStartTime = System.currentTimeMillis();

        HyperparameterTuningJob job = getTuningJob(tuningJobId);
        if (job == null) {
            log.error("调优任务不存在: {}", tuningJobId);
            return;
        }

        TrainingConfig.OptunaConfig optunaConfig = config.getOptunaConfig();
        double bestScore = optunaConfig.getDirection().equals("minimize") ? Double.MAX_VALUE : 0.0;
        Map<String, Object> bestParams = new HashMap<>();
        Map<String, Object> bestMetrics = new HashMap<>();
        String bestModelPath = null;

        try {
            // 1. 更新状态为运行中
            updateTuningJobStatus(tuningJobId, HyperparameterTuningJob.JobStatus.RUNNING, null, null, null, null, null);

            // 2. 创建实验
            ExperimentDTO experiment = ExperimentDTO.of(
                    config.getExperimentName(),
                    "Hyperparameter tuning",
                    config.getDatasetVersion(),
                    config.getModel().getType()
            );
            mlflowRegistryService.createExperiment(experiment);

            // 3. 循环执行 trial
            for (int i = 0; i < optunaConfig.getNTrials(); i++) {
                long trialStartTime = System.currentTimeMillis();
                updateTuningJobCurrentTrial(tuningJobId, i);

                Map<String, Object> trialParams = generateTrialParams(config.getModel().getType(), optunaConfig.getParamRanges());
                TrainingConfig trialConfig = cloneConfig(config);
                trialConfig.getModel().setParams(trialParams);

                // 创建 trial 记录
                Long trialId = createTrial(tuningJobId, i, trialParams);
                updateTrialStatus(trialId, HyperparameterTrial.TrialStatus.RUNNING, null, null);

                Map<String, Object> metrics;
                String modelPath = null;

                try {
                    if (k8sTrainingEnabled) {
                        log.info("Trial {}/{}: 使用 K8s 执行训练", i + 1, optunaConfig.getNTrials());
                        Map<String, Object> trialResult = executeTrialK8s(trialConfig, i);
                        modelPath = (String) trialResult.remove("__modelPath");
                        metrics = trialResult;
                    } else {
                        modelPath = getTrainer(config.getModel().getType()).train(trialConfig);
                        metrics = getTrainer(config.getModel().getType()).validate(modelPath, trialConfig);
                    }

                    double score = ((Number) metrics.get(optunaConfig.getMetric())).doubleValue();

                    // 记录 trial 结果
                    long trialElapsed = System.currentTimeMillis() - trialStartTime;
                    updateTrialResult(trialId, metrics, score, trialElapsed);

                    // 记录到 MLflow
                    mlflowRegistryService.logParams(config.getExperimentName() + "_trial_" + i, trialParams);
                    @SuppressWarnings("unchecked")
                    Map<String, Double> metricsDouble = (Map<String, Double>) (Object) metrics;
                    mlflowRegistryService.logMetrics(config.getExperimentName() + "_trial_" + i, metricsDouble);

                    // 更新最佳参数
                    boolean isBetter = optunaConfig.getDirection().equals("minimize")
                            ? score < bestScore
                            : score > bestScore;

                    if (isBetter) {
                        bestScore = score;
                        bestParams = new HashMap<>(trialParams);
                        bestMetrics = new HashMap<>(metrics);
                        bestModelPath = modelPath;
                        updateTrialIsBest(trialId, true);
                        // 更新 job 的最佳参数
                        updateTuningJobBestParams(tuningJobId, bestScore, bestParams);
                        log.info("Trial {}/{}: 新的最佳 {} = {}, 参数: {}",
                                i + 1, optunaConfig.getNTrials(), optunaConfig.getMetric(), score, bestParams);
                    } else {
                        log.info("Trial {}/{}: {} = {}, 未改善",
                                i + 1, optunaConfig.getNTrials(), optunaConfig.getMetric(), score);
                    }

                } catch (Exception e) {
                    log.error("Trial {}/{} 失败: {}", i + 1, optunaConfig.getNTrials(), e.getMessage());
                    updateTrialStatus(trialId, HyperparameterTrial.TrialStatus.FAILED, e.getMessage(), null);
                }
            }

            // 4. 使用最佳参数再次训练并注册模型
            log.info("使用最佳参数执行最终训练");
            TrainingConfig bestConfig = cloneConfig(config);
            bestConfig.getModel().setParams(bestParams);

            String finalModelPath;
            Map<String, Object> finalMetrics;

            if (k8sTrainingEnabled) {
                Map<String, Object> bestResult = executeTrialK8s(bestConfig, -1);
                finalModelPath = (String) bestResult.remove("__modelPath");
                finalMetrics = bestResult;
            } else {
                finalModelPath = getTrainer(config.getModel().getType()).train(bestConfig);
                finalMetrics = getTrainer(config.getModel().getType()).validate(finalModelPath, bestConfig);
            }

            mlflowRegistryService.logParams(config.getExperimentName(), bestParams);
            @SuppressWarnings("unchecked")
            Map<String, Double> finalMetricsDouble = (Map<String, Double>) (Object) finalMetrics;
            mlflowRegistryService.logMetrics(config.getExperimentName(), finalMetricsDouble);
            mlflowRegistryService.logModel(config.getExperimentName(), finalModelPath);
            mlflowRegistryService.endExperiment(config.getExperimentName(), "COMPLETED");

            // 5. 完成任务
            long totalElapsed = System.currentTimeMillis() - totalStartTime;
            updateTuningJobStatus(tuningJobId, HyperparameterTuningJob.JobStatus.SUCCESS,
                    optunaConfig.getNTrials(), bestScore, objectMapper.valueToTree(bestParams),
                    objectMapper.valueToTree(finalMetrics), finalModelPath);
            updateTuningJobElapsedTime(tuningJobId, totalElapsed);

            log.info("超参数调优完成, ID: {}, 最佳参数: {}, 最佳得分: {}",
                    tuningJobId, bestParams, bestScore);

        } catch (Exception e) {
            log.error("超参数调优失败, ID: {}, 错误: {}", tuningJobId, e.getMessage(), e);
            mlflowRegistryService.endExperiment(config.getExperimentName(), "FAILED");
            updateTuningJobStatus(tuningJobId, HyperparameterTuningJob.JobStatus.FAILED, null, null, null, null, null);
            updateTuningJobError(tuningJobId, e.getMessage());
        }
    }

    // ========== 查询方法 ==========

    public HyperparameterTuningJob getTuningJob(Long tuningJobId) {
        HyperparameterTuningJob cached = tuningJobCache.get(tuningJobId);
        if (cached != null) {
            return cached;
        }
        Optional<HyperparameterTuningJob> opt = tuningJobRepository.findById(tuningJobId);
        return opt.orElse(null);
    }

    public List<HyperparameterTuningJob> listTuningJobs() {
        return tuningJobRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<HyperparameterTrial> listTrials(Long tuningJobId) {
        return trialRepository.findByTuningJobIdOrderByTrialIndexAsc(tuningJobId);
    }

    // ========== 内部方法：Trial 管理 ==========

    private Long createTrial(Long tuningJobId, int trialIndex, Map<String, Object> params) {
        try {
            HyperparameterTrial trial = new HyperparameterTrial();
            trial.setTuningJobId(tuningJobId);
            trial.setTrialIndex(trialIndex);
            trial.setParams(objectMapper.valueToTree(params));
            trial.setStatus(HyperparameterTrial.TrialStatus.PENDING);
            HyperparameterTrial saved = trialRepository.save(trial);
            return saved.getId();
        } catch (Exception e) {
            log.error("创建 trial 记录失败: {}", e.getMessage());
            return null;
        }
    }

    private void updateTrialStatus(Long trialId, HyperparameterTrial.TrialStatus status, String errorMessage, Long elapsedTimeMs) {
        if (trialId == null) return;
        try {
            Optional<HyperparameterTrial> opt = trialRepository.findById(trialId);
            if (opt.isPresent()) {
                HyperparameterTrial trial = opt.get();
                trial.setStatus(status);
                if (errorMessage != null) trial.setErrorMessage(errorMessage);
                if (elapsedTimeMs != null) trial.setElapsedTimeMs(elapsedTimeMs);
                trialRepository.save(trial);
            }
        } catch (Exception e) {
            log.warn("更新 trial 状态失败: {}", e.getMessage());
        }
    }

    private void updateTrialResult(Long trialId, Map<String, Object> metrics, Double score, Long elapsedTimeMs) {
        if (trialId == null) return;
        try {
            Optional<HyperparameterTrial> opt = trialRepository.findById(trialId);
            if (opt.isPresent()) {
                HyperparameterTrial trial = opt.get();
                trial.setStatus(HyperparameterTrial.TrialStatus.SUCCESS);
                trial.setMetrics(objectMapper.valueToTree(metrics));
                trial.setScore(score);
                trial.setElapsedTimeMs(elapsedTimeMs);
                trialRepository.save(trial);
            }
        } catch (Exception e) {
            log.warn("更新 trial 结果失败: {}", e.getMessage());
        }
    }

    private void updateTrialIsBest(Long trialId, boolean isBest) {
        if (trialId == null) return;
        try {
            Optional<HyperparameterTrial> opt = trialRepository.findById(trialId);
            if (opt.isPresent()) {
                HyperparameterTrial trial = opt.get();
                trial.setIsBest(isBest);
                trialRepository.save(trial);
            }
        } catch (Exception e) {
            log.warn("更新 trial isBest 失败: {}", e.getMessage());
        }
    }

    // ========== 内部方法：Job 状态更新 ==========

    private void updateTuningJobStatus(Long tuningJobId, HyperparameterTuningJob.JobStatus status,
                                       Integer currentTrial, Double bestScore, JsonNode bestParams,
                                       JsonNode finalMetrics, String modelPath) {
        try {
            Optional<HyperparameterTuningJob> opt = tuningJobRepository.findById(tuningJobId);
            if (!opt.isPresent()) return;
            HyperparameterTuningJob job = opt.get();
            job.setStatus(status);
            if (currentTrial != null) job.setCurrentTrial(currentTrial);
            if (bestScore != null) job.setBestScore(bestScore);
            if (bestParams != null) job.setBestParams(bestParams);
            if (finalMetrics != null) job.setFinalMetrics(finalMetrics);
            if (modelPath != null) job.setModelPath(modelPath);
            if (status == HyperparameterTuningJob.JobStatus.RUNNING && job.getStartedAt() == null) {
                job.setStartedAt(LocalDateTime.now());
            }
            if (status == HyperparameterTuningJob.JobStatus.SUCCESS || status == HyperparameterTuningJob.JobStatus.FAILED) {
                job.setCompletedAt(LocalDateTime.now());
            }
            tuningJobRepository.save(job);
            tuningJobCache.put(tuningJobId, job);
        } catch (Exception e) {
            log.warn("更新调优任务状态失败: {}", e.getMessage());
        }
    }

    private void updateTuningJobCurrentTrial(Long tuningJobId, int currentTrial) {
        try {
            Optional<HyperparameterTuningJob> opt = tuningJobRepository.findById(tuningJobId);
            if (opt.isPresent()) {
                HyperparameterTuningJob job = opt.get();
                job.setCurrentTrial(currentTrial);
                tuningJobRepository.save(job);
                tuningJobCache.put(tuningJobId, job);
            }
        } catch (Exception e) {
            log.warn("更新当前 trial 失败: {}", e.getMessage());
        }
    }

    private void updateTuningJobBestParams(Long tuningJobId, Double bestScore, Map<String, Object> bestParams) {
        try {
            Optional<HyperparameterTuningJob> opt = tuningJobRepository.findById(tuningJobId);
            if (opt.isPresent()) {
                HyperparameterTuningJob job = opt.get();
                job.setBestScore(bestScore);
                job.setBestParams(objectMapper.valueToTree(bestParams));
                tuningJobRepository.save(job);
                tuningJobCache.put(tuningJobId, job);
            }
        } catch (Exception e) {
            log.warn("更新最佳参数失败: {}", e.getMessage());
        }
    }

    private void updateTuningJobElapsedTime(Long tuningJobId, Long elapsedTimeMs) {
        try {
            Optional<HyperparameterTuningJob> opt = tuningJobRepository.findById(tuningJobId);
            if (opt.isPresent()) {
                HyperparameterTuningJob job = opt.get();
                job.setElapsedTimeMs(elapsedTimeMs);
                tuningJobRepository.save(job);
                tuningJobCache.put(tuningJobId, job);
            }
        } catch (Exception e) {
            log.warn("更新耗时失败: {}", e.getMessage());
        }
    }

    private void updateTuningJobError(Long tuningJobId, String errorMessage) {
        try {
            Optional<HyperparameterTuningJob> opt = tuningJobRepository.findById(tuningJobId);
            if (opt.isPresent()) {
                HyperparameterTuningJob job = opt.get();
                job.setErrorMessage(errorMessage);
                tuningJobRepository.save(job);
                tuningJobCache.put(tuningJobId, job);
            }
        } catch (Exception e) {
            log.warn("更新错误信息失败: {}", e.getMessage());
        }
    }

    // ========== K8s Trial 执行 ==========

    private Map<String, Object> executeTrialK8s(TrainingConfig config, int trialIndex) throws Exception {
        String jobName = "tune-" + System.currentTimeMillis() + "-" + (trialIndex >= 0 ? trialIndex : "best");
        String experimentName = config.getExperimentName() + (trialIndex >= 0 ? "_trial_" + trialIndex : "_best");
        String modelType = config.getModel().getType();
        String modelExt = modelType.equals("lightgbm") ? "txt" : "json";

        List<Dataset> datasets = datasetRepository.findByName(config.getDatasetName());
        if (datasets.isEmpty()) {
            throw new BusinessException("数据集不存在: " + config.getDatasetName());
        }
        Long datasetId = datasets.get(0).getId();
        Optional<DatasetVersion> dvOpt = datasetVersionRepository.findByDatasetIdAndVersion(
                datasetId, config.getDatasetVersion());
        if (!dvOpt.isPresent()) {
            dvOpt = datasetVersionRepository.findTopByDatasetIdOrderByCreatedAtDesc(datasetId);
        }
        DatasetVersion datasetVersion = dvOpt.orElseThrow(() ->
                new BusinessException("数据集版本不存在: " + config.getDatasetName() + "@" + config.getDatasetVersion()));

        Map<String, String> envVars = new HashMap<>();
        envVars.put("MINIO_ENDPOINT", k8sMinioEndpoint);
        envVars.put("MINIO_ACCESS_KEY", minioAccessKey);
        envVars.put("MINIO_SECRET_KEY", minioSecretKey);
        envVars.put("MINIO_SECURE", "false");
        envVars.put("DATASET_NAME", config.getDatasetName());
        envVars.put("DATASET_VERSION", config.getDatasetVersion());
        envVars.put("MODEL_TYPE", modelType);
        envVars.put("EXPERIMENT_NAME", experimentName);
        envVars.put("PARAMS_JSON", objectMapper.writeValueAsString(config.getModel().getParams()));
        envVars.put("LABEL_COL", resolveLabelColumn(config));

        if (datasetVersion.getTrainPath() != null) envVars.put("TRAIN_PATH", datasetVersion.getTrainPath());
        if (datasetVersion.getValPath() != null) envVars.put("VAL_PATH", datasetVersion.getValPath());
        if (datasetVersion.getTestPath() != null) envVars.put("TEST_PATH", datasetVersion.getTestPath());

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String outputPath = String.format("models/%s/%s/%s/model.%s", modelType, config.getExperimentName(), dateStr, modelExt);
        String metricsPath = String.format("models/%s/%s/%s/metrics.json", modelType, config.getExperimentName(), dateStr);

        envVars.put("OUTPUT_PATH", "/tmp/model." + modelExt);
        envVars.put("METRICS_OUTPUT_PATH", "/tmp/metrics.json");
        envVars.put("OUTPUT_MINIO_PATH", outputPath);
        envVars.put("METRICS_MINIO_PATH", metricsPath);

        Map<String, String> labels = new HashMap<>();
        labels.put("tune-trial", String.valueOf(trialIndex));
        labels.put("experiment", config.getExperimentName());

        k8sOperations.createTrainingJob(
                k8sNamespace, jobName, k8sTrainingImage, envVars,
                k8sCpuRequest, k8sMemoryRequest, k8sCpuLimit, k8sMemoryLimit,
                k8sActiveDeadlineSeconds, labels
        );

        pollK8sJob(jobName);

        Map<String, Object> metrics = downloadMetricsFromMinio(metricsPath);
        metrics.put("__modelPath", outputPath);
        return metrics;
    }

    private void pollK8sJob(String jobName) throws Exception {
        int pollIntervalMs = 5000;
        int maxPolls = k8sActiveDeadlineSeconds * 1000 / pollIntervalMs;

        for (int i = 0; i < maxPolls; i++) {
            Thread.sleep(pollIntervalMs);
            V1Job job = k8sOperations.getJobStatus(k8sNamespace, jobName);
            if (job == null) {
                throw new BusinessException("K8s Job 异常消失: " + jobName);
            }
            Integer succeeded = job.getStatus().getSucceeded();
            Integer failed = job.getStatus().getFailed();
            if (succeeded != null && succeeded > 0) return;
            if (failed != null && failed > 0) {
                String logs = k8sOperations.getJobPodLogs(k8sNamespace, jobName, 200);
                String detail = logs != null ? logs.substring(0, Math.min(logs.length(), 500)) : "未知错误";
                throw new BusinessException("K8s 调优失败: " + detail);
            }
        }
        throw new BusinessException("K8s 调优 Job 轮询超时");
    }

    private Map<String, Object> downloadMetricsFromMinio(String metricsPath) throws Exception {
        try {
            String[] parts = metricsPath.split("/", 2);
            if (parts.length == 2) {
                java.io.InputStream is = minioService.downloadFile(parts[0], parts[1]);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) baos.write(buffer, 0, len);
                is.close();
                String content = new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> metrics = objectMapper.readValue(content, Map.class);
                return metrics;
            }
        } catch (Exception e) {
            log.warn("尝试下载指标失败: {}", metricsPath);
        }
        Map<String, Object> empty = new HashMap<>();
        empty.put("auc", 0.0);
        return empty;
    }

    private String resolveLabelColumn(TrainingConfig config) {
        String datasetName = config.getDatasetName();
        if (datasetName != null && !datasetName.isEmpty()) {
            List<Dataset> datasets = datasetRepository.findByName(datasetName);
            if (!datasets.isEmpty() && datasets.get(0).getLabelColumn() != null) {
                return datasets.get(0).getLabelColumn();
            }
        }
        return "is_churned";
    }

    private Map<String, Object> generateTrialParams(String modelType, List<TrainingConfig.ParamRange> paramRanges) {
        Map<String, Object> params = new HashMap<>();

        // 如果传入了自定义参数范围，使用自定义范围
        if (paramRanges != null && !paramRanges.isEmpty()) {
            for (TrainingConfig.ParamRange range : paramRanges) {
                if (Boolean.FALSE.equals(range.getEnabled())) {
                    continue;
                }
                String name = range.getName();
                String type = range.getType();
                if ("categorical".equals(type) && range.getChoices() != null && !range.getChoices().isEmpty()) {
                    List<String> choices = range.getChoices();
                    params.put(name, choices.get((int) (Math.random() * choices.size())));
                } else if ("int".equals(type)) {
                    int min = range.getMin() != null ? range.getMin().intValue() : 0;
                    int max = range.getMax() != null ? range.getMax().intValue() : 100;
                    params.put(name, min + (int) (Math.random() * (max - min + 1)));
                } else if ("float".equals(type)) {
                    double min = range.getMin() != null ? range.getMin() : 0.0;
                    double max = range.getMax() != null ? range.getMax() : 1.0;
                    params.put(name, min + Math.random() * (max - min));
                }
            }
            return params;
        }

        // 默认参数范围
        if ("lightgbm".equals(modelType)) {
            params.put("num_leaves", 31 + (int) (Math.random() * 96));
            params.put("learning_rate", 0.01 + Math.random() * 0.19);
            params.put("feature_fraction", 0.5 + Math.random() * 0.5);
            params.put("bagging_fraction", 0.5 + Math.random() * 0.5);
            params.put("bagging_freq", 1 + (int) (Math.random() * 10));
        } else if ("xgboost".equals(modelType)) {
            params.put("max_depth", 3 + (int) (Math.random() * 10));
            params.put("learning_rate", 0.01 + Math.random() * 0.19);
            params.put("subsample", 0.5 + Math.random() * 0.5);
            params.put("colsample_bytree", 0.5 + Math.random() * 0.5);
        }
        return params;
    }

    private TrainingConfig cloneConfig(TrainingConfig config) {
        TrainingConfig cloned = new TrainingConfig();
        cloned.setExperimentName(config.getExperimentName());
        cloned.setDatasetName(config.getDatasetName());
        cloned.setDatasetVersion(config.getDatasetVersion());
        TrainingConfig.ModelConfig modelConfig = new TrainingConfig.ModelConfig();
        modelConfig.setType(config.getModel().getType());
        modelConfig.setObjective(config.getModel().getObjective());
        if (config.getModel().getParams() != null) {
            modelConfig.setParams(new HashMap<>(config.getModel().getParams()));
        }
        cloned.setModel(modelConfig);
        if (config.getTrainingParams() != null) {
            TrainingConfig.TrainingParams tp = new TrainingConfig.TrainingParams();
            tp.setNumRounds(config.getTrainingParams().getNumRounds());
            tp.setEarlyStoppingRounds(config.getTrainingParams().getEarlyStoppingRounds());
            tp.setValidationFraction(config.getTrainingParams().getValidationFraction());
            tp.setCrossValidationFolds(config.getTrainingParams().getCrossValidationFolds());
            tp.setBatchSize(config.getTrainingParams().getBatchSize());
            tp.setLearningRate(config.getTrainingParams().getLearningRate());
            cloned.setTrainingParams(tp);
        }
        cloned.setOptunaConfig(config.getOptunaConfig());
        cloned.setMetrics(config.getMetrics());
        return cloned;
    }

    private Trainer getTrainer(String modelType) {
        switch (modelType) {
            case "lightgbm": return lightGBMTrainer;
            case "xgboost": return xgBoostTrainer;
            default: throw new BusinessException("不支持的模型类型: " + modelType);
        }
    }
}
