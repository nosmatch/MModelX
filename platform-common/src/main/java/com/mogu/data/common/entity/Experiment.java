package com.mogu.data.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实验实体类
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "experiments")
@Data
@EqualsAndHashCode(callSuper = false)
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    @Column(nullable = false, length = 20)
    private String modelType;

    @Column(columnDefinition = "JSONB")
    private String hyperparameters;

    @Column(length = 50)
    private String metricName = "auc";

    @Column(precision = 10, scale = 4)
    private BigDecimal metricValue;

    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    private OptimizationDirection optimizationDirection = OptimizationDirection.MAXIMIZE;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ExperimentStatus status = ExperimentStatus.PENDING;

    @Column
    private Integer progress = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 255)
    private String mlflowRunId;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 模型类型枚举
     */
    public enum ModelType {
        LIGHTGBM,
        XGBOOST,
        TENSORFLOW,
        PYTORCH,
        SKLEARN
    }

    /**
     * 优化方向枚举
     */
    public enum OptimizationDirection {
        MAXIMIZE,  // 最大化
        MINIMIZE   // 最小化
    }

    /**
     * 实验状态枚举
     */
    public enum ExperimentStatus {
        PENDING,    // 待运行
        RUNNING,    // 运行中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        STOPPED     // 已停止
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
