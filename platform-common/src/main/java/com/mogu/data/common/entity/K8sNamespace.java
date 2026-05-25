package com.mogu.data.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * K8s Namespace 管理实体
 * 映射业务线到 K8s 命名空间
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Entity
@Table(name = "k8s_namespaces")
@Data
@EqualsAndHashCode(callSuper = false)
public class K8sNamespace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column
    private String description;

    @Column(nullable = false, length = 50)
    private String businessLine;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 状态枚举
     */
    public enum Status {
        ACTIVE,     // 活跃
        INACTIVE    // 停用
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
