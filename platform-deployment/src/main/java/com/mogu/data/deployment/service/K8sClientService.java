package com.mogu.data.deployment.service;

import com.mogu.data.common.exception.BusinessException;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * K8s 客户端操作服务（真实实现）
 * 封装对 K8s API 的调用，管理 Deployment、Service、ConfigMap、Pod 等资源
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class K8sClientService implements K8sOperations {

    private final CoreV1Api coreV1Api;
    private final AppsV1Api appsV1Api;

    private static final String APP_LABEL = "app";
    private static final String MANAGED_BY_LABEL = "managed-by";
    private static final String MANAGED_BY_VALUE = "mmodelx";
    private static final String MODEL_NAME_LABEL = "model-name";
    private static final String MODEL_VERSION_LABEL = "model-version";

    @Override
    public V1Deployment createOrUpdateDeployment(String namespace, String name, String image,
                                                    int replicas, Map<String, String> envVars,
                                                    String cpuRequest, String memoryRequest,
                                                    String cpuLimit, String memoryLimit,
                                                    String modelName, String modelVersion) {
        log.info("创建/更新 Deployment: namespace={}, name={}, image={}, replicas={}",
                namespace, name, image, replicas);
        try {
            ensureNamespaceExists(namespace);

            V1Deployment existing = getDeployment(namespace, name);
            if (existing != null) {
                existing.getSpec().setReplicas(replicas);
                existing.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(image);
                updateContainerResources(existing, cpuRequest, memoryRequest, cpuLimit, memoryLimit);
                updateEnvVars(existing, envVars);

                return appsV1Api.replaceNamespacedDeployment(name, namespace, existing, null, null, null, null);
            }

            Map<String, String> labels = buildLabels(name, modelName, modelVersion);

            V1Deployment deployment = new V1Deployment()
                    .apiVersion("apps/v1")
                    .kind("Deployment")
                    .metadata(new V1ObjectMeta().name(name).labels(labels))
                    .spec(new V1DeploymentSpec()
                            .replicas(replicas)
                            .selector(new V1LabelSelector().matchLabels(labels))
                            .template(new V1PodTemplateSpec()
                                    .metadata(new V1ObjectMeta().labels(labels))
                                    .spec(new V1PodSpec()
                                            .containers(Collections.singletonList(
                                                    buildContainer(name, image, envVars,
                                                            cpuRequest, memoryRequest, cpuLimit, memoryLimit)
                                            ))
                                            .volumes(Collections.singletonList(
                                                    new V1Volume()
                                                            .name("model-config")
                                                            .configMap(new V1ConfigMapVolumeSource()
                                                                    .name(name + "-config"))
                                            )))));

            return appsV1Api.createNamespacedDeployment(namespace, deployment, null, null, null, null);
        } catch (ApiException e) {
            log.error("创建 Deployment 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("创建 Deployment 失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDeployment(String namespace, String name) {
        log.info("删除 Deployment: namespace={}, name={}", namespace, name);
        try {
            appsV1Api.deleteNamespacedDeployment(name, namespace, null, null, null, null, null, null);
            log.info("Deployment 删除成功: {}", name);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.warn("Deployment 不存在，跳过删除: {}", name);
                return;
            }
            log.error("删除 Deployment 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("删除 Deployment 失败: " + e.getMessage());
        }
    }

    @Override
    public void scaleDeployment(String namespace, String name, int replicas) {
        log.info("扩缩容 Deployment: namespace={}, name={}, replicas={}", namespace, name, replicas);
        try {
            V1Deployment deployment = getDeployment(namespace, name);
            if (deployment == null) {
                throw new BusinessException("Deployment 不存在: " + name);
            }
            deployment.getSpec().setReplicas(replicas);
            appsV1Api.replaceNamespacedDeployment(name, namespace, deployment, null, null, null, null);
            log.info("扩缩容成功: {} -> {} replicas", name, replicas);
        } catch (ApiException e) {
            log.error("扩缩容失败: {}", e.getResponseBody(), e);
            throw new BusinessException("扩缩容失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDeploymentStatus(String namespace, String name) {
        Map<String, Object> status = new HashMap<>();
        try {
            V1Deployment deployment = getDeployment(namespace, name);
            if (deployment == null) {
                status.put("exists", false);
                return status;
            }

            V1DeploymentStatus deployStatus = deployment.getStatus();
            status.put("exists", true);
            status.put("replicas", deployStatus.getReplicas() != null ? deployStatus.getReplicas() : 0);
            status.put("readyReplicas", deployStatus.getReadyReplicas() != null ? deployStatus.getReadyReplicas() : 0);
            status.put("availableReplicas", deployStatus.getAvailableReplicas() != null ? deployStatus.getAvailableReplicas() : 0);
            status.put("updatedReplicas", deployStatus.getUpdatedReplicas() != null ? deployStatus.getUpdatedReplicas() : 0);

            if (deployStatus.getConditions() != null) {
                List<Map<String, String>> conditions = new ArrayList<>();
                for (V1DeploymentCondition condition : deployStatus.getConditions()) {
                    Map<String, String> c = new HashMap<>();
                    c.put("type", condition.getType());
                    c.put("status", condition.getStatus());
                    c.put("reason", condition.getReason());
                    c.put("message", condition.getMessage());
                    conditions.add(c);
                }
                status.put("conditions", conditions);
            }

            return status;
        } catch (Exception e) {
            log.error("获取 Deployment 状态失败: {}", e.getMessage(), e);
            status.put("exists", false);
            status.put("error", e.getMessage());
            return status;
        }
    }

    @Override
    public V1Service createOrUpdateService(String namespace, String name, int port) {
        log.info("创建/更新 Service: namespace={}, name={}, port={}", namespace, name, port);
        try {
            ensureNamespaceExists(namespace);

            Map<String, String> labels = buildLabels(name, null, null);

            V1Service existing = getService(namespace, name);
            if (existing != null) {
                existing.getSpec().getPorts().get(0).setPort(port);
                return coreV1Api.replaceNamespacedService(name, namespace, existing, null, null, null, null);
            }

            V1Service service = new V1Service()
                    .apiVersion("v1")
                    .kind("Service")
                    .metadata(new V1ObjectMeta().name(name).labels(labels))
                    .spec(new V1ServiceSpec()
                            .selector(labels)
                            .type("ClusterIP")
                            .ports(Collections.singletonList(
                                    new V1ServicePort()
                                            .port(port)
                                            .targetPort(new IntOrString(port))
                                            .protocol("TCP")
                            )));

            return coreV1Api.createNamespacedService(namespace, service, null, null, null, null);
        } catch (ApiException e) {
            log.error("创建 Service 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("创建 Service 失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteService(String namespace, String name) {
        log.info("删除 Service: namespace={}, name={}", namespace, name);
        try {
            coreV1Api.deleteNamespacedService(name, namespace, null, null, null, null, null, null);
            log.info("Service 删除成功: {}", name);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.warn("Service 不存在，跳过删除: {}", name);
                return;
            }
            log.error("删除 Service 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("删除 Service 失败: " + e.getMessage());
        }
    }

    @Override
    public V1ConfigMap createOrUpdateConfigMap(String namespace, String name, Map<String, String> data) {
        log.info("创建/更新 ConfigMap: namespace={}, name={}", namespace, name);
        try {
            ensureNamespaceExists(namespace);

            V1ConfigMap existing = getConfigMap(namespace, name);
            if (existing != null) {
                existing.setData(data);
                return coreV1Api.replaceNamespacedConfigMap(name, namespace, existing, null, null, null, null);
            }

            V1ConfigMap configMap = new V1ConfigMap()
                    .apiVersion("v1")
                    .kind("ConfigMap")
                    .metadata(new V1ObjectMeta().name(name))
                    .data(data);

            return coreV1Api.createNamespacedConfigMap(namespace, configMap, null, null, null, null);
        } catch (ApiException e) {
            log.error("创建 ConfigMap 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("创建 ConfigMap 失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteConfigMap(String namespace, String name) {
        log.info("删除 ConfigMap: namespace={}, name={}", namespace, name);
        try {
            coreV1Api.deleteNamespacedConfigMap(name, namespace, null, null, null, null, null, null);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.warn("ConfigMap 不存在，跳过删除: {}", name);
                return;
            }
            log.error("删除 ConfigMap 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("删除 ConfigMap 失败: " + e.getMessage());
        }
    }

    @Override
    public List<V1Pod> listPods(String namespace, String labelSelector) {
        log.info("列出 Pod: namespace={}, labelSelector={}", namespace, labelSelector);
        try {
            V1PodList podList = coreV1Api.listNamespacedPod(namespace, null, null, null, null,
                    labelSelector, null, null, null, null, null);
            return podList.getItems();
        } catch (ApiException e) {
            log.error("列出 Pod 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("列出 Pod 失败: " + e.getMessage());
        }
    }

    @Override
    public String getPodLogs(String namespace, String podName, int tailLines) {
        log.info("获取 Pod 日志: namespace={}, podName={}, tailLines={}", namespace, podName, tailLines);
        try {
            return coreV1Api.readNamespacedPodLog(podName, namespace, null, false, null,
                    null, null, null, tailLines, null, null);
        } catch (ApiException e) {
            log.error("获取 Pod 日志失败: {}", e.getResponseBody(), e);
            throw new BusinessException("获取 Pod 日志失败: " + e.getMessage());
        }
    }

    @Override
    public void deletePod(String namespace, String podName) {
        log.info("删除 Pod: namespace={}, podName={}", namespace, podName);
        try {
            coreV1Api.deleteNamespacedPod(podName, namespace, null, null, null, null, null, null);
            log.info("Pod 删除成功: {}", podName);
        } catch (ApiException e) {
            log.error("删除 Pod 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("删除 Pod 失败: " + e.getMessage());
        }
    }

    @Override
    public List<V1Namespace> listNamespaces() {
        log.info("列出所有 Namespace");
        try {
            V1NamespaceList namespaceList = coreV1Api.listNamespace(null, null, null, null,
                    null, null, null, null, null, null);
            return namespaceList.getItems();
        } catch (ApiException e) {
            log.error("列出 Namespace 失败: {}", e.getResponseBody(), e);
            throw new BusinessException("列出 Namespace 失败: " + e.getMessage());
        }
    }

    @Override
    public void ensureNamespaceExists(String namespace) {
        try {
            coreV1Api.readNamespace(namespace, null);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.info("Namespace 不存在，创建: {}", namespace);
                try {
                    V1Namespace ns = new V1Namespace()
                            .apiVersion("v1")
                            .kind("Namespace")
                            .metadata(new V1ObjectMeta().name(namespace));
                    coreV1Api.createNamespace(ns, null, null, null, null);
                } catch (ApiException createEx) {
                    log.error("创建 Namespace 失败: {}", createEx.getResponseBody(), createEx);
                    throw new BusinessException("创建 Namespace 失败: " + createEx.getMessage());
                }
            } else {
                log.error("检查 Namespace 失败: {}", e.getResponseBody(), e);
                throw new BusinessException("检查 Namespace 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public void deleteAllResources(String namespace, String deploymentName, String serviceName, String configMapName) {
        log.info("清理 K8s 资源: namespace={}, deployment={}", namespace, deploymentName);
        try {
            deleteDeployment(namespace, deploymentName);
        } catch (Exception e) {
            log.warn("删除 Deployment 失败（可能已不存在）: {}", e.getMessage());
        }
        try {
            deleteService(namespace, serviceName);
        } catch (Exception e) {
            log.warn("删除 Service 失败（可能已不存在）: {}", e.getMessage());
        }
        try {
            deleteConfigMap(namespace, configMapName);
        } catch (Exception e) {
            log.warn("删除 ConfigMap 失败（可能已不存在）: {}", e.getMessage());
        }
    }

    private V1Deployment getDeployment(String namespace, String name) {
        try {
            return appsV1Api.readNamespacedDeployment(name, namespace, null);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return null;
            }
            throw new BusinessException("读取 Deployment 失败: " + e.getMessage());
        }
    }

    private V1Service getService(String namespace, String name) {
        try {
            return coreV1Api.readNamespacedService(name, namespace, null);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return null;
            }
            throw new BusinessException("读取 Service 失败: " + e.getMessage());
        }
    }

    private V1ConfigMap getConfigMap(String namespace, String name) {
        try {
            return coreV1Api.readNamespacedConfigMap(name, namespace, null);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return null;
            }
            throw new BusinessException("读取 ConfigMap 失败: " + e.getMessage());
        }
    }

    private Map<String, String> buildLabels(String name, String modelName, String modelVersion) {
        Map<String, String> labels = new HashMap<>();
        labels.put(APP_LABEL, name);
        labels.put(MANAGED_BY_LABEL, MANAGED_BY_VALUE);
        if (modelName != null) {
            labels.put(MODEL_NAME_LABEL, modelName);
        }
        if (modelVersion != null) {
            labels.put(MODEL_VERSION_LABEL, modelVersion);
        }
        return labels;
    }

    private V1Container buildContainer(String name, String image, Map<String, String> envVars,
                                        String cpuRequest, String memoryRequest,
                                        String cpuLimit, String memoryLimit) {
        V1ResourceRequirements resources = new V1ResourceRequirements();
        Map<String, Quantity> requests = new HashMap<>();
        requests.put("cpu", new Quantity(cpuRequest));
        requests.put("memory", new Quantity(memoryRequest));
        resources.setRequests(requests);

        Map<String, Quantity> limits = new HashMap<>();
        limits.put("cpu", new Quantity(cpuLimit));
        limits.put("memory", new Quantity(memoryLimit));
        resources.setLimits(limits);

        List<V1EnvVar> envList = new ArrayList<>();
        if (envVars != null) {
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                envList.add(new V1EnvVar().name(entry.getKey()).value(entry.getValue()));
            }
        }

        V1VolumeMount volumeMount = new V1VolumeMount()
                .name("model-config")
                .mountPath("/etc/model-config");

        return new V1Container()
                .name(name)
                .image(image)
                .imagePullPolicy("IfNotPresent")
                .ports(Collections.singletonList(new V1ContainerPort().containerPort(8080).protocol("TCP")))
                .env(envList)
                .resources(resources)
                .volumeMounts(Collections.singletonList(volumeMount))
                .livenessProbe(new V1Probe()
                        .httpGet(new V1HTTPGetAction()
                                .path("/health")
                                .port(new IntOrString(8080)))
                        .initialDelaySeconds(30)
                        .periodSeconds(10))
                .readinessProbe(new V1Probe()
                        .httpGet(new V1HTTPGetAction()
                                .path("/health")
                                .port(new IntOrString(8080)))
                        .initialDelaySeconds(5)
                        .periodSeconds(5));
    }

    private void updateContainerResources(V1Deployment deployment, String cpuRequest, String memoryRequest,
                                           String cpuLimit, String memoryLimit) {
        V1ResourceRequirements resources = new V1ResourceRequirements();
        Map<String, Quantity> requests = new HashMap<>();
        requests.put("cpu", new Quantity(cpuRequest));
        requests.put("memory", new Quantity(memoryRequest));
        resources.setRequests(requests);

        Map<String, Quantity> limits = new HashMap<>();
        limits.put("cpu", new Quantity(cpuLimit));
        limits.put("memory", new Quantity(memoryLimit));
        resources.setLimits(limits);

        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setResources(resources);
    }

    private void updateEnvVars(V1Deployment deployment, Map<String, String> envVars) {
        List<V1EnvVar> envList = new ArrayList<>();
        if (envVars != null) {
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                envList.add(new V1EnvVar().name(entry.getKey()).value(entry.getValue()));
            }
        }
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(envList);
    }
}
