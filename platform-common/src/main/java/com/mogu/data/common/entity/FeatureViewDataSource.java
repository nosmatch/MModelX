package com.mogu.data.common.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 特征视图与数据源的关联实体
 *
 * 用于建立特征视图和数据源之间的多对多关系
 * 一个特征视图可以使用多个数据源
 * 一个数据源可以被多个特征视图使用
 *
 * @author MModelX Team
 * @since 2026-05-21
 */
@Entity
@Table(name = "feature_view_datasources")
@Data
public class FeatureViewDataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 特征视图
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_view_id", nullable = false)
    private FeatureView featureView;

    /**
     * 数据源
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", nullable = false)
    private DataSource datasource;

    /**
     * 数据源使用配置（JSON格式）
     *
     * 存储业务配置信息，根据数据源类型不同，内容不同：
     *
     * PostgreSQL 示例：
     * {
     *   "table": "orders",
     *   "entityColumn": "user_id",
     *   "dateColumn": "order_date",
     *   "columns": ["user_id", "order_amount", "payment_method"],
     *   "where": "status='completed'",
     *   "orderBy": "order_date DESC"
     * }
     *
     * Redis 示例：
     * {
     *   "keyPattern": "user:{user_id}:profile:*",
     *   "dataType": "hash",
     *   "scanCount": 1000
     * }
     *
     * API 示例：
     * {
     *   "path": "/v1/events",
     *   "method": "GET",
     *   "entityField": "user_id",
     *   "params": {"start_date": "2026-05-21"},
     *   "dataPath": "data.items"
     * }
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String config;

    /**
     * 是否为主数据源
     *
     * 一个特征视图可以有多个数据源，但只能有一个主数据源
     * 主数据源用于主要的特征计算
     * 其他数据源可能用于数据增强或补充
     */
    @Column(name = "is_primary")
    private Boolean isPrimary;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ==================== 生命周期回调 ====================

    /**
     * 创建前的回调
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        // 默认为主数据源
        if (isPrimary == null) {
            isPrimary = true;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取配置中的某个字段值
     */
    public String getConfigValue(String key) {
        if (config == null || config.isEmpty()) return null;
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(config).get(key);
            if (node == null || node.isNull()) return null;
            return node.isTextual() ? node.asText() : node.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取配置中的某个字段值，带默认值
     */
    public String getConfigValue(String key, String defaultValue) {
        String value = getConfigValue(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查是否为主数据源
     */
    public boolean isPrimary() {
        return Boolean.TRUE.equals(isPrimary);
    }

    /**
     * 获取数据源类型快捷方式
     */
    public String getDataSourceType() {
        return datasource != null ? datasource.getType() : null;
    }

    /**
     * 获取数据源名称快捷方式
     */
    public String getDataSourceName() {
        return datasource != null ? datasource.getName() : null;
    }

    /**
     * 获取特征视图名称快捷方式
     */
    public String getFeatureViewName() {
        return featureView != null ? featureView.getName() : null;
    }

    /**
     * 获取完整的连接字符串
     */
    public String getFullConnectionString() {
        if (datasource == null) {
            return "N/A";
        }
        return datasource.getConnectionString();
    }
}
