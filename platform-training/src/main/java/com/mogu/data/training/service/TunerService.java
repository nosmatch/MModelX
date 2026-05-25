package com.mogu.data.training.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.dto.ExperimentDTO;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.registry.MlflowRegistryService;
import com.mogu.data.training.entity.TrainingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 超参数调优服务
 * 使用网格搜索/随机搜索进行超参数搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TunerService {

    private final LightGBMTrainer lightGBMTrainer;
    private final XGBoostTrainer xgBoostTrainer;
    private final MlflowRegistryService mlflowRegistryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 超参数调优
     * @param config 训练配置
     * @return 最佳参数和性能
     */
    public Map<String, Object> hyperparameterTuning(TrainingConfig config) {
        log.info("开始超参数调优: {}, trials: {}",
                config.getExperimentName(),
                config.getOptunaConfig().getNTrials());

        try {
            TrainingConfig.OptunaConfig optunaConfig = config.getOptunaConfig();
            Trainer trainer = getTrainer(config.getModel().getType());

            // 创建实验
            ExperimentDTO experiment = ExperimentDTO.of(
                    config.getExperimentName(),
                    "Hyperparameter tuning",
                    config.getDatasetVersion(),
                    config.getModel().getType()
            );
            mlflowRegistryService.createExperiment(experiment);

            double bestScore = optunaConfig.getDirection().equals("minimize") ? Double.MAX_VALUE : 0.0;
            Map<String, Object> bestParams = new HashMap<>();
            Map<String, Object> bestMetrics = new HashMap<>();

            // 模拟超参数搜索过程
            for (int i = 0; i < optunaConfig.getNTrials(); i++) {
                Map<String, Object> trialParams = generateTrialParams(config.getModel().getType());

                // 训练模型
                TrainingConfig trialConfig = cloneConfig(config);
                trialConfig.getModel().setParams(trialParams);

                String modelPath = trainer.train(trialConfig);
                Map<String, Object> metrics = trainer.validate(modelPath, trialConfig);
                double score = ((Number) metrics.get(optunaConfig.getMetric())).doubleValue();

                // 记录每个 trial 的参数和指标
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
                }

                log.info("Trial {}/{}: {} = {}",
                        i + 1, optunaConfig.getNTrials(), optunaConfig.getMetric(), score);
            }

            // 使用最佳参数再次训练并注册模型
            TrainingConfig bestConfig = cloneConfig(config);
            bestConfig.getModel().setParams(bestParams);
            String bestModelPath = trainer.train(bestConfig);
            Map<String, Object> finalMetrics = trainer.validate(bestModelPath, bestConfig);

            mlflowRegistryService.logParams(config.getExperimentName(), bestParams);
            @SuppressWarnings("unchecked")
            Map<String, Double> finalMetricsDouble = (Map<String, Double>) (Object) finalMetrics;
            mlflowRegistryService.logMetrics(config.getExperimentName(), finalMetricsDouble);
            mlflowRegistryService.logModel(config.getExperimentName(), bestModelPath);
            mlflowRegistryService.endExperiment(config.getExperimentName(), "COMPLETED");

            Map<String, Object> result = new HashMap<>();
            result.put("bestParams", bestParams);
            result.put("bestScore", bestScore);
            result.put("metric", optunaConfig.getMetric());
            result.put("direction", optunaConfig.getDirection());
            result.put("finalMetrics", finalMetrics);
            result.put("modelPath", bestModelPath);

            log.info("超参数调优完成, 最佳参数: {}, 最佳得分: {}", bestParams, bestScore);
            return result;

        } catch (Exception e) {
            log.error("超参数调优失败: {}", e.getMessage(), e);
            mlflowRegistryService.endExperiment(config.getExperimentName(), "FAILED");
            throw new BusinessException("超参数调优失败: " + e.getMessage());
        }
    }

    /**
     * 生成试验参数
     */
    private Map<String, Object> generateTrialParams(String modelType) {
        Map<String, Object> params = new HashMap<>();

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

    /**
     * 克隆配置
     */
    private TrainingConfig cloneConfig(TrainingConfig config) {
        TrainingConfig cloned = new TrainingConfig();
        cloned.setExperimentName(config.getExperimentName());
        cloned.setDatasetVersion(config.getDatasetVersion());
        cloned.setModel(config.getModel());
        cloned.setTrainingParams(config.getTrainingParams());
        cloned.setOptunaConfig(config.getOptunaConfig());
        cloned.setMetrics(config.getMetrics());
        return cloned;
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
                throw new BusinessException("不支持的模型类型: " + modelType);
        }
    }
}
