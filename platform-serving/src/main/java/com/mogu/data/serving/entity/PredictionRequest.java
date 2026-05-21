package com.mogu.data.serving.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 预测请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRequest {

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型版本（可选，默认使用生产版本）
     */
    private String modelVersion;

    /**
     * 实体类型
     */
    private String entityType;

    /**
     * 实体ID
     */
    private String entityId;

    /**
     * 特征数据
     */
    private Map<String, Object> features;

    /**
     * 是否包含特征详细信息
     */
    private Boolean includeDetails = false;

    /**
     * 预测请求数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchPredictionRequest {
        private String modelName;
        private String modelVersion;
        private String entityType;
        private List<String> entityIds;
        private Map<String, Object> features;
        private Boolean includeDetails = false;
    }
}