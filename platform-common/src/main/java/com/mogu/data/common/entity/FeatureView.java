package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 特征视图实体类
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "feature_views")
@Data
@EqualsAndHashCode(callSuper = false)
public class FeatureView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String entity;

    @Column(length = 20)
    private Integer ttl;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 关联的数据源ID（直接映射数据库列）
     */
    @Column(name = "datasource_id")
    private Long datasourceId;

    /**
     * 关联的数据源对象（用于导航和写入）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", insertable = false, updatable = false)
    private DataSource datasource;

    /**
     * 数据源使用配置（JSON格式）
     *
     * 存储业务配置信息，如表名、列名、过滤条件等：
     * {
     *   "table": "orders",
     *   "entityColumn": "user_id",
     *   "columns": ["user_id", "order_amount"],
     *   "where": "status='completed'"
     * }
     */
    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "source_config", columnDefinition = "jsonb")
    private JsonNode sourceConfig;

    /**
     * 【已废弃】数据源类型，请使用 datasource 关联数据源配置
     */
    @Deprecated
    @Column(length = 50)
    private String dataSourceType;

    /**
     * 【已废弃】数据源配置，请使用 datasource 关联数据源配置
     */
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String dataSourceConfig;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FeatureViewStatus status;

    @Column(name = "last_computed_time")
    private Date lastComputedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "featureView", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Feature> features = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = FeatureViewStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 特征视图状态枚举
     */
    public enum FeatureViewStatus {
        DRAFT,      // 草稿
        ACTIVE,     // 活跃
        DEPRECATED, // 已弃用
        ARCHIVED    // 已归档
    }
}