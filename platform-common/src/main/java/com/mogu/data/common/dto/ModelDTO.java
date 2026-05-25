package com.mogu.data.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型 DTO（用于 API 请求/响应和内存缓存）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelDTO {

    private Long id;
    private String name;
    private String version;
    private String experimentId;
    private String modelType;
    private String modelPath;
    private Double performance;
    private String stage; // Staging, Production, Archived
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ModelDTO of(String name, String experimentId, String modelType, String modelPath, Double performance) {
        ModelDTO model = new ModelDTO();
        model.setName(name);
        model.setExperimentId(experimentId);
        model.setModelType(modelType);
        model.setModelPath(modelPath);
        model.setPerformance(performance);
        model.setStage("Staging");
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        return model;
    }
}
