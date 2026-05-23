package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 样本构建任务实体类
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Entity
@Table(name = "sample_build_jobs")
@Data
@EqualsAndHashCode(callSuper = false)
public class SampleBuildJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "sample_config_id", nullable = false)
    private Long sampleConfigId;

    /**
     * 关联的样本配置（懒加载）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_config_id", insertable = false, updatable = false)
    private SampleConfig sampleConfig;

    // ========== 任务状态 ==========
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    @Column
    private Integer progress = 0;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @Column(name = "total_steps")
    private Integer totalSteps = 4;

    // ========== 执行统计 ==========
    @Column(name = "entity_count")
    private Long entityCount;

    @Column(name = "feature_count")
    private Integer featureCount;

    @Column(name = "positive_count")
    private Long positiveCount;

    @Column(name = "negative_count")
    private Long negativeCount;

    @Column(name = "elapsed_time_ms")
    private Long elapsedTimeMs;

    // ========== 输出路径 ==========
    @Column(name = "output_path", length = 500)
    private String outputPath;

    @Column(name = "train_path", length = 500)
    private String trainPath;

    @Column(name = "val_path", length = 500)
    private String valPath;

    @Column(name = "test_path", length = 500)
    private String testPath;

    /**
     * 质量报告（JSON格式）
     */
    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "quality_report", columnDefinition = "jsonb")
    private JsonNode qualityReport;

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
            totalSteps = 4;
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
