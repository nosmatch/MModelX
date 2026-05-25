package com.mogu.data.deployment.controller;

import com.mogu.data.common.entity.Deployment;
import com.mogu.data.common.entity.K8sNamespace;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.repository.K8sNamespaceRepository;
import com.mogu.data.common.result.Result;
import com.mogu.data.deployment.dto.DeploymentRequest;
import com.mogu.data.deployment.dto.DeploymentStatusDTO;
import com.mogu.data.deployment.dto.PodStatusDTO;
import com.mogu.data.deployment.service.DeploymentService;
import com.mogu.data.deployment.service.K8sOperations;
import io.kubernetes.client.openapi.models.V1Namespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型部署控制器
 * 管理 K8s 模型部署的全生命周期
 *
 * @author MModelX Team
 * @since 2026-05-24
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/deployment")
public class DeploymentController {

    private final DeploymentService deploymentService;
    private final K8sNamespaceRepository k8sNamespaceRepository;

    @Autowired(required = false)
    private K8sOperations k8sClientService;

    public DeploymentController(DeploymentService deploymentService,
                                 K8sNamespaceRepository k8sNamespaceRepository) {
        this.deploymentService = deploymentService;
        this.k8sNamespaceRepository = k8sNamespaceRepository;
    }

    // ==================== 部署管理 ====================

    /**
     * 部署模型到 K8s
     */
    @PostMapping("/deploy")
    public Result<Deployment> deployModel(@Valid @RequestBody DeploymentRequest request) {
        log.info("收到部署请求: modelId={}, namespace={}", request.getModelId(), request.getNamespace());
        Deployment deployment = deploymentService.deployModel(request);
        return Result.success(deployment);
    }

    /**
     * 下线部署
     */
    @DeleteMapping("/{id}")
    public Result<Void> undeploy(@PathVariable Long id) {
        log.info("收到下线请求: id={}", id);
        deploymentService.undeploy(id);
        return Result.success();
    }

    /**
     * 扩缩容
     */
    @PutMapping("/{id}/scale")
    public Result<Void> scale(@PathVariable Long id, @RequestParam int replicas) {
        log.info("收到扩缩容请求: id={}, replicas={}", id, replicas);
        deploymentService.scale(id, replicas);
        return Result.success();
    }

    /**
     * 获取部署状态
     */
    @GetMapping("/{id}/status")
    public Result<DeploymentStatusDTO> getStatus(@PathVariable Long id) {
        DeploymentStatusDTO status = deploymentService.getStatus(id);
        return Result.success(status);
    }

    /**
     * 获取部署详情
     */
    @GetMapping("/{id}")
    public Result<DeploymentStatusDTO> getDeployment(@PathVariable Long id) {
        DeploymentStatusDTO deployment = deploymentService.getDeployment(id);
        return Result.success(deployment);
    }

    // ==================== Pod 管理 ====================

    /**
     * 获取 Pod 列表
     */
    @GetMapping("/{id}/pods")
    public Result<List<PodStatusDTO>> getPods(@PathVariable Long id) {
        List<PodStatusDTO> pods = deploymentService.getPods(id);
        return Result.success(pods);
    }

    /**
     * 获取 Pod 日志
     */
    @GetMapping("/{id}/pods/{podName}/logs")
    public Result<String> getPodLogs(@PathVariable Long id,
                                      @PathVariable String podName,
                                      @RequestParam(defaultValue = "100") int tailLines) {
        String logs = deploymentService.getPodLogs(id, podName, tailLines);
        return Result.success(logs);
    }

    /**
     * 重启 Pod
     */
    @DeleteMapping("/{id}/pods/{podName}")
    public Result<Void> restartPod(@PathVariable Long id, @PathVariable String podName) {
        deploymentService.restartPod(id, podName);
        return Result.success();
    }

    // ==================== 列表查询 ====================

    /**
     * 列出所有部署
     */
    @GetMapping
    public Result<List<DeploymentStatusDTO>> listDeployments() {
        List<DeploymentStatusDTO> deployments = deploymentService.listDeployments();
        return Result.success(deployments);
    }

    /**
     * 按 namespace 列出部署
     */
    @GetMapping("/namespace/{namespace}")
    public Result<List<DeploymentStatusDTO>> listDeploymentsByNamespace(@PathVariable String namespace) {
        List<DeploymentStatusDTO> deployments = deploymentService.listDeploymentsByNamespace(namespace);
        return Result.success(deployments);
    }

    // ==================== Namespace 管理 ====================

    /**
     * 列出所有 K8s namespace（从数据库）
     */
    @GetMapping("/namespaces")
    public Result<List<K8sNamespace>> listNamespaces() {
        List<K8sNamespace> namespaces = k8sNamespaceRepository.findByStatus(K8sNamespace.Status.ACTIVE);
        return Result.success(namespaces);
    }

    /**
     * 列出 K8s 集群中的所有 namespace
     */
    @GetMapping("/namespaces/cluster")
    public Result<List<Map<String, String>>> listClusterNamespaces() {
        if (k8sClientService == null) {
            return Result.success(new ArrayList<>());
        }
        List<V1Namespace> nsList = k8sClientService.listNamespaces();
        List<Map<String, String>> result = new ArrayList<>();
        for (V1Namespace ns : nsList) {
            Map<String, String> item = new HashMap<>();
            item.put("name", ns.getMetadata().getName());
            item.put("status", ns.getStatus() != null ? ns.getStatus().getPhase() : "Unknown");
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 列出某 namespace 下的部署
     */
    @GetMapping("/namespaces/{namespace}/deployments")
    public Result<List<DeploymentStatusDTO>> getDeploymentsByNamespace(@PathVariable String namespace) {
        List<DeploymentStatusDTO> deployments = deploymentService.listDeploymentsByNamespace(namespace);
        return Result.success(deployments);
    }
}
