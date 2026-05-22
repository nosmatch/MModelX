package com.mogu.data.feature.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 特征视图实体
 */
public class FeatureView {

    private Long id;
    private String name;
    private String entity;
    private Integer ttl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
    private String status;

    // 数据源关联（从数据源配置管理中引用）
    private Long datasourceId;
    private String datasourceName;
    private String sourceConfig;

    public FeatureView() {
    }

    public FeatureView(Long id, String name, String entity, Integer ttl, String description,
                      LocalDateTime createdAt, LocalDateTime updatedAt, Integer version, String status) {
        this.id = id;
        this.name = name;
        this.entity = entity;
        this.ttl = ttl;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(Long datasourceId) {
        this.datasourceId = datasourceId;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(String sourceConfig) {
        this.sourceConfig = sourceConfig;
    }

    private String datasourceType;
    private List<com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec> features;

    public String getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(String datasourceType) {
        this.datasourceType = datasourceType;
    }

    public List<com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec> getFeatures() {
        return features;
    }

    public void setFeatures(List<com.mogu.data.feature.entity.FeatureDefinition.FeatureSpec> features) {
        this.features = features;
    }

    public static FeatureView of(String name, String entity, Integer ttl, String description) {
        FeatureView featureView = new FeatureView();
        featureView.setName(name);
        featureView.setEntity(entity);
        featureView.setTtl(ttl);
        featureView.setDescription(description);
        featureView.setCreatedAt(LocalDateTime.now());
        featureView.setUpdatedAt(LocalDateTime.now());
        featureView.setVersion(1);
        featureView.setStatus("ACTIVE");
        return featureView;
    }
}
