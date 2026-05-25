package com.mogu.data.api.service;

import com.mogu.data.common.config.MlflowConfig;
import com.mogu.data.deployment.service.K8sOperations;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 平台依赖健康检查服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformHealthService {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final MinioClient minioClient;
    private final MlflowConfig mlflowConfig;
    private final ObjectProvider<K8sOperations> k8sOperationsProvider;

    public Map<String, Object> buildHealthReport() {
        Map<String, Object> components = new HashMap<>();
        components.put("postgresql", checkPostgreSql());
        components.put("redis", checkRedis());
        components.put("minio", checkMinio());
        components.put("mlflow", checkMlflow());
        components.put("k8s", checkK8s());

        String status = "UP";
        for (Object value : components.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detail = (Map<String, Object>) value;
            Object s = detail.get("status");
            if ("DOWN".equals(s)) {
                status = "DOWN";
                break;
            }
        }

        Map<String, Object> health = new HashMap<>();
        health.put("status", status);
        health.put("timestamp", System.currentTimeMillis());
        health.put("components", components);
        return health;
    }

    private Map<String, Object> checkPostgreSql() {
        Map<String, Object> result = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(3);
            result.put("status", valid ? "UP" : "DOWN");
            result.put("database", connection.getCatalog());
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> result = new HashMap<>();
        RedisConnection connection = null;
        try {
            connection = redisConnectionFactory.getConnection();
            String pong = connection.ping();
            result.put("status", "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN");
            result.put("ping", pong);
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception closeError) {
                    log.debug("Redis connection close failed", closeError);
                }
            }
        }
        return result;
    }

    private Map<String, Object> checkMinio() {
        Map<String, Object> result = new HashMap<>();
        try {
            int bucketCount = minioClient.listBuckets().size();
            result.put("status", "UP");
            result.put("bucketCount", bucketCount);
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> checkMlflow() {
        Map<String, Object> result = new HashMap<>();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(mlflowConfig.getTrackingUri());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            int statusCode = connection.getResponseCode();

            result.put("status", statusCode >= 200 && statusCode < 500 ? "UP" : "DOWN");
            result.put("httpCode", statusCode);
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    private Map<String, Object> checkK8s() {
        Map<String, Object> result = new HashMap<>();
        K8sOperations k8sOperations = k8sOperationsProvider.getIfAvailable();
        if (k8sOperations == null) {
            result.put("status", "UNKNOWN");
            result.put("message", "k8s bean not enabled");
            return result;
        }

        try {
            int namespaceCount = k8sOperations.listNamespaces().size();
            result.put("status", "UP");
            result.put("namespaceCount", namespaceCount);
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return result;
    }
}
