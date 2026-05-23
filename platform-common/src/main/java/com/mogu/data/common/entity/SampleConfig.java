package com.mogu.data.common.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 样本配置实体类
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Entity
@Table(name = "sample_configs")
@Data
@EqualsAndHashCode(callSuper = false)
public class SampleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 关联的特征视图名称列表（JSON数组）
     */
    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "feature_views", columnDefinition = "jsonb")
    private JsonNode featureViews;

    // ========== 标签配置 ==========
    @JsonProperty("labelSource")
    @Column(name = "label_table", length = 100)
    private String labelTable;

    @Column(name = "label_column", length = 100)
    private String labelColumn;

    @Column(name = "label_type", length = 20)
    @Enumerated(EnumType.STRING)
    private LabelType labelType = LabelType.BINARY;

    // ========== 时间配置 ==========
    @Column(name = "time_column", length = 100)
    private String timeColumn;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // ========== 划分配置 ==========
    @Column(name = "split_strategy", length = 20)
    @Enumerated(EnumType.STRING)
    private SplitStrategy splitStrategy = SplitStrategy.RANDOM;

    @Column(name = "train_ratio", precision = 3, scale = 2)
    private Double trainRatio = 0.80;

    @Column(name = "val_ratio", precision = 3, scale = 2)
    private Double valRatio = 0.10;

    @Column(name = "test_ratio", precision = 3, scale = 2)
    private Double testRatio = 0.10;

    @Column(name = "stratify_column", length = 100)
    private String stratifyColumn;

    // ========== 负样本采样 ==========
    @Column(name = "negative_sampling_enabled")
    private Boolean negativeSamplingEnabled = false;

    @Column(name = "negative_ratio", precision = 4, scale = 2)
    private Double negativeRatio = 1.00;

    // ========== 状态管理 ==========
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ConfigStatus status = ConfigStatus.ACTIVE;

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
            status = ConfigStatus.ACTIVE;
        }
        if (labelType == null) {
            labelType = LabelType.BINARY;
        }
        if (splitStrategy == null) {
            splitStrategy = SplitStrategy.RANDOM;
        }
        if (trainRatio == null) trainRatio = 0.80;
        if (valRatio == null) valRatio = 0.10;
        if (testRatio == null) testRatio = 0.10;
        if (negativeSamplingEnabled == null) negativeSamplingEnabled = false;
        if (negativeRatio == null) negativeRatio = 1.00;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 标签类型枚举
     */
    public enum LabelType {
        BINARY,      // 二分类
        MULTICLASS,  // 多分类
        REGRESSION   // 回归
    }

    /**
     * 划分策略枚举
     */
    public enum SplitStrategy {
        RANDOM,      // 随机划分
        TEMPORAL,    // 时间划分
        STRATIFIED   // 分层划分
    }

    /**
     * 配置状态枚举
     */
    public enum ConfigStatus {
        ACTIVE,     // 活跃
        DISABLED,   // 禁用
        ARCHIVED    // 已归档
    }
}
