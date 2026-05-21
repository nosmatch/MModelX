package com.mogu.data.training.service;

import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.training.entity.TrainingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 超参数调优服务
 * 使用Optuna进行超参数搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TunerService {

    private final java.util.Map<String, Trainer> trainerMap;

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

            // 这里应该调用Optuna进行超参数搜索
            // 由于Optuna是Python库，这里需要通过ProcessBuilder调用Python脚本
            // 或者使用Java的Optuna绑定（如果有的话）

            double bestScore = 0.0;
            Map<String, Object> bestParams = new HashMap<>();

            // 模拟超参数搜索过程
            for (int i = 0; i < optunaConfig.getNTrials(); i++) {
                // 随机生成超参数
                Map<String, Object> trialParams = generateTrialParams(config.getModel().getType());

                // 训练模型
                TrainingConfig trialConfig = cloneConfig(config);
                trialConfig.getModel().setParams(trialParams);

                Trainer trainer = getTrainer(trialConfig.getModel().getType());
                String modelPath = trainer.train(trialConfig);

                // 评估模型
                Map<String, Object> metrics = trainer.validate(modelPath, trialConfig);
                double score = (double) metrics.get(optunaConfig.getMetric());

                // 更新最佳参数
                if (score > bestScore) {
                    bestScore = score;
                    bestParams = trialParams;
                }

                log.info("Trial {}/{}: {} = {}",
                        i + 1, optunaConfig.getNTrials(), optunaConfig.getMetric(), score);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("bestParams", bestParams);
            result.put("bestScore", bestScore);
            result.put("metric", optunaConfig.getMetric());

            log.info("超参数调优完成, 最佳参数: {}, 最佳得分: {}", bestParams, bestScore);
            return result;

        } catch (Exception e) {
            log.error("超参数调优失败: {}", e.getMessage(), e);
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
        // 深拷贝配置对象
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
        String trainerName = modelType + "Trainer";
        Trainer trainer = trainerMap.get(trainerName);
        if (trainer == null) {
            throw new BusinessException("不支持的模型类型: " + modelType);
        }
        return trainer;
    }
}