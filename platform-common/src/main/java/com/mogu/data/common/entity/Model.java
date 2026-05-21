package com.mogu.data.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 模型实体类
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "models")
@Data
@EqualsAndHashCode(callSuper = false)
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Column
    private Long fileSize;

    @Column(nullable = false, length = 20)
    private String framework;

    @Column(nullable = false, length = 20)
    private String modelType;

    @Column(columnDefinition = "JSONB")
    private String hyperparameters;

    @Column(columnDefinition = "JSONB")
    private String metrics;

    @Column(length = 255)
    private String mlflowRunId;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 框架类型枚举
     */
    public enum Framework {
        LIGHTGBM,
        XGBOOST,
        TENSORFLOW,
        PYTORCH,
        SKLEARN,
        ONNX
    }

    /**
     * 模型类型枚举
     */
    public enum ModelType {
        CLASSIFICATION,  // 分类
        REGRESSION,     // 回归
        RANKING,        // 排序
        CLUSTERING       // 聚类
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        registeredAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}