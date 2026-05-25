package com.mogu.data.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型部署实体类
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "deployments")
@Data
@EqualsAndHashCode(callSuper = false)
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Environment environment;

    @Column(nullable = false)
    private Integer trafficPercentage = 0;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private DeploymentStatus status = DeploymentStatus.STOPPED;

    @Column(length = 255)
    private String endpointUrl;

    @Column
    private Integer currentQps = 0;

    @Column
    private Integer avgLatencyMs = 0;

    @Column(precision = 5, scale = 4)
    private BigDecimal errorRate = BigDecimal.ZERO;

    @Column
    private LocalDateTime deployedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== K8s 扩展字段 ====================

    @Column(length = 100)
    private String namespace;

    @Column(nullable = false)
    private Integer replicas = 1;

    @Column(length = 255)
    private String image;

    @Column(length = 20)
    private String cpuRequest = "500m";

    @Column(length = 20)
    private String memoryRequest = "512Mi";

    @Column(length = 20)
    private String cpuLimit = "2000m";

    @Column(length = 20)
    private String memoryLimit = "2Gi";

    @Column(length = 100)
    private String serviceName;

    @Column(length = 100)
    private String deploymentName;

    @Column(length = 50)
    private String k8sStatus;

    @Column
    private Integer availableReplicas = 0;

    @Column
    private Integer readyReplicas = 0;

    /**
     * 部署环境枚举
     */
    public enum Environment {
        DEVELOPMENT,  // 开发环境
        STAGING,      // 测试环境
        PRODUCTION    // 生产环境
    }

    /**
     * 部署状态枚举
     */
    public enum DeploymentStatus {
        STOPPED,    // 已停止
        DEPLOYING,   // 部署中
        RUNNING,     // 运行中
        FAILED       // 失败
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
