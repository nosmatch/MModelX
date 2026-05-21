package com.mogu.data.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 在线特征查询历史记录实体
 *
 * 记录用户查询在线特征的历史操作
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "query_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 查询名称（用户自定义）
     */
    @Column(name = "query_name", length = 100)
    private String queryName;

    /**
     * 实体类型
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * 实体ID
     */
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    /**
     * 查询的特征名称列表（JSON格式存储）
     */
    @Column(name = "feature_names", columnDefinition = "TEXT")
    private String featureNames;

    /**
     * 查询结果（JSON格式存储）
     */
    @Column(name = "query_result", columnDefinition = "TEXT")
    private String queryResult;

    /**
     * 查询状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QueryStatus status;

    /**
     * 查询耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 查询者
     */
    @Column(name = "queried_by", length = 50)
    private String queriedBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 查询状态枚举
     */
    public enum QueryStatus {
        SUCCESS,    // 查询成功
        FAILED,     // 查询失败
        TIMEOUT,    // 查询超时
        PARTIAL     // 部分成功
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
