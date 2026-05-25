package com.mogu.data.deployment.service;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;
import java.util.Map;

/**
 * K8s 操作接口
 * 定义所有 K8s 资源操作的标准契约
 */
public interface K8sOperations {

    V1Deployment createOrUpdateDeployment(String namespace, String name, String image,
                                           int replicas, Map<String, String> envVars,
                                           String cpuRequest, String memoryRequest,
                                           String cpuLimit, String memoryLimit,
                                           String modelName, String modelVersion);

    void deleteDeployment(String namespace, String name);

    void scaleDeployment(String namespace, String name, int replicas);

    Map<String, Object> getDeploymentStatus(String namespace, String name);

    V1Service createOrUpdateService(String namespace, String name, int port);

    void deleteService(String namespace, String name);

    V1ConfigMap createOrUpdateConfigMap(String namespace, String name, Map<String, String> data);

    void deleteConfigMap(String namespace, String name);

    List<V1Pod> listPods(String namespace, String labelSelector);

    String getPodLogs(String namespace, String podName, int tailLines);

    void deletePod(String namespace, String podName);

    List<V1Namespace> listNamespaces();

    void ensureNamespaceExists(String namespace);

    void deleteAllResources(String namespace, String deploymentName, String serviceName, String configMapName);
}
