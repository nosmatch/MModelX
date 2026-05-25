package com.mogu.data.training.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.Experiment;
import com.mogu.data.common.entity.Model;
import com.mogu.data.common.entity.TrainingJob;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.repository.ExperimentRepository;
import com.mogu.data.common.repository.ModelRepository;
import com.mogu.data.common.dto.ExperimentDTO;
import com.mogu.data.common.dto.ModelDTO;
import com.mogu.data.common.registry.MlflowRegistryService;
import com.mogu.data.common.repository.TrainingJobRepository;
import com.mogu.data.training.entity.TrainingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 训练任务调度服务
 * 支持异步训练任务执行、状态跟踪
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingJobService {

    private final TrainingJobRepository trainingJobRepository;
    private final ExperimentRepository experimentRepository;
    private final ModelRepository modelRepository;
    private final MlflowRegistryService mlflowRegistryService;
    private final LightGBMTrainer lightGBMTrainer;
    private final XGBoostTrainer xgBoostTrainer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 内存中的训练进度缓存（用于前端快速轮询）
    private final ConcurrentHashMap<Long, TrainingJob> jobProgressCache = new ConcurrentHashMap<>();

    /**
     * 提交训练任务
     *
     * @param config 训练配置
     * @return 任务ID
     */
    @Transactional
    public Long submitTrainingJob(TrainingConfig config) {
        log.info("提交训练任务: {}", config.getExperimentName());

        // 创建训练任务记录
        TrainingJob job = new TrainingJob();
        job.setJobName(config.getExperimentName() + "_" + UUID.randomUUID().toString().substring(0, 8));
        job.setExperimentName(config.getExperimentName());
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
     * 执行训练任务（异步）
     *
     * @param jobId  任务ID
     * @param config 训练配置
     */
    @Async
    public void executeTraining(Long jobId, TrainingConfig config) {
        log.info("开始异步训练任务: {}", jobId);
        long startTime = System.currentTimeMillis();

        try {
            // 1. 更新任务状态为运行中
            updateJobStatus(jobId, TrainingJob.JobStatus.RUNNING, 10, "创建实验");

            // 2. 创建实验（内存 + 数据库）
            ExperimentDTO experiment = ExperimentDTO.of(
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
            Trainer trainer = getTrainer(config.getModel().getType());
            String modelPath = trainer.train(config);

            // 5. 验证模型
            updateJobStatus(jobId, null, 60, "验证模型");
            Map<String, Object> metrics = trainer.validate(modelPath, config);

            // 记录指标
            @SuppressWarnings("unchecked")
            Map<String, Double> metricsDouble = (Map<String, Double>) (Object) metrics;
            mlflowRegistryService.logMetrics(config.getExperimentName(), metricsDouble);
            mlflowRegistryService.logModel(config.getExperimentName(), modelPath);

            // 6. 注册模型
            updateJobStatus(jobId, null, 80, "注册模型");
            String version = "v" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            ModelDTO model = ModelDTO.of(
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
        }
    }

    /**
     * 获取任务状态
     *
     * @param jobId 任务ID
     * @return 任务信息
     */
    public TrainingJob getJobStatus(Long jobId) {
        // 优先从缓存获取（更新更及时）
        TrainingJob cached = jobProgressCache.get(jobId);
        if (cached != null && cached.getStatus() == TrainingJob.JobStatus.RUNNING) {
            return cached;
        }
        return trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("训练任务不存在: " + jobId));
    }

    /**
     * 列出所有训练任务
     *
     * @return 任务列表
     */
    public java.util.List<TrainingJob> listJobs() {
        return trainingJobRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
                )
        );
    }

    /**
     * 列出运行中的任务
     *
     * @return 任务列表
     */
    public java.util.List<TrainingJob> listRunningJobs() {
        return trainingJobRepository.findByStatusInOrderByCreatedAtDesc(
                java.util.Arrays.asList(TrainingJob.JobStatus.PENDING, TrainingJob.JobStatus.RUNNING)
        );
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
