package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 超参数调优任务实体类
 */
@Entity
@Table(name = "hyperparameter_tuning_jobs")
@Data
@EqualsAndHashCode(callSuper = false)
public class HyperparameterTuningJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_name", nullable = false, length = 100)
    private String experimentName;

    @Column(name = "dataset_name", length = 100)
    private String datasetName;

    @Column(name = "dataset_version", length = 50)
    private String datasetVersion;

    @Column(name = "model_type", length = 20)
    private String modelType;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "n_trials", nullable = false)
    private Integer nTrials = 20;

    @Column(name = "current_trial")
    private Integer currentTrial = 0;

    @Column(length = 50)
    private String metric = "auc";

    @Column(length = 20)
    private String direction = "maximize";

    @Column(name = "best_score")
    private Double bestScore;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "best_params", columnDefinition = "jsonb")
    private JsonNode bestParams;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "final_metrics", columnDefinition = "jsonb")
    private JsonNode finalMetrics;

    @Column(name = "model_path", length = 500)
    private String modelPath;

    @Column(name = "elapsed_time_ms")
    private Long elapsedTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum JobStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED
    }
}
