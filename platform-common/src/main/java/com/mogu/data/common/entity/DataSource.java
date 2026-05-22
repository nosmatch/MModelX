package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据源配置实体
 *
 * 用于统一管理所有数据源的连接信息，包括：
 * - 关系型数据库（PostgreSQL, MySQL）
 * - 缓存（Redis）
 * - 消息队列（Kafka）
 * - 对象存储（MinIO）
 * - API 接口
 * - 本地文件
 *
 * @author MModelX Team
 * @since 2026-05-21
 */
@Entity
@Table(name = "datasources")
@Data
@EqualsAndHashCode(callSuper = false)
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 数据源名称
     * 全局唯一，用于标识和引用数据源
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * 数据源类型
     * 可选值：postgresql, mysql, redis, kafka, minio, api, local_file
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 数据源描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    // ==================== 连接配置 ====================

    /**
     * 主机地址
     * 数据库：localhost 或 IP 地址
     * API：完整的 URL（如 https://api.example.com）
     */
    @Column(length = 255)
    private String host;

    /**
     * 端口号
     * 数据库、Redis、Kafka 等需要
     * API 类型不需要此字段
     */
    @Column
    private Integer port;

    /**
     * 数据库名称
     * 适用于关系型数据库
     */
    @Column(name = "database_name", length = 100)
    private String databaseName;

    /**
     * 用户名
     * 用于数据库连接认证
     */
    @Column
    private String username;

    /**
     * 加密后的密码
     * 使用 AES 加密算法存储
     * 注意：此字段存储的是加密后的密码，不是明文
     */
    @Column(name = "password_encrypted", columnDefinition = "TEXT")
    private String passwordEncrypted;

    /**
     * 扩展属性
     * 用于存储特殊配置，JSON 格式
     * 例如：
     * - Redis 集群模式配置
     * - Kafka 消费者组配置
     * - API 认证方式
     * - MinIO bucket 配置
     */
    @org.hibernate.annotations.Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(columnDefinition = "jsonb")
    private JsonNode properties;

    // ==================== 状态管理 ====================

    /**
     * 数据源状态
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DataSourceStatus status;

    /**
     * 最后测试连接的时间
     */
    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;

    /**
     * 最后一次连接测试的结果
     */
    @Column(name = "last_test_result")
    private Boolean lastTestResult;

    /**
     * 最后一次连接失败的错误信息
     */
    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    // ==================== 元数据 ====================

    /**
     * 创建人
     */
    @Column(name = "created_by", length = 50)
    private String createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== 生命周期回调 ====================

    /**
     * 创建前的回调
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        // 默认状态为 ACTIVE
        if (status == null) {
            status = DataSourceStatus.ACTIVE;
        }
    }

    /**
     * 更新前的回调
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 数据源状态枚举 ====================

    /**
     * 数据源状态枚举
     */
    public enum DataSourceStatus {
        /**
         * 激活可用
         */
        ACTIVE,

        /**
         * 已禁用
         * 临时禁用，不会删除配置
         */
        DISABLED,

        /**
         * 错误状态
         * 最后一次连接测试失败
         */
        ERROR
    }

    // ==================== 工具方法 ====================

    /**
     * 获取连接信息字符串
     * 用于显示，格式：host:port/database
     */
    public String getConnectionString() {
        StringBuilder sb = new StringBuilder();

        if (host != null) {
            sb.append(host);
        }

        if (port != null) {
            sb.append(":").append(port);
        }

        if (databaseName != null) {
            sb.append("/").append(databaseName);
        }

        return sb.toString();
    }

    /**
     * 检查数据源是否可用
     */
    public boolean isAvailable() {
        return status == DataSourceStatus.ACTIVE;
    }

    /**
     * 检查是否需要密码认证
     */
    public boolean requiresAuthentication() {
        return "postgresql".equals(type)
            || "mysql".equals(type)
            || "redis".equals(type)
            || "kafka".equals(type);
    }
}
