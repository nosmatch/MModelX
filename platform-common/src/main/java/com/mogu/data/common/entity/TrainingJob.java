package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 训练任务实体类
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Entity
@Table(name = "training_jobs")
@Data
@EqualsAndHashCode(callSuper = false)
public class TrainingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "experiment_name", nullable = false, length = 100)
    private String experimentName;

    @Column(name = "dataset_version", length = 50)
    private String datasetVersion;

    @Column(name = "model_type", length = 20)
    private String modelType;

    // ========== 任务状态 ==========
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    @Column
    private Integer progress = 0;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @Column(name = "total_steps")
    private Integer totalSteps = 5;

    // ========== 训练结果 ==========
    @Column(name = "model_path", length = 500)
    private String modelPath;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "metrics", columnDefinition = "jsonb")
    private JsonNode metrics;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "hyperparameters", columnDefinition = "jsonb")
    private JsonNode hyperparameters;

    // ========== 执行统计 ==========
    @Column(name = "elapsed_time_ms")
    private Long elapsedTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ========== 时间戳 ==========
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = JobStatus.PENDING;
        }
        if (progress == null) {
            progress = 0;
        }
        if (totalSteps == null) {
            totalSteps = 5;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 任务状态枚举
     */
    public enum JobStatus {
        PENDING,    // 等待中
        RUNNING,    // 运行中
        SUCCESS,    // 成功
        FAILED,     // 失败
        CANCELLED   // 已取消
    }
}
