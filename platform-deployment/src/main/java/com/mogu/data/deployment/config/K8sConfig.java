package com.mogu.data.deployment.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.IOException;

/**
 * K8s Java Client 配置
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "k8s")
@ConditionalOnProperty(prefix = "k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class K8sConfig {

    private String kubeconfigPath;
    private String masterUrl;
    private String token;
    private boolean verifySsl = false;

    @Bean
    public ApiClient k8sApiClient() {
        ApiClient client = null;
        try {
            if (kubeconfigPath != null && !kubeconfigPath.isEmpty()) {
                log.info("使用 kubeconfig 文件连接 K8s: {}", kubeconfigPath);
                client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeconfigPath))).build();
            } else {
                log.info("使用默认方式连接 K8s");
                client = io.kubernetes.client.util.Config.defaultClient();
            }
            client.setVerifyingSsl(verifySsl);
            io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
            log.info("K8s ApiClient 初始化成功");
        } catch (Throwable e) {
            log.warn("K8s 连接初始化失败，K8s 相关功能将不可用。原因: {}", e.getMessage());
            // 创建一个空客户端占位，避免后续 NullPointerException
            client = new ApiClient();
            client.setBasePath("http://localhost:0");
        }
        return client;
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient client) {
        return new CoreV1Api(client);
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient client) {
        return new AppsV1Api(client);
    }
}
