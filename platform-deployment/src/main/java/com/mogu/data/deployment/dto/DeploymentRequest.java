package com.mogu.data.deployment.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 模型部署请求 DTO
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Data
public class DeploymentRequest {

    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @NotBlank(message = "Namespace 不能为空")
    private String namespace;

    @Min(value = 1, message = "副本数至少为1")
    private Integer replicas = 1;

    private String image;

    private String cpuRequest = "500m";

    private String memoryRequest = "512Mi";

    private String cpuLimit = "2000m";

    private String memoryLimit = "2Gi";

    private Integer trafficPercentage = 0;
}
