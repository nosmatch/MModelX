package com.mogu.data.deployment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.Deployment;
import com.mogu.data.common.entity.K8sNamespace;
import com.mogu.data.common.entity.Model;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.repository.DeploymentRepository;
import com.mogu.data.common.repository.K8sNamespaceRepository;
import com.mogu.data.common.repository.ModelRepository;
import com.mogu.data.common.storage.MinioService;
import com.mogu.data.deployment.dto.DeploymentRequest;
import com.mogu.data.deployment.dto.DeploymentStatusDTO;
import com.mogu.data.deployment.dto.PodStatusDTO;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 模型部署业务服务
 * 编排 K8s 资源创建、状态监控、扩缩容等操作
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final K8sOperations k8sClientService;
    private final DeploymentRepository deploymentRepository;
    private final ModelRepository modelRepository;
    private final K8sNamespaceRepository k8sNamespaceRepository;
    private final MinioService minioService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${inference.image:mmodelx-inference:latest}")
    private String defaultInferenceImage;

    @Value("${inference.port:8080}")
    private int inferencePort;

    @Value("${minio.endpoint:http://localhost:9002}")
    private String minioEndpoint;

    @Value("${minio.access-key:minioadmin}")
    private String minioAccessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String minioSecretKey;

    // ==================== 核心部署操作 ====================

    /**
     * 部署模型到 K8s
     */
    @Transactional
    public Deployment deployModel(DeploymentRequest request) {
        log.info("开始部署模型: modelId={}, namespace={}, replicas={}",
                request.getModelId(), request.getNamespace(), request.getReplicas());

        // 1. 校验模型存在
        Model model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new BusinessException("模型不存在: " + request.getModelId()));

        // 2. 校验 namespace 存在且活跃
        K8sNamespace ns = k8sNamespaceRepository.findByNameAndStatus(request.getNamespace(), K8sNamespace.Status.ACTIVE)
                .orElseThrow(() -> new BusinessException("Namespace 不存在或未激活: " + request.getNamespace()));

        // 3. 生成 K8s 资源名称
        String deploymentName = buildK8sResourceName(model.getName(), model.getVersion());
        String serviceName = deploymentName + "-svc";
        String configMapName = deploymentName + "-config";

        // 4. 构建 ConfigMap 数据（模型配置）
        Map<String, String> configMapData = buildConfigMapData(model);

        // 5. 构建环境变量
        Map<String, String> envVars = new HashMap<>();
        envVars.put("MODEL_NAME", model.getName());
        envVars.put("MODEL_VERSION", model.getVersion());
        envVars.put("MODEL_FRAMEWORK", model.getFramework());
        envVars.put("MINIO_ENDPOINT", minioEndpoint);
        envVars.put("MINIO_ACCESS_KEY", minioAccessKey);
        envVars.put("MINIO_SECRET_KEY", minioSecretKey);

        // 6. 创建 K8s 资源
        String image = request.getImage() != null ? request.getImage() : defaultInferenceImage;

        k8sClientService.createOrUpdateConfigMap(request.getNamespace(), configMapName, configMapData);
        k8sClientService.createOrUpdateDeployment(
                request.getNamespace(), deploymentName, image, request.getReplicas(), envVars,
                request.getCpuRequest(), request.getMemoryRequest(),
                request.getCpuLimit(), request.getMemoryLimit(),
                model.getName(), model.getVersion()
        );
        k8sClientService.createOrUpdateService(request.getNamespace(), serviceName, inferencePort);

        // 7. 保存或更新部署记录
        Deployment deployment = deploymentRepository.findByModelIdAndStatus(model.getId(), Deployment.DeploymentStatus.RUNNING)
                .orElse(new Deployment());

        deployment.setModel(model);
        deployment.setEnvironment(Deployment.Environment.PRODUCTION);
        deployment.setTrafficPercentage(request.getTrafficPercentage());
        deployment.setStatus(Deployment.DeploymentStatus.DEPLOYING);
        deployment.setNamespace(request.getNamespace());
        deployment.setReplicas(request.getReplicas());
        deployment.setImage(image);
        deployment.setCpuRequest(request.getCpuRequest());
        deployment.setMemoryRequest(request.getMemoryRequest());
        deployment.setCpuLimit(request.getCpuLimit());
        deployment.setMemoryLimit(request.getMemoryLimit());
        deployment.setServiceName(serviceName);
        deployment.setDeploymentName(deploymentName);
        deployment.setK8sStatus("Deploying");
        deployment.setDeployedAt(LocalDateTime.now());

        Deployment saved = deploymentRepository.save(deployment);
        log.info("部署记录已保存: id={}", saved.getId());

        // 8. 异步轮询状态（简化版：后续可改为异步任务）
        pollDeploymentStatus(saved);

        return saved;
    }

    /**
     * 下线部署
     */
    @Transactional
    public void undeploy(Long deploymentId) {
        log.info("下线部署: id={}", deploymentId);
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new BusinessException("部署不存在: " + deploymentId));

        String namespace = deployment.getNamespace();
        String deploymentName = deployment.getDeploymentName();
        String serviceName = deployment.getServiceName();
        String configMapName = deploymentName + "-config";

        // 删除 K8s 资源
        k8sClientService.deleteAllResources(namespace, deploymentName, serviceName, configMapName);

        // 更新数据库状态
        deployment.setStatus(Deployment.DeploymentStatus.STOPPED);
        deployment.setK8sStatus("Stopped");
        deployment.setAvailableReplicas(0);
        deployment.setReadyReplicas(0);
        deploymentRepository.save(deployment);

        log.info("部署已下线: id={}", deploymentId);
    }

    /**
     * 扩缩容
     */
    @Transactional
    public void scale(Long deploymentId, int replicas) {
        log.info("扩缩容: id={}, replicas={}", deploymentId, replicas);
        if (replicas < 1 || replicas > 100) {
            throw new BusinessException("副本数必须在 1-100 之间");
        }

        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new BusinessException("部署不存在: " + deploymentId));

        k8sClientService.scaleDeployment(deployment.getNamespace(), deployment.getDeploymentName(), replicas);

        deployment.setReplicas(replicas);
        deploymentRepository.save(deployment);

        log.info("扩缩容成功: id={} -> {} replicas", deploymentId, replicas);
    }

    // ==================== 状态查询 ====================

    /**
     * 获取部署状态
     */
    public DeploymentStatusDTO getStatus(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new BusinessException("部署不存在: " + deploymentId));

        DeploymentStatusDTO dto = new DeploymentStatusDTO();
        dto.setDeploymentId(deployment.getId());
        dto.setModelName(deployment.getModel().getName());
        dto.setModelVersion(deployment.getModel().getVersion());
        dto.setNamespace(deployment.getNamespace());
        dto.setDeploymentName(deployment.getDeploymentName());
        dto.setServiceName(deployment.getServiceName());
        dto.setStatus(deployment.getStatus().name());
        dto.setK8sStatus(deployment.getK8sStatus());
        dto.setReplicas(deployment.getReplicas());
        dto.setEndpointUrl(deployment.getEndpointUrl());
        dto.setDeployedAt(deployment.getDeployedAt());

        // 从 K8s 获取实时状态
        if (deployment.getNamespace() != null && deployment.getDeploymentName() != null) {
            Map<String, Object> k8sStatus = k8sClientService.getDeploymentStatus(
                    deployment.getNamespace(), deployment.getDeploymentName());

            if (Boolean.TRUE.equals(k8sStatus.get("exists"))) {
                Integer ready = (Integer) k8sStatus.getOrDefault("readyReplicas", 0);
                Integer available = (Integer) k8sStatus.getOrDefault("availableReplicas", 0);
                dto.setReadyReplicas(ready);
                dto.setAvailableReplicas(available);
                dto.setConditions((List<Map<String, String>>) k8sStatus.get("conditions"));

                // 自动更新状态：如果副本全部就绪且状态为 DEPLOYING，则改为 RUNNING
                if (deployment.getStatus() == Deployment.DeploymentStatus.DEPLOYING
                        && ready != null && ready >= deployment.getReplicas()) {
                    deployment.setStatus(Deployment.DeploymentStatus.RUNNING);
                    deployment.setK8sStatus("Running");
                    deployment.setEndpointUrl("http://" + deployment.getServiceName() + "."
                            + deployment.getNamespace() + ".svc.cluster.local:" + inferencePort);
                    deploymentRepository.save(deployment);
                    dto.setStatus(Deployment.DeploymentStatus.RUNNING.name());
                    dto.setK8sStatus("Running");
                }
            }
        }

        return dto;
    }

    /**
     * 获取 Pod 列表
     */
    public List<PodStatusDTO> getPods(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new BusinessException("部署不存在: " + deploymentId));

        String namespace = deployment.getNamespace();
        String deploymentName = deployment.getDeploymentName();
        String labelSelector = "app=" + deploymentName + ",managed-by=mmodelx";

        List<V1Pod> pods = k8sClientService.listPods(namespace, labelSelector);
        List<PodStatusDTO> result = new ArrayList<>();

        for (V1Pod pod : pods) {
            PodStatusDTO dto = new PodStatusDTO();
            dto.setName(pod.getMetadata().getName());
            dto.setNamespace(pod.getMetadata().getNamespace());
            dto.setPhase(pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown");
            dto.setPodIp(pod.getStatus() != null ? pod.getStatus().getPodIP() : null);
            dto.setNodeName(pod.getSpec() != null ? pod.getSpec().getNodeName() : null);
            dto.setImage(pod.getSpec() != null && !pod.getSpec().getContainers().isEmpty()
                    ? pod.getSpec().getContainers().get(0).getImage() : null);

            // 计算重启次数
            int restartCount = 0;
            if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
                for (io.kubernetes.client.openapi.models.V1ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                    restartCount += cs.getRestartCount() != null ? cs.getRestartCount() : 0;
                }
            }
            dto.setRestartCount(String.valueOf(restartCount));

            // Ready 状态
            boolean ready = false;
            if (pod.getStatus() != null && pod.getStatus().getConditions() != null) {
                for (V1PodCondition condition : pod.getStatus().getConditions()) {
                    if ("Ready".equals(condition.getType())) {
                        ready = "True".equals(condition.getStatus());
                        break;
                    }
                }
            }
            dto.setReady(ready ? "True" : "False");

            // 开始时间
            if (pod.getStatus() != null && pod.getStatus().getStartTime() != null) {
                OffsetDateTime odt = pod.getStatus().getStartTime();
                dto.setStartTime(odt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            }

            // 简化状态
            if ("Running".equals(dto.getPhase()) && ready) {
                dto.setStatus("Running");
            } else if ("Pending".equals(dto.getPhase())) {
                dto.setStatus("Pending");
            } else if ("Failed".equals(dto.getPhase())) {
                dto.setStatus("Failed");
            } else if ("Succeeded".equals(dto.getPhase())) {
                dto.setStatus("Succeeded");
            } else {
                dto.setStatus(dto.getPhase());
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * 获取 Pod 日志
     */
    public String getPodLogs(Long deploymentId, String podName, int tailLines) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new BusinessException("部署不存在: " + deploymentId));
        return k8sClientService.getPodLogs(deployment.getNamespace(), podName, tailLines);
    }

    /**
     * 重启 Pod
     */
    public void restartPod(Long deploymentId, String podName) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new BusinessException("部署不存在: " + deploymentId));
        k8sClientService.deletePod(deployment.getNamespace(), podName);
    }

    // ==================== 列表查询 ====================

    /**
     * 列出所有部署（返回 DTO，避免 Jackson 序列化深层关联）
     */
    @Transactional(readOnly = true)
    public List<DeploymentStatusDTO> listDeployments() {
        List<Deployment> deployments = deploymentRepository.findAll();
        List<DeploymentStatusDTO> result = new ArrayList<>();
        for (Deployment d : deployments) {
            enrichK8sStatus(d);
            result.add(convertToStatusDTO(d));
        }
        return result;
    }

    /**
     * 按 namespace 列出部署
     */
    @Transactional(readOnly = true)
    public List<DeploymentStatusDTO> listDeploymentsByNamespace(String namespace) {
        List<Deployment> deployments = deploymentRepository.findAll();
        List<DeploymentStatusDTO> result = new ArrayList<>();
        for (Deployment d : deployments) {
            if (namespace.equals(d.getNamespace())) {
                enrichK8sStatus(d);
                result.add(convertToStatusDTO(d));
            }
        }
        return result;
    }

    /**
     * 获取部署详情
     */
    @Transactional(readOnly = true)
    public DeploymentStatusDTO getDeployment(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new BusinessException("部署不存在: " + deploymentId));
        enrichK8sStatus(deployment);
        return convertToStatusDTO(deployment);
    }

    // ==================== 内部方法 ====================

    private String buildK8sResourceName(String modelName, String modelVersion) {
        return "mmodelx-" + modelName.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + modelVersion.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    private Map<String, String> buildConfigMapData(Model model) {
        Map<String, String> data = new HashMap<>();
        try {
            Map<String, Object> modelConfig = new HashMap<>();
            modelConfig.put("modelName", model.getName());
            modelConfig.put("modelVersion", model.getVersion());
            modelConfig.put("framework", model.getFramework());
            modelConfig.put("modelType", model.getModelType());
            modelConfig.put("filePath", model.getFilePath());
            modelConfig.put("minioEndpoint", minioEndpoint);
            modelConfig.put("minioBucket", "mmodelx");

            data.put("model.json", objectMapper.writeValueAsString(modelConfig));
        } catch (Exception e) {
            log.error("构建 ConfigMap 数据失败", e);
            throw new BusinessException("构建模型配置失败: " + e.getMessage());
        }
        return data;
    }

    private void pollDeploymentStatus(Deployment deployment) {
        // 简化版：直接同步查询一次状态
        // 生产环境建议使用 @Async 或 Spring Scheduler 轮询
        try {
            Map<String, Object> status = k8sClientService.getDeploymentStatus(
                    deployment.getNamespace(), deployment.getDeploymentName());

            if (Boolean.TRUE.equals(status.get("exists"))) {
                Integer ready = (Integer) status.getOrDefault("readyReplicas", 0);
                Integer available = (Integer) status.getOrDefault("availableReplicas", 0);
                Integer desired = deployment.getReplicas();

                deployment.setReadyReplicas(ready);
                deployment.setAvailableReplicas(available);

                if (ready != null && ready >= desired) {
                    deployment.setStatus(Deployment.DeploymentStatus.RUNNING);
                    deployment.setK8sStatus("Running");
                    deployment.setEndpointUrl("http://" + deployment.getServiceName() + "."
                            + deployment.getNamespace() + ".svc.cluster.local:" + inferencePort);
                } else {
                    deployment.setK8sStatus("Scaling");
                }
            } else {
                deployment.setK8sStatus("NotFound");
            }

            deploymentRepository.save(deployment);
        } catch (Exception e) {
            log.error("轮询部署状态失败: {}", e.getMessage(), e);
        }
    }

    private DeploymentStatusDTO convertToStatusDTO(Deployment deployment) {
        DeploymentStatusDTO dto = new DeploymentStatusDTO();
        dto.setId(deployment.getId());
        dto.setDeploymentId(deployment.getId());
        if (deployment.getModel() != null) {
            dto.setModelId(deployment.getModel().getId());
            dto.setModelName(deployment.getModel().getName());
            dto.setModelVersion(deployment.getModel().getVersion());
        }
        dto.setNamespace(deployment.getNamespace());
        dto.setDeploymentName(deployment.getDeploymentName());
        dto.setServiceName(deployment.getServiceName());
        dto.setStatus(deployment.getStatus() != null ? deployment.getStatus().name() : null);
        dto.setK8sStatus(deployment.getK8sStatus());
        dto.setReplicas(deployment.getReplicas());
        dto.setReadyReplicas(deployment.getReadyReplicas());
        dto.setAvailableReplicas(deployment.getAvailableReplicas());
        dto.setEndpointUrl(deployment.getEndpointUrl());
        dto.setImage(deployment.getImage());
        dto.setDeployedAt(deployment.getDeployedAt());
        return dto;
    }

    private void enrichK8sStatus(Deployment deployment) {
        if (deployment.getNamespace() == null || deployment.getDeploymentName() == null) {
            return;
        }
        try {
            Map<String, Object> status = k8sClientService.getDeploymentStatus(
                    deployment.getNamespace(), deployment.getDeploymentName());
            if (Boolean.TRUE.equals(status.get("exists"))) {
                Integer ready = (Integer) status.getOrDefault("readyReplicas", 0);
                Integer available = (Integer) status.getOrDefault("availableReplicas", 0);
                deployment.setReadyReplicas(ready);
                deployment.setAvailableReplicas(available);

                if (deployment.getStatus() == Deployment.DeploymentStatus.DEPLOYING
                        && ready != null && ready >= deployment.getReplicas()) {
                    deployment.setStatus(Deployment.DeploymentStatus.RUNNING);
                    deployment.setK8sStatus("Running");
                    deployment.setEndpointUrl("http://" + deployment.getServiceName() + "."
                            + deployment.getNamespace() + ".svc.cluster.local:" + inferencePort);
                    deploymentRepository.save(deployment);
                }
            }
        } catch (Exception e) {
            log.warn("补充 K8s 状态失败: {}", e.getMessage());
        }
    }
}
