package com.mogu.data.training.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 训练配置
 */
@Data
public class TrainingConfig {

    /**
     * 实验名称
     */
    private String experimentName;

    /**
     * 数据集版本
     */
    private String datasetVersion;

    /**
     * 模型配置
     */
    private ModelConfig model;

    /**
     * 训练参数
     */
    private TrainingParams trainingParams;

    /**
     * Optuna调参配置
     */
    private OptunaConfig optunaConfig;

    /**
     * 评估指标配置
     */
    private List<String> metrics;

    /**
     * 模型配置
     */
    @Data
    public static class ModelConfig {
        private String type; // lightgbm, xgboost, pytorch
        private String objective;
        private Map<String, Object> params;
    }

    /**
     * 训练参数
     */
    @Data
    public static class TrainingParams {
        private int numRounds = 100;
        private int earlyStoppingRounds = 10;
        private double validationFraction = 0.2;
        private int crossValidationFolds = 5;
        private int batchSize = 32;
        private double learningRate = 0.1;
    }

    /**
     * Optuna调参配置
     */
    @Data
    public static class OptunaConfig {
        private int nTrials = 50;
        private String metric = "auc";
        private String direction = "maximize";
        private int timeout = 3600; // 秒
    }
}