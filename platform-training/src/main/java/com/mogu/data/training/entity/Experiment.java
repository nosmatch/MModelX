package com.mogu.data.training.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 实验实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {

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

    public static Experiment of(String name, String description, String datasetVersion, String modelType) {
        Experiment experiment = new Experiment();
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