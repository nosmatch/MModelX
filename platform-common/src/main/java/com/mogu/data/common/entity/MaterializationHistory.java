package com.mogu.data.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 特征物化历史记录实体
 *
 * 记录特征从MinIO物化到Redis的历史操作
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "materialization_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterializationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 特征视图名称
     */
    @Column(name = "feature_view_name", nullable = false, length = 100)
    private String featureViewName;

    /**
     * 特征视图ID
     */
    @Column(name = "feature_view_id")
    private Long featureViewId;

    /**
     * 物化开始时间
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * 物化完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 物化状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MaterializationStatus status;

    /**
     * 物化特征数量
     */
    @Column(name = "feature_count")
    private Integer featureCount;

    /**
     * 物化实体数量
     */
    @Column(name = "entity_count")
    private Long entityCount;

    /**
     * MinIO源文件路径
     */
    @Column(name = "source_path", length = 500)
    private String sourcePath;

    /**
     * Redis目标键前缀
     */
    @Column(name = "redis_key_prefix", length = 200)
    private String redisKeyPrefix;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 操作者
     */
    @Column(name = "operator", length = 50)
    private String operator;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 物化状态枚举
     */
    public enum MaterializationStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        SUCCESS,    // 成功
        FAILED,     // 失败
        PARTIAL     // 部分成功
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
