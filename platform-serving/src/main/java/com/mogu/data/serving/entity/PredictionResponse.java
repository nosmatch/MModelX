package com.mogu.data.serving.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 预测响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {

    /**
     * 实体类型
     */
    private String entityType;

    /**
     * 实体ID
     */
    private String entityId;

    /**
     * 预测结果
     */
    private Double prediction;

    /**
     * 预测概率（对于分类问题）
     */
    private Map<String, Double> probabilities;

    /**
     * 模型信息
     */
    private ModelInfo modelInfo;

    /**
     * 特征信息
     */
    private FeatureInfo featureInfo;

    /**
     * 预测耗时（毫秒）
     */
    private Long latency;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 模型信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfo {
        private String modelName;
        private String modelVersion;
        private String modelType;
    }

    /**
     * 特征信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureInfo {
        private Map<String, Object> features;
        private Integer featureCount;
        private Map<String, String> featureSources;
    }
}