package com.mogu.data.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MLflow配置类
 * 用于实验追踪和模型注册
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mlflow")
public class MlflowConfig {

    private String trackingUri;
    private String registryUri;
    private String s3EndpointUrl;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName;
}