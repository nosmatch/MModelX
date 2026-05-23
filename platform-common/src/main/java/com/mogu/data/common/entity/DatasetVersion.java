package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据集版本实体类
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Entity
@Table(name = "dataset_versions")
@Data
@EqualsAndHashCode(callSuper = false)
public class DatasetVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    /**
     * 关联的数据集（懒加载）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", insertable = false, updatable = false)
    private Dataset dataset;

    // ========== 版本信息 ==========
    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "version_tag", length = 100)
    private String versionTag;

    @Column(name = "parent_version", length = 50)
    private String parentVersion;

    // ========== 数据路径 ==========
    @Column(name = "data_path", length = 500)
    private String dataPath;

    @Column(name = "train_path", length = 500)
    private String trainPath;

    @Column(name = "val_path", length = 500)
    private String valPath;

    @Column(name = "test_path", length = 500)
    private String testPath;

    @Column(name = "metadata_path", length = 500)
    private String metadataPath;

    // ========== 数据统计 ==========
    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "feature_count")
    private Integer featureCount;

    @Column(name = "train_count")
    private Long trainCount;

    @Column(name = "val_count")
    private Long valCount;

    @Column(name = "test_count")
    private Long testCount;

    /**
     * 特征名称列表（JSON格式）
     */
    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "feature_names", columnDefinition = "jsonb")
    private JsonNode featureNames;

    /**
     * 质量指标（JSON格式）
     */
    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "quality_metrics", columnDefinition = "jsonb")
    private JsonNode qualityMetrics;

    // ========== 关联信息 ==========
    @Column(name = "experiment_id", length = 100)
    private String experimentId;

    @Column(name = "model_id", length = 100)
    private String modelId;

    // ========== 状态 ==========
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private VersionStatus status = VersionStatus.CREATED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ========== 元数据 ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = VersionStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 版本状态枚举
     */
    public enum VersionStatus {
        CREATED,      // 已创建
        VALIDATING,   // 校验中
        READY,        // 可用
        DEPRECATED,   // 已弃用
        DELETED       // 已删除
    }
}
