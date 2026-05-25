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
     * @return 实验列表
     */
    public java.util.List<ExperimentDTO> listExperiments() {
        return experimentCache.values().stream().collect(java.util.stream.Collectors.toList());
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
     * 列出所有模型
     * @return 模型列表
     */
    public java.util.List<ModelDTO> listModels() {
        return modelCache.values().stream().collect(java.util.stream.Collectors.toList());
    }
}
