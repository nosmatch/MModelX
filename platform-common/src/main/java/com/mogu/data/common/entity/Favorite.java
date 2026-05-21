package com.mogu.data.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 特征查询收藏夹实体
 *
 * 用户收藏常用的特征查询配置
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "favorites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 收藏名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 收藏描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 实体类型
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * 特征名称列表（JSON格式存储）
     */
    @Column(name = "feature_names", nullable = false, columnDefinition = "TEXT")
    private String featureNames;

    /**
     * 收藏类型（FEATURE_QUERY, FEATURE_VIEW, etc.）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "favorite_type", nullable = false, length = 50)
    private FavoriteType favoriteType;

    /**
     * 关联的特征视图名称
     */
    @Column(name = "feature_view_name", length = 100)
    private String featureViewName;

    /**
     * 排序序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 是否默认
     */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    /**
     * 创建者
     */
    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

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
     * 收藏类型枚举
     */
    public enum FavoriteType {
        FEATURE_QUERY,   // 特征查询
        FEATURE_VIEW,    // 特征视图
        DATASET          // 数据集
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isDefault == null) {
            isDefault = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
