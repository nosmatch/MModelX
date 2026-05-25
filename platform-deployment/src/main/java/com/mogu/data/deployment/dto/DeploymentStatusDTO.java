package com.mogu.data.deployment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 部署状态 DTO
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Data
public class DeploymentStatusDTO {

    private Long id;
    private Long deploymentId;
    private String modelName;
    private String modelVersion;
    private String namespace;
    private String deploymentName;
    private String serviceName;
    private String status;
    private String k8sStatus;
    private Integer replicas;
    private Integer readyReplicas;
    private Integer availableReplicas;
    private String endpointUrl;
    private String image;
    private LocalDateTime deployedAt;
    private List<Map<String, String>> conditions;
}
