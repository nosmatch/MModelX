package com.mogu.data.training.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Model {

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

    public static Model of(String name, String experimentId, String modelType, String modelPath, Double performance) {
        Model model = new Model();
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