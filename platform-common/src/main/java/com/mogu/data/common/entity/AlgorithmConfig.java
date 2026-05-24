package com.mogu.data.common.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 算法配置实体类
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Entity
@Table(name = "algorithm_configs")
@Data
@EqualsAndHashCode(callSuper = false)
public class AlgorithmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String algorithmName;

    @Column(nullable = false, length = 20)
    private String framework;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "hyperparameter_space", columnDefinition = "jsonb")
    private JsonNode hyperparameterSpace;

    @Type(type = "com.mogu.data.common.util.JsonbType")
    @Column(name = "default_hyperparameters", columnDefinition = "jsonb")
    private JsonNode defaultHyperparameters;

    @Column(length = 20)
    private String modelType;

    @Column(length = 500)
    private String description;

    @Column
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
