package com.mogu.data.training.controller;

import com.mogu.data.common.result.Result;
import com.mogu.data.training.entity.Experiment;
import com.mogu.data.training.entity.Model;
import com.mogu.data.training.entity.TrainingConfig;
import com.mogu.data.training.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 训练控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

    private final LightGBMTrainer lightGBMTrainer;
    private final XGBoostTrainer xgBoostTrainer;
    private final TunerService tunerService;
    private final MlflowRegistryService mlflowRegistryService;

    /**
     * 训练模型
     */
    @PostMapping("/train")
    public Result<String> train(@RequestBody TrainingConfig config) {
        log.info("开始训练模型: {}", config.getExperimentName());

        try {
            // 创建实验
            Experiment experiment = Experiment.of(
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
     * 超参数调优
     */
    @PostMapping("/tune")
    public Result<Map<String, Object>> hyperparameterTuning(@RequestBody TrainingConfig config) {
        Map<String, Object> result = tunerService.hyperparameterTuning(config);
        return Result.success(result);
    }

    /**
     * 创建实验
     */
    @PostMapping("/experiments")
    public Result<Long> createExperiment(@RequestBody Experiment experiment) {
        Long id = mlflowRegistryService.createExperiment(experiment);
        return Result.success(id);
    }

    /**
     * 获取实验
     */
    @GetMapping("/experiments/{name}")
    public Result<Experiment> getExperiment(@PathVariable String name) {
        Experiment experiment = mlflowRegistryService.getExperiment(name);
        return Result.success(experiment);
    }

    /**
     * 列出所有实验
     */
    @GetMapping("/experiments")
    public Result<List<Experiment>> listExperiments() {
        List<Experiment> experiments = mlflowRegistryService.listExperiments();
        return Result.success(experiments);
    }

    /**
     * 注册模型
     */
    @PostMapping("/models")
    public Result<Long> registerModel(@RequestBody Model model) {
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
    public Result<Model> getProductionModel(@PathVariable String name) {
        Model model = mlflowRegistryService.getProductionModel(name);
        return Result.success(model);
    }

    /**
     * 列出所有模型
     */
    @GetMapping("/models")
    public Result<List<Model>> listModels() {
        List<Model> models = mlflowRegistryService.listModels();
        return Result.success(models);
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
}