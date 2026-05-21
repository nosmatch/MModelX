package com.mogu.data.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.ENGINEER;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 用户角色枚举
     */
    public enum UserRole {
        ADMIN,      // 管理员
        MLOPS,      // MLOps工程师
        ENGINEER    // 算法工程师
    }

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        ACTIVE,     // 激活
        INACTIVE,   // 未激活
        LOCKED      // 锁定
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