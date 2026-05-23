package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Type;

/**
 * 数据集实体类
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "datasets")
@Data
@EqualsAndHashCode(callSuper = false)
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "feature_view_ids", columnDefinition = "jsonb")
    private JsonNode featureViewIds;

    @Column(nullable = false, length = 100)
    private String labelColumn;

    @Column(length = 100)
    private String timeColumn;

    @Column
    private Long sampleCount;

    @Column(name = "feature_count")
    private Integer featureCount;

    @Column
    private Long positiveCount;

    @Column
    private Long negativeCount;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "split_ratio", columnDefinition = "jsonb")
    private JsonNode splitRatio;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Boolean pointInTimeEnabled = true;

    @Column(length = 10)
    private String qualityScore;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private DatasetStatus status = DatasetStatus.BUILDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 数据集状态枚举
     */
    public enum DatasetStatus {
        BUILDING,   // 构建中
        COMPLETED,  // 已完成
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