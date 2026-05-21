package com.mogu.data.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 特征定义实体类
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "features")
@Data
@EqualsAndHashCode(callSuper = false)
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_view_id", nullable = false)
    private FeatureView featureView;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String dtype;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String sourceType;

    @Column(columnDefinition = "TEXT")
    private String sourcePath;

    @Column(columnDefinition = "JSONB")
    private String config;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private FeatureStatus status = FeatureStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 特征数据类型枚举
     */
    public enum FeatureDataType {
        INT64,    // 整数
        FLOAT64,  // 浮点数
        STRING,   // 字符串
        BOOLEAN   // 布尔值
    }

    /**
     * 特征状态枚举
     */
    public enum FeatureStatus {
        DRAFT,      // 草稿
        COMPUTING,  // 计算中
        ACTIVE,     // 激活
        FAILED      // 失败
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