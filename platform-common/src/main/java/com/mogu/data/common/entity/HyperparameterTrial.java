package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 超参数调优 Trial 实体类
 * 记录每一轮搜索的参数和结果
 */
@Entity
@Table(name = "hyperparameter_trials")
@Data
@EqualsAndHashCode(callSuper = false)
public class HyperparameterTrial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tuning_job_id", nullable = false)
    private Long tuningJobId;

    @Column(name = "trial_index", nullable = false)
    private Integer trialIndex;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "params", columnDefinition = "jsonb")
    private JsonNode params;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "metrics", columnDefinition = "jsonb")
    private JsonNode metrics;

    @Column
    private Double score;

    @Column(name = "is_best")
    private Boolean isBest = false;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TrialStatus status = TrialStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "elapsed_time_ms")
    private Long elapsedTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TrialStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED
    }
}
