package com.mogu.data.training.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.Dataset;
import com.mogu.data.common.entity.DatasetVersion;
import com.mogu.data.common.entity.TrainingJob;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.registry.MlflowRegistryService;
import com.mogu.data.common.repository.DatasetRepository;
import com.mogu.data.common.repository.DatasetVersionRepository;
import com.mogu.data.common.repository.TrainingJobRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 训练任务调度服务
 * 支持异步训练任务执行、状态跟踪（本地进程或 K8s Job）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingJobService {

    private final TrainingJobRepository trainingJobRepository;
    private final MlflowRegistryService mlflowRegistryService;
    private final MinioService minioService;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final LightGBMTrainer lightGBMTrainer;
    private final XGBoostTrainer xgBoostTrainer;
    private final K8sOperations k8sOperations;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Value("${minio.endpoint:http://localhost:9002}")
    private String minioEndpoint;

    @Value("${minio.access-key:minioadmin}")
    private String minioAccessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String minioSecretKey;

    @Value("${training.k8s.minio-endpoint:http://host.docker.internal:9002}")
    private String k8sMinioEndpoint;

    // 内存中的训练进度缓存（用于前端快速轮询）
    private final ConcurrentHashMap<Long, TrainingJob> jobProgressCache = new ConcurrentHashMap<>();

    /**
     * 提交训练任务
     */
    @Transactional
    public Long submitTrainingJob(TrainingConfig config) {
        log.info("提交训练任务: {}", config.getExperimentName());

        TrainingJob job = new TrainingJob();
        job.setJobName(config.getExperimentName() + "_" + UUID.randomUUID().toString().substring(0, 8));
        job.setExperimentName(config.getExperimentName());
        job.setDatasetName(config.getDatasetName());
        job.setDatasetVersion(config.getDatasetVersion());
        job.setModelType(config.getModel().getType());
        job.setStatus(TrainingJob.JobStatus.PENDING);
        job.setProgress(0);
        job.setTotalSteps(5);
        job.setCurrentStep("等待中");

        if (config.getModel().getParams() != null) {
            job.setHyperparameters(objectMapper.valueToTree(config.getModel().getParams()));
        }

        TrainingJob saved = trainingJobRepository.save(job);
        jobProgressCache.put(saved.getId(), saved);

        log.info("训练任务已提交, ID: {}", saved.getId());
        return saved.getId();
    }

    /**
     * 重新训练（基于已有任务的配置）
     */
    @Transactional
    public Long retryTrainingJob(Long jobId) {
        log.info("重新训练, 原任务ID: {}", jobId);

        TrainingJob originalJob = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("训练任务不存在: " + jobId));

        if (originalJob.getDatasetName() == null || originalJob.getDatasetName().isEmpty()) {
            throw new BusinessException("该任务缺少数据集信息，无法重新训练，请新建训练任务");
        }

        TrainingConfig config = new TrainingConfig();
        config.setExperimentName(originalJob.getExperimentName());
        config.setDatasetName(originalJob.getDatasetName());
        config.setDatasetVersion(originalJob.getDatasetVersion());

        TrainingConfig.ModelConfig modelConfig = new TrainingConfig.ModelConfig();
        modelConfig.setType(originalJob.getModelType());
        if (originalJob.getHyperparameters() != null) {
            modelConfig.setParams(objectMapper.convertValue(originalJob.getHyperparameters(), Map.class));
        }
        config.setModel(modelConfig);

        return submitTrainingJob(config);
    }

    /**
     * 执行训练任务（异步）
     */
    @Async
    public void executeTraining(Long jobId, TrainingConfig config) {
        log.info("开始异步训练任务: {}", jobId);
        long startTime = System.currentTimeMillis();

        try {
            // 1. 更新任务状态为运行中
            updateJobStatus(jobId, TrainingJob.JobStatus.RUNNING, 10, "创建实验");

            // 2. 创建实验
            com.mogu.data.common.dto.ExperimentDTO experiment = com.mogu.data.common.dto.ExperimentDTO.of(
                    config.getExperimentName(),
                    "Auto generated experiment",
                    config.getDatasetVersion(),
                    config.getModel().getType()
            );
            Long experimentId = mlflowRegistryService.createExperiment(experiment);

            // 3. 记录参数
            updateJobStatus(jobId, null, 20, "记录训练参数");
            mlflowRegistryService.logParams(config.getExperimentName(), config.getModel().getParams());

            // 4. 训练模型
            updateJobStatus(jobId, null, 30, "训练模型");

            String modelPath;
            Map<String, Object> metrics;

            if (k8sTrainingEnabled) {
                // K8s 模式
                modelPath = executeK8sTraining(jobId, config);
                metrics = downloadMetricsFromMinio(config.getExperimentName());
            } else {
                // 本地模式（回退）
                Trainer trainer = getTrainer(config.getModel().getType());
                modelPath = trainer.train(config);
                metrics = trainer.validate(modelPath, config);
            }

            // 5. 记录指标
            updateJobStatus(jobId, null, 60, "验证模型");
            @SuppressWarnings("unchecked")
            Map<String, Double> metricsDouble = (Map<String, Double>) (Object) metrics;
            mlflowRegistryService.logMetrics(config.getExperimentName(), metricsDouble);
            mlflowRegistryService.logModel(config.getExperimentName(), modelPath);

            // 6. 注册模型
            updateJobStatus(jobId, null, 80, "注册模型");
            String version = "v" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            com.mogu.data.common.dto.ModelDTO model = com.mogu.data.common.dto.ModelDTO.of(
                    config.getExperimentName(),
                    String.valueOf(experimentId),
                    config.getModel().getType(),
                    modelPath,
                    metricsDouble.get("auc")
            );
            model.setVersion(version);
            mlflowRegistryService.registerModel(model);

            // 7. 完成实验
            updateJobStatus(jobId, null, 90, "完成实验");
            mlflowRegistryService.endExperiment(config.getExperimentName(), "COMPLETED");

            // 8. 更新任务状态为成功
            long elapsed = System.currentTimeMillis() - startTime;
            finishJob(jobId, TrainingJob.JobStatus.SUCCESS, 100, "训练完成", modelPath, version,
                    objectMapper.valueToTree(metrics), elapsed);

            log.info("训练任务完成: {}, 耗时: {}ms", jobId, elapsed);

        } catch (Exception e) {
            log.error("训练任务失败: {}, 错误: {}", jobId, e.getMessage(), e);
            mlflowRegistryService.endExperiment(config.getExperimentName(), "FAILED");
            long elapsed = System.currentTimeMillis() - startTime;
            finishJob(jobId, TrainingJob.JobStatus.FAILED, null, null, null, null, null, elapsed);
            updateJobError(jobId, e.getMessage());
        } finally {
            // 清理 K8s Job
            if (k8sTrainingEnabled) {
                try {
                    k8sOperations.deleteJob(k8sNamespace, "train-" + jobId);
                } catch (Exception e) {
                    log.warn("清理 K8s Job 失败: {}", e.getMessage());
                }
            }
        }
    }

    // ========== K8s 训练 ==========

    private String executeK8sTraining(Long jobId, TrainingConfig config) throws Exception {
        String jobName = "train-" + jobId;
        String experimentName = config.getExperimentName();
        String modelType = config.getModel().getType();
        String modelExt = modelType.equals("lightgbm") ? "txt" : "json";

        // 查找数据集版本，获取实际 MinIO 路径
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

        // 构建环境变量（使用 K8s 可访问的 MinIO 地址）
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

        // 传递实际 MinIO 路径
        if (datasetVersion.getTrainPath() != null) {
            envVars.put("TRAIN_PATH", datasetVersion.getTrainPath());
        }
        if (datasetVersion.getValPath() != null) {
            envVars.put("VAL_PATH", datasetVersion.getValPath());
        }
        if (datasetVersion.getTestPath() != null) {
            envVars.put("TEST_PATH", datasetVersion.getTestPath());
        }

        String outputPath = String.format("models/%s/%s/model.%s", modelType, experimentName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        String metricsPath = String.format("models/%s/%s/metrics.json", modelType, experimentName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        envVars.put("OUTPUT_PATH", "/tmp/model." + modelExt);
        envVars.put("METRICS_OUTPUT_PATH", "/tmp/metrics.json");
        envVars.put("OUTPUT_MINIO_PATH", outputPath);
        envVars.put("METRICS_MINIO_PATH", metricsPath);

        Map<String, String> labels = new HashMap<>();
        labels.put("job-id", String.valueOf(jobId));
        labels.put("experiment", experimentName);

        // 创建 K8s Job
        log.info("提交 K8s 训练 Job: {}, image={}", jobName, k8sTrainingImage);
        k8sOperations.createTrainingJob(
                k8sNamespace,
                jobName,
                k8sTrainingImage,
                envVars,
                k8sCpuRequest,
                k8sMemoryRequest,
                k8sCpuLimit,
                k8sMemoryLimit,
                k8sActiveDeadlineSeconds,
                labels
        );

        // 轮询 Job 状态
        return pollK8sJob(jobId, jobName, outputPath);
    }

    private String pollK8sJob(Long jobId, String jobName, String expectedModelPath) throws Exception {
        int pollIntervalMs = 5000;
        int maxPolls = k8sActiveDeadlineSeconds * 1000 / pollIntervalMs;

        for (int i = 0; i < maxPolls; i++) {
            Thread.sleep(pollIntervalMs);

            V1Job job = k8sOperations.getJobStatus(k8sNamespace, jobName);
            if (job == null) {
                log.warn("K8s Job 不存在: {}", jobName);
                throw new BusinessException("K8s Job 异常消失");
            }

            Integer succeeded = job.getStatus().getSucceeded();
            Integer failed = job.getStatus().getFailed();

            if (succeeded != null && succeeded > 0) {
                log.info("K8s 训练 Job 完成: {}", jobName);
                return expectedModelPath;
            }

            if (failed != null && failed > 0) {
                String logs = k8sOperations.getJobPodLogs(k8sNamespace, jobName, 200);
                log.error("K8s 训练 Job 失败: {}, 日志: {}", jobName, logs);
                String errorDetail = (logs != null) ? logs.substring(0, Math.min(logs.length(), 500)) : "未知错误";
                throw new BusinessException("K8s 训练失败: " + errorDetail);
            }

            // 更新进度（每轮增加一点）
            int progress = Math.min(30 + (i * 2), 55);
            updateJobStatus(jobId, null, progress, "训练中 (K8s)");
        }

        throw new BusinessException("K8s 训练 Job 轮询超时");
    }

    private Map<String, Object> downloadMetricsFromMinio(String experimentName) throws Exception {
        // 从 MinIO 下载指标文件
        String metricsPath = String.format("models/lightgbm/%s/metrics.json", experimentName);
        // 尝试多种路径
        String[] possiblePaths = {
                String.format("models/lightgbm/%s/metrics.json", experimentName),
                String.format("models/xgboost/%s/metrics.json", experimentName),
        };

        for (String path : possiblePaths) {
            try {
                String[] parts = path.split("/", 2);
                if (parts.length == 2) {
                    java.io.InputStream is = minioService.downloadFile(parts[0], parts[1]);
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    is.close();
                    String content = new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metrics = objectMapper.readValue(content, Map.class);
                    return metrics;
                }
            } catch (Exception e) {
                log.debug("尝试下载指标失败: {}", path);
            }
        }

        log.warn("无法从 MinIO 下载训练指标，返回空指标");
        Map<String, Object> empty = new HashMap<>();
        empty.put("auc", 0.0);
        return empty;
    }

    private String resolveLabelColumn(TrainingConfig config) {
        // 默认标签列
        return "is_churned";
    }

    // ========== 查询方法 ==========

    public TrainingJob getJobStatus(Long jobId) {
        TrainingJob cached = jobProgressCache.get(jobId);
        if (cached != null && cached.getStatus() == TrainingJob.JobStatus.RUNNING) {
            return cached;
        }
        return trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("训练任务不存在: " + jobId));
    }

    public List<TrainingJob> listJobs() {
        return trainingJobRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
                )
        );
    }

    public List<TrainingJob> listRunningJobs() {
        return trainingJobRepository.findByStatusInOrderByCreatedAtDesc(
                java.util.Arrays.asList(TrainingJob.JobStatus.PENDING, TrainingJob.JobStatus.RUNNING)
        );
    }

    /**
     * 获取训练任务的日志（K8s Pod 日志）
     */
    public String getJobLogs(Long jobId) {
        log.info("获取训练任务日志: {}", jobId);

        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("训练任务不存在: " + jobId));

        // 优先返回已记录的错误日志（包含 K8s 失败摘要）
        if (job.getErrorMessage() != null && !job.getErrorMessage().isEmpty()) {
            return job.getErrorMessage();
        }

        // 尝试获取 K8s Pod 日志
        if (k8sTrainingEnabled) {
            try {
                String jobName = "train-" + jobId;
                String logs = k8sOperations.getJobPodLogs(k8sNamespace, jobName, 500);
                if (logs != null && !logs.isEmpty()) {
                    return logs;
                }
            } catch (Exception e) {
                log.warn("获取 K8s 日志失败: {}", e.getMessage());
            }
        }

        return "暂无日志记录";
    }

    /**
     * 删除训练任务
     */
    @Transactional
    public void deleteJob(Long jobId) {
        log.info("删除训练任务: {}", jobId);

        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("训练任务不存在: " + jobId));

        // 运行中的任务不允许直接删除
        if (job.getStatus() == TrainingJob.JobStatus.RUNNING || job.getStatus() == TrainingJob.JobStatus.PENDING) {
            throw new BusinessException("运行中或等待中的任务无法删除，请先停止任务");
        }

        trainingJobRepository.deleteById(jobId);
        jobProgressCache.remove(jobId);

        log.info("训练任务已删除: {}", jobId);
    }

    // ========== 内部方法 ==========

    private void updateJobStatus(Long jobId, TrainingJob.JobStatus status, Integer progress, String step) {
        TrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        if (status != null) {
            job.setStatus(status);
            if (status == TrainingJob.JobStatus.RUNNING && job.getStartedAt() == null) {
                job.setStartedAt(LocalDateTime.now());
            }
        }
        if (progress != null) {
            job.setProgress(progress);
        }
        if (step != null) {
            job.setCurrentStep(step);
        }

        trainingJobRepository.save(job);
        jobProgressCache.put(jobId, job);
    }

    private void finishJob(Long jobId, TrainingJob.JobStatus status, Integer progress,
                           String step, String modelPath, String modelVersion,
                           JsonNode metrics, long elapsedTime) {
        TrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        job.setStatus(status);
        if (progress != null) job.setProgress(progress);
        if (step != null) job.setCurrentStep(step);
        if (modelPath != null) job.setModelPath(modelPath);
        if (modelVersion != null) job.setModelVersion(modelVersion);
        if (metrics != null) job.setMetrics(metrics);
        job.setElapsedTimeMs(elapsedTime);
        job.setCompletedAt(LocalDateTime.now());

        trainingJobRepository.save(job);
        jobProgressCache.put(jobId, job);
    }

    private void updateJobError(Long jobId, String errorMessage) {
        TrainingJob job = trainingJobRepository.findById(jobId).orElse(null);
        if (job == null) return;
        job.setErrorMessage(errorMessage);
        trainingJobRepository.save(job);
        jobProgressCache.put(jobId, job);
    }

    private Trainer getTrainer(String modelType) {
        switch (modelType) {
            case "lightgbm":
                return lightGBMTrainer;
            case "xgboost":
                return xgBoostTrainer;
            default:
                throw new IllegalArgumentException("不支持的模型类型: " + modelType);
        }
    }
}
