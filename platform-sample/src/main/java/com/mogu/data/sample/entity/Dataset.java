package com.mogu.data.sample.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据集实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {

    private Long id;
    private String name;
    private String description;
    private String version;
    private String featureView;
    private Long rowCount;
    private String path;
    private String format;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Dataset of(String name, String description, String featureView, String path) {
        Dataset dataset = new Dataset();
        dataset.setName(name);
        dataset.setDescription(description);
        dataset.setFeatureView(featureView);
        dataset.setPath(path);
        dataset.setFormat("parquet");
        dataset.setStatus("CREATED");
        dataset.setCreatedAt(LocalDateTime.now());
        dataset.setUpdatedAt(LocalDateTime.now());
        dataset.setVersion("v1.0");
        return dataset;
    }
}