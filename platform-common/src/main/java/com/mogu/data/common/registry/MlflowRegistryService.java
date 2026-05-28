package com.mogu.data.common.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.dto.ExperimentDTO;
import com.mogu.data.common.dto.ModelDTO;
import com.mogu.data.common.entity.Experiment;
import com.mogu.data.common.entity.Model;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.repository.ExperimentRepository;
import com.mogu.data.common.repository.ModelRepository;
import com.mogu.data.common.storage.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MLflow模型注册服务
 * 管理实验追踪和模型注册
 * 移动到 platform-common，供 training 和 serving 共享
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MlflowRegistryService {

    // 内存中的实验和模型缓存
    private final Map<String, ExperimentDTO> experimentCache = new ConcurrentHashMap<>();
    private final Map<String, ModelDTO> modelCache = new ConcurrentHashMap<>();

    private final ExperimentRepository experimentRepository;
    private final ModelRepository modelRepository;
    private final MinioService minioService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建实验
     * @param experiment 实验实体
     * @return 实验ID
     */
    public Long createExperiment(ExperimentDTO experiment) {
        log.info("创建实验: {}", experiment.getName());

        try {
            Long id = System.currentTimeMillis();
            experiment.setId(id);
            experiment.setCreatedAt(LocalDateTime.now());
            experiment.setUpdatedAt(LocalDateTime.now());

            experimentCache.put(experiment.getName(), experiment);

            // 同步保存到数据库
            Experiment entity = new Experiment();
            entity.setName(experiment.getName());
            entity.setDescription(experiment.getDescription());
            entity.setModelType(experiment.getModelType());
            entity.setStatus(Experiment.ExperimentStatus.RUNNING);
            if (experiment.getParams() != null) {
                entity.setHyperparameters(objectMapper.valueToTree(experiment.getParams()));
            }
            experimentRepository.save(entity);

            log.info("实验创建成功: {}", experiment.getName());
            return id;

        } catch (Exception e) {
            log.error("创建实验失败: {}", e.getMessage(), e);
            throw new BusinessException("创建实验失败: " + e.getMessage());
        }
    }

    /**
     * 记录训练参数
     * @param experimentName 实验名称
     * @param params 参数
     */
    public void logParams(String experimentName, Map<String, Object> params) {
        log.info("记录训练参数: {}", experimentName);

        try {
            ExperimentDTO experiment = experimentCache.get(experimentName);
            if (experiment != null) {
                experiment.setParams(params);
                experiment.setUpdatedAt(LocalDateTime.now());
            }

        } catch (Exception e) {
            log.error("记录训练参数失败: {}", e.getMessage(), e);
            throw new BusinessException("记录训练参数失败: " + e.getMessage());
        }
    }

    /**
     * 记录训练指标
     * @param experimentName 实验名称
     * @param metrics 指标
     */
    public void logMetrics(String experimentName, Map<String, Double> metrics) {
        log.info("记录训练指标: {}", experimentName);

        try {
            ExperimentDTO experiment = experimentCache.get(experimentName);
            if (experiment != null) {
                if (experiment.getMetrics() == null) {
                    experiment.setMetrics(new HashMap<>());
                }
                experiment.getMetrics().putAll(metrics);
                experiment.setUpdatedAt(LocalDateTime.now());
            }

        } catch (Exception e) {
            log.error("记录训练指标失败: {}", e.getMessage(), e);
            throw new BusinessException("记录训练指标失败: " + e.getMessage());
        }
    }

    /**
     * 记录模型
     * @param experimentName 实验名称
     * @param modelPath 模型路径
     */
    public void logModel(String experimentName, String modelPath) {
        log.info("记录模型: {}, 路径: {}", experimentName, modelPath);

        try {
            ExperimentDTO experiment = experimentCache.get(experimentName);
            if (experiment != null) {
                experiment.setModelPath(modelPath);
                experiment.setUpdatedAt(LocalDateTime.now());
            }

        } catch (Exception e) {
            log.error("记录模型失败: {}", e.getMessage(), e);
            throw new BusinessException("记录模型失败: " + e.getMessage());
        }
    }

    /**
     * 完成实验
     * @param experimentName 实验名称
     * @param status 状态
     */
    public void endExperiment(String experimentName, String status) {
        log.info("结束实验: {}, 状态: {}", experimentName, status);

        try {
            ExperimentDTO experiment = experimentCache.get(experimentName);
            if (experiment != null) {
                experiment.setStatus(status);
                experiment.setUpdatedAt(LocalDateTime.now());
            }

        } catch (Exception e) {
            log.error("结束实验失败: {}", e.getMessage(), e);
            throw new BusinessException("结束实验失败: " + e.getMessage());
        }
    }

    /**
     * 注册模型
     * @param model 模型实体
     * @return 模型ID
     */
    public Long registerModel(ModelDTO model) {
        log.info("注册模型: {}", model.getName());

        try {
            Long id = System.currentTimeMillis();
            model.setId(id);
            model.setCreatedAt(LocalDateTime.now());
            model.setUpdatedAt(LocalDateTime.now());

            String modelKey = model.getName() + ":" + model.getVersion();
            modelCache.put(modelKey, model);

            // 同步保存到数据库
            Model entity = new Model();
            entity.setName(model.getName());
            entity.setVersion(model.getVersion());
            entity.setFilePath(model.getModelPath());
            entity.setFramework(model.getModelType());
            entity.setModelType(model.getModelType());
            if (model.getPerformance() != null) {
                entity.setMetrics(objectMapper.valueToTree(
                    java.util.Collections.singletonMap("performance", model.getPerformance())));
            }
            if (model.getStage() != null) {
                entity.setStage(Model.ModelStage.valueOf(model.getStage().toUpperCase()));
            }
            modelRepository.save(entity);

            log.info("模型注册成功: {}", model.getName());
            return id;

        } catch (Exception e) {
            log.error("模型注册失败: {}", e.getMessage(), e);
            throw new BusinessException("模型注册失败: " + e.getMessage());
        }
    }

    /**
     * 转换模型阶段
     * @param modelName 模型名称
     * @param version 版本
     * @param stage 阶段
     */
    public void transitionModelStage(String modelName, String version, String stage) {
        log.info("转换模型阶段: {}, 版本: {}, 阶段: {}", modelName, version, stage);

        try {
            String modelKey = modelName + ":" + version;
            ModelDTO model = modelCache.get(modelKey);
            if (model != null) {
                model.setStage(stage);
                model.setUpdatedAt(LocalDateTime.now());
            }

            // 同步更新数据库
            Optional<Model> optional = modelRepository.findByNameAndVersion(modelName, version);
            if (optional.isPresent()) {
                Model entity = optional.get();
                entity.setStage(Model.ModelStage.valueOf(stage.toUpperCase()));
                modelRepository.save(entity);
            }

        } catch (Exception e) {
            log.error("转换模型阶段失败: {}", e.getMessage(), e);
            throw new BusinessException("转换模型阶段失败: " + e.getMessage());
        }
    }

    /**
     * 获取生产环境模型
     * @param modelName 模型名称
     * @return 模型
     */
    public ModelDTO getProductionModel(String modelName) {
        log.info("获取生产环境模型: {}", modelName);

        try {
            // 优先从数据库查询
            Optional<Model> optional = modelRepository.findByNameAndStage(modelName, Model.ModelStage.PRODUCTION);
            if (optional.isPresent()) {
                Model entity = optional.get();
                ModelDTO dto = new ModelDTO();
                dto.setId(entity.getId());
                dto.setName(entity.getName());
                dto.setVersion(entity.getVersion());
                dto.setModelPath(entity.getFilePath());
                dto.setModelType(entity.getModelType());
                dto.setStage(entity.getStage() != null ? entity.getStage().name() : "Staging");
                dto.setCreatedAt(entity.getCreatedAt());
                dto.setUpdatedAt(entity.getUpdatedAt());
                return dto;
            }

            // fallback 到内存缓存
            for (Map.Entry<String, ModelDTO> entry : modelCache.entrySet()) {
                ModelDTO model = entry.getValue();
                if (model.getName().equals(modelName) && "Production".equals(model.getStage())) {
                    return model;
                }
            }

            throw new BusinessException("没有找到生产环境模型: " + modelName);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取生产环境模型失败: {}", e.getMessage(), e);
            throw new BusinessException("获取生产环境模型失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有实验
     * 优先从内存缓存获取，缓存为空时从数据库回查
     * @return 实验列表
     */
    public java.util.List<ExperimentDTO> listExperiments() {
        java.util.List<ExperimentDTO> result = new java.util.ArrayList<>();
        java.util.Set<String> cacheNames = new java.util.HashSet<>();

        // 1. 先从内存缓存取
        for (ExperimentDTO exp : experimentCache.values()) {
            result.add(exp);
            cacheNames.add(exp.getName());
        }

        // 2. 从数据库补充
        try {
            java.util.List<Experiment> dbExperiments = experimentRepository.findAll(
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
                )
            );
            for (Experiment entity : dbExperiments) {
                if (!cacheNames.contains(entity.getName())) {
                    ExperimentDTO dto = new ExperimentDTO();
                    dto.setId(entity.getId());
                    dto.setName(entity.getName());
                    dto.setDescription(entity.getDescription());
                    dto.setModelType(entity.getModelType());
                    dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : "RUNNING");
                    dto.setCreatedAt(entity.getCreatedAt());
                    dto.setUpdatedAt(entity.getUpdatedAt());
                    if (entity.getHyperparameters() != null) {
                        dto.setParams(objectMapper.convertValue(entity.getHyperparameters(), Map.class));
                    }
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            log.warn("从数据库查询实验列表失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 获取实验
     * @param name 实验名称
     * @return 实验
     */
    public ExperimentDTO getExperiment(String name) {
        ExperimentDTO experiment = experimentCache.get(name);
        if (experiment == null) {
            throw new BusinessException("实验不存在: " + name);
        }
        return experiment;
    }

    /**
     * 删除实验
     * @param name 实验名称
     */
    public void deleteExperiment(String name) {
        log.info("删除实验: {}", name);

        try {
            // 1. 从内存缓存移除
            experimentCache.remove(name);

            // 2. 从数据库删除
            java.util.List<Experiment> experiments = experimentRepository.findByName(name);
            if (!experiments.isEmpty()) {
                experimentRepository.deleteAll(experiments);
            }

            log.info("实验已删除: {}", name);
        } catch (Exception e) {
            log.error("删除实验失败: {}", e.getMessage(), e);
            throw new BusinessException("删除实验失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有模型
     * 优先从内存缓存获取，缓存为空时从数据库回查
     * @return 模型列表
     */
    public java.util.List<ModelDTO> listModels() {
        java.util.List<ModelDTO> result = new java.util.ArrayList<>();
        java.util.Set<String> cacheKeys = new java.util.HashSet<>();

        // 1. 先从内存缓存取
        for (ModelDTO model : modelCache.values()) {
            result.add(model);
            cacheKeys.add(model.getName() + ":" + model.getVersion());
        }

        // 2. 从数据库补充（缓存中没有的模型）
        try {
            java.util.List<Model> dbModels = modelRepository.findAll(
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "registeredAt"
                )
            );
            for (Model entity : dbModels) {
                String key = entity.getName() + ":" + entity.getVersion();
                if (!cacheKeys.contains(key)) {
                    ModelDTO dto = new ModelDTO();
                    dto.setId(entity.getId());
                    dto.setName(entity.getName());
                    dto.setVersion(entity.getVersion());
                    dto.setModelType(entity.getModelType());
                    dto.setModelPath(entity.getFilePath());
                    dto.setStage(entity.getStage() != null ? entity.getStage().name() : "Staging");
                    dto.setCreatedAt(entity.getCreatedAt());
                    dto.setUpdatedAt(entity.getUpdatedAt());
                    // 从 metrics jsonb 中提取 performance (auc)
                    if (entity.getMetrics() != null && entity.getMetrics().has("performance")) {
                        dto.setPerformance(entity.getMetrics().get("performance").asDouble());
                    }
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            log.warn("从数据库查询模型列表失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 删除模型（包括 MinIO 文件、数据库记录和内存缓存）
     * @param name 模型名称
     * @param version 模型版本
     */
    public void deleteModel(String name, String version) {
        log.info("删除模型: {}, 版本: {}", name, version);

        try {
            String modelKey = name + ":" + version;
            ModelDTO modelDTO = modelCache.get(modelKey);

            // 1. 删除 MinIO 中的模型文件
            String modelPath = modelDTO != null ? modelDTO.getModelPath() : null;
            if (modelPath == null || modelPath.isEmpty()) {
                // 从数据库中获取文件路径
                Optional<Model> optional = modelRepository.findByNameAndVersion(name, version);
                if (optional.isPresent()) {
                    modelPath = optional.get().getFilePath();
                }
            }

            if (modelPath != null && !modelPath.isEmpty()) {
                try {
                    String[] parts = modelPath.split("/", 2);
                    if (parts.length == 2) {
                        String bucket = parts[0];
                        String objectName = parts[1];
                        minioService.deleteFile(bucket, objectName);
                        log.info("已删除 MinIO 模型文件: {}/{}", bucket, objectName);

                        // 尝试删除同目录下的 metrics.json
                        String metricsObjectName = objectName.substring(0, objectName.lastIndexOf('/') + 1) + "metrics.json";
                        try {
                            minioService.deleteFile(bucket, metricsObjectName);
                            log.info("已删除 MinIO metrics 文件: {}/{}", bucket, metricsObjectName);
                        } catch (Exception e) {
                            log.debug("metrics 文件不存在或删除失败: {}", metricsObjectName);
                        }
                    }
                } catch (Exception e) {
                    log.warn("删除 MinIO 模型文件失败: {}, 错误: {}", modelPath, e.getMessage());
                }
            }

            // 2. 从内存缓存移除
            modelCache.remove(modelKey);

            // 3. 从数据库删除
            Optional<Model> optional = modelRepository.findByNameAndVersion(name, version);
            if (optional.isPresent()) {
                modelRepository.delete(optional.get());
            }

            log.info("模型已删除: {}:{}", name, version);
        } catch (Exception e) {
            log.error("删除模型失败: {}", e.getMessage(), e);
            throw new BusinessException("删除模型失败: " + e.getMessage());
        }
    }
}
