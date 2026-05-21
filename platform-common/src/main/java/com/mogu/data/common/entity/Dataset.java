package com.mogu.data.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

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

    @Column(nullable = false, columnDefinition = "BIGINT[]")
    private Long[] featureViewIds;

    @Column(nullable = false, length = 100)
    private String labelColumn;

    @Column(length = 100)
    private String timeColumn;

    @Column
    private Long sampleCount;

    @Column
    private Long positiveCount;

    @Column
    private Long negativeCount;

    @Column(columnDefinition = "JSONB")
    private String splitRatio;

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