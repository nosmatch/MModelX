package com.mogu.data.deployment.service;

import com.mogu.data.common.exception.BusinessException;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * K8s 客户端空实现
 * 当 K8s 不可用时使用，所有操作返回空结果或抛出业务异常
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "k8s", name = "enabled", havingValue = "false")
public class NoOpK8sClientService implements K8sOperations {

    private void logNoOp(String operation) {
        log.warn("K8s 不可用，{} 操作被忽略", operation);
    }

    private BusinessException notAvailable() {
        return new BusinessException("K8s 集群当前不可用，请检查 K8s 配置");
    }

    @Override
    public V1Deployment createOrUpdateDeployment(String namespace, String name, String image,
                                                  int replicas, Map<String, String> envVars,
                                                  String cpuRequest, String memoryRequest,
                                                  String cpuLimit, String memoryLimit,
                                                  String modelName, String modelVersion) {
        logNoOp("创建/更新 Deployment");
        throw notAvailable();
    }

    @Override
    public void deleteDeployment(String namespace, String name) {
        logNoOp("删除 Deployment");
    }

    @Override
    public void scaleDeployment(String namespace, String name, int replicas) {
        logNoOp("扩缩容");
        throw notAvailable();
    }

    @Override
    public Map<String, Object> getDeploymentStatus(String namespace, String name) {
        logNoOp("获取 Deployment 状态");
        Map<String, Object> status = new HashMap<>();
        status.put("exists", false);
        return status;
    }

    @Override
    public V1Service createOrUpdateService(String namespace, String name, int port) {
        logNoOp("创建/更新 Service");
        throw notAvailable();
    }

    @Override
    public void deleteService(String namespace, String name) {
        logNoOp("删除 Service");
    }

    @Override
    public V1ConfigMap createOrUpdateConfigMap(String namespace, String name, Map<String, String> data) {
        logNoOp("创建/更新 ConfigMap");
        throw notAvailable();
    }

    @Override
    public void deleteConfigMap(String namespace, String name) {
        logNoOp("删除 ConfigMap");
    }

    @Override
    public List<V1Pod> listPods(String namespace, String labelSelector) {
        logNoOp("列出 Pod");
        return new ArrayList<>();
    }

    @Override
    public String getPodLogs(String namespace, String podName, int tailLines) {
        logNoOp("获取 Pod 日志");
        return "K8s 不可用，无法获取日志";
    }

    @Override
    public void deletePod(String namespace, String podName) {
        logNoOp("删除 Pod");
    }

    @Override
    public List<V1Namespace> listNamespaces() {
        logNoOp("列出 Namespace");
        return new ArrayList<>();
    }

    @Override
    public void ensureNamespaceExists(String namespace) {
        logNoOp("确保 Namespace 存在");
    }

    @Override
    public void deleteAllResources(String namespace, String deploymentName, String serviceName, String configMapName) {
        logNoOp("清理 K8s 资源");
    }
}
