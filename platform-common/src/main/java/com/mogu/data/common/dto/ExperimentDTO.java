package com.mogu.data.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 实验 DTO（用于 API 请求/响应和内存缓存）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentDTO {

    private Long id;
    private String name;
    private String description;
    private String datasetVersion;
    private String modelType;
    private Map<String, Object> params;
    private Map<String, Double> metrics;
    private String modelPath;
    private String status; // RUNNING, COMPLETED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExperimentDTO of(String name, String description, String datasetVersion, String modelType) {
        ExperimentDTO experiment = new ExperimentDTO();
        experiment.setName(name);
        experiment.setDescription(description);
        experiment.setDatasetVersion(datasetVersion);
        experiment.setModelType(modelType);
        experiment.setStatus("RUNNING");
        experiment.setCreatedAt(LocalDateTime.now());
        experiment.setUpdatedAt(LocalDateTime.now());
        return experiment;
    }
}
