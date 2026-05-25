package com.mogu.data.deployment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Pod 状态 DTO
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Data
public class PodStatusDTO {

    private String name;
    private String namespace;
    private String status;
    private String phase;
    private String podIp;
    private String nodeName;
    private String restartCount;
    private LocalDateTime startTime;
    private String image;
    private String ready;
}
