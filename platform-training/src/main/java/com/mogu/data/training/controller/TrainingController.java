package com.mogu.data.training.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.mogu.data.common.dto.ExperimentDTO;
import com.mogu.data.common.dto.ModelDTO;
import com.mogu.data.common.entity.HyperparameterTrial;
import com.mogu.data.common.entity.HyperparameterTuningJob;
import com.mogu.data.common.entity.TrainingJob;
import com.mogu.data.common.registry.MlflowRegistryService;
import com.mogu.data.common.result.Result;
import com.mogu.data.training.entity.TrainingConfig;
import com.mogu.data.training.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 训练控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/training")
@RequiredArgsConstructor
public class TrainingController {

    private final LightGBMTrainer lightGBMTrainer;
    private final XGBoostTrainer xgBoostTrainer;
    private final TunerService tunerService;
    private final MlflowRegistryService mlflowRegistryService;
    private final TrainingJobService trainingJobService;

    /**
     * 同步训练模型（快捷入口）
     */
    @PostMapping("/train")
    public Result<String> train(@RequestBody TrainingConfig config) {
        log.info("开始训练模型: {}", config.getExperimentName());

        try {
            // 创建实验
            ExperimentDTO experiment = ExperimentDTO.of(
                    config.getExperimentName(),
                    "Auto generated experiment",
                    config.getDatasetVersion(),
                    config.getModel().getType()
            );
            Long experimentId = mlflowRegistryService.createExperiment(experiment);

            // 记录参数
            mlflowRegistryService.logParams(config.getExperimentName(), config.getModel().getParams());

            // 选择训练器
            Trainer trainer = getTrainer(config.getModel().getType());

            // 训练模型
            String modelPath = trainer.train(config);

            // 验证模型
            Map<String, Object> metrics = trainer.validate(modelPath, config);

            // 记录指标
            mlflowRegistryService.logMetrics(config.getExperimentName(),
                    (Map<String, Double>) (Object) metrics);

            // 记录模型
            mlflowRegistryService.logModel(config.getExperimentName(), modelPath);

            // 结束实验
            mlflowRegistryService.endExperiment(config.getExperimentName(), "COMPLETED");

            return Result.success(modelPath);

        } catch (Exception e) {
            log.error("训练模型失败: {}", e.getMessage(), e);
            mlflowRegistryService.endExperiment(config.getExperimentName(), "FAILED");
            return Result.error("训练模型失败: " + e.getMessage());
        }
    }

    /**
     * 提交异步训练任务
     */
    @PostMapping("/jobs")
    public Result<Long> submitTrainingJob(@RequestBody TrainingConfig config) {
        log.info("提交异步训练任务: {}", config.getExperimentName());
        Long jobId = trainingJobService.submitTrainingJob(config);
        // 异步执行训练
        trainingJobService.executeTraining(jobId, config);
        return Result.success(jobId);
    }

    /**
     * 重新训练（基于已有任务的配置）
     */
    @PostMapping("/jobs/{id}/retry")
    public Result<Long> retryTrainingJob(@PathVariable Long id) {
        log.info("重新训练, 原任务ID: {}", id);
        Long newJobId = trainingJobService.retryTrainingJob(id);
        // 异步执行训练
        TrainingJob job = trainingJobService.getJobStatus(newJobId);
        TrainingConfig config = rebuildConfigFromJob(job);
        trainingJobService.executeTraining(newJobId, config);
        return Result.success(newJobId);
    }

    /**
     * 获取训练任务状态
     */
    @GetMapping("/jobs/{id}")
    public Result<TrainingJob> getJobStatus(@PathVariable Long id) {
        TrainingJob job = trainingJobService.getJobStatus(id);
        return Result.success(job);
    }

    /**
     * 列出所有训练任务
     */
    @GetMapping("/jobs")
    public Result<List<TrainingJob>> listTrainingJobs() {
        List<TrainingJob> jobs = trainingJobService.listJobs();
        return Result.success(jobs);
    }

    /**
     * 列出运行中的训练任务
     */
    @GetMapping("/jobs/running")
    public Result<List<TrainingJob>> listRunningJobs() {
        List<TrainingJob> jobs = trainingJobService.listRunningJobs();
        return Result.success(jobs);
    }

    /**
     * 获取训练任务日志
     */
    @GetMapping("/jobs/{id}/logs")
    public Result<String> getJobLogs(@PathVariable Long id) {
        log.info("获取训练任务日志, ID: {}", id);
        String logs = trainingJobService.getJobLogs(id);
        return Result.success(logs);
    }

    /**
     * 删除训练任务
     */
    @DeleteMapping("/jobs/{id}")
    public Result<Void> deleteTrainingJob(@PathVariable Long id) {
        log.info("删除训练任务, ID: {}", id);
        trainingJobService.deleteJob(id);
        return Result.success();
    }

    /**
     * 提交超参数调优任务
     */
    @PostMapping("/tune-jobs")
    public Result<Long> submitTuningJob(@RequestBody TrainingConfig config) {
        log.info("提交超参数调优任务: {}", config.getExperimentName());
        Long tuningJobId = tunerService.submitTuningJob(config);
        // 异步执行调优
        tunerService.executeTuning(tuningJobId, config);
        return Result.success(tuningJobId);
    }

    /**
     * 获取调优任务状态
     */
    @GetMapping("/tune-jobs/{id}")
    public Result<HyperparameterTuningJob> getTuningJob(@PathVariable Long id) {
        HyperparameterTuningJob job = tunerService.getTuningJob(id);
        return Result.success(job);
    }

    /**
     * 获取调优任务 trial 列表
     */
    @GetMapping("/tune-jobs/{id}/trials")
    public Result<List<HyperparameterTrial>> getTuningTrials(@PathVariable Long id) {
        List<HyperparameterTrial> trials = tunerService.listTrials(id);
        return Result.success(trials);
    }

    /**
     * 列出所有调优任务
     */
    @GetMapping("/tune-jobs")
    public Result<List<HyperparameterTuningJob>> listTuningJobs() {
        List<HyperparameterTuningJob> jobs = tunerService.listTuningJobs();
        return Result.success(jobs);
    }

    /**
     * 创建实验
     */
    @PostMapping("/experiments")
    public Result<Long> createExperiment(@RequestBody ExperimentDTO experiment) {
        Long id = mlflowRegistryService.createExperiment(experiment);
        return Result.success(id);
    }

    /**
     * 获取实验
     */
    @GetMapping("/experiments/{name}")
    public Result<ExperimentDTO> getExperiment(@PathVariable String name) {
        ExperimentDTO experiment = mlflowRegistryService.getExperiment(name);
        return Result.success(experiment);
    }

    /**
     * 列出所有实验
     */
    @GetMapping("/experiments")
    public Result<List<ExperimentDTO>> listExperiments() {
        List<ExperimentDTO> experiments = mlflowRegistryService.listExperiments();
        return Result.success(experiments);
    }

    /**
     * 删除实验
     */
    @DeleteMapping("/experiments/{name}")
    public Result<Void> deleteExperiment(@PathVariable String name) {
        log.info("删除实验, 名称: {}", name);
        mlflowRegistryService.deleteExperiment(name);
        return Result.success();
    }

    /**
     * 注册模型
     */
    @PostMapping("/models")
    public Result<Long> registerModel(@RequestBody ModelDTO model) {
        Long id = mlflowRegistryService.registerModel(model);
        return Result.success(id);
    }

    /**
     * 转换模型阶段
     */
    @PutMapping("/models/{name}/{version}/stage")
    public Result<Void> transitionModelStage(@PathVariable String name,
                                             @PathVariable String version,
                                             @RequestParam String stage) {
        mlflowRegistryService.transitionModelStage(name, version, stage);
        return Result.success();
    }

    /**
     * 获取生产环境模型
     */
    @GetMapping("/models/{name}/production")
    public Result<ModelDTO> getProductionModel(@PathVariable String name) {
        ModelDTO model = mlflowRegistryService.getProductionModel(name);
        return Result.success(model);
    }

    /**
     * 列出所有模型
     */
    @GetMapping("/models")
    public Result<List<ModelDTO>> listModels() {
        List<ModelDTO> models = mlflowRegistryService.listModels();
        return Result.success(models);
    }

    /**
     * 删除模型（包括 MinIO 文件）
     */
    @DeleteMapping("/models/{name}/{version}")
    public Result<Void> deleteModel(@PathVariable String name,
                                    @PathVariable String version) {
        log.info("删除模型, 名称: {}, 版本: {}", name, version);
        mlflowRegistryService.deleteModel(name, version);
        return Result.success();
    }

    /**
     * 获取训练器
     */
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

    /**
     * 从训练任务记录重建训练配置
     */
    private TrainingConfig rebuildConfigFromJob(TrainingJob job) {
        TrainingConfig config = new TrainingConfig();
        config.setExperimentName(job.getExperimentName());
        config.setDatasetName(job.getDatasetName());
        config.setDatasetVersion(job.getDatasetVersion());

        TrainingConfig.ModelConfig modelConfig = new TrainingConfig.ModelConfig();
        modelConfig.setType(job.getModelType());

        Map<String, Object> params = new HashMap<>();
        JsonNode hyperparameters = job.getHyperparameters();
        if (hyperparameters != null) {
            hyperparameters.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isNumber()) {
                    params.put(entry.getKey(), value.numberValue());
                } else if (value.isBoolean()) {
                    params.put(entry.getKey(), value.booleanValue());
                } else {
                    params.put(entry.getKey(), value.asText());
                }
            });
        }
        modelConfig.setParams(params);
        config.setModel(modelConfig);

        return config;
    }
}
