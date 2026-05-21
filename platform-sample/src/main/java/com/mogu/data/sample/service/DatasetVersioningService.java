package com.mogu.data.sample.service;

import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.storage.MinioService;
import com.mogu.data.common.storage.RedisService;
import com.mogu.data.sample.entity.Dataset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据集版本管理服务
 * 使用DVC进行数据版本控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetVersioningService {

    private final MinioService minioService;
    private final RedisService redisService;

    /**
     * 注册数据集版本
     * @param dataset 数据集
     * @return 版本号
     */
    public String registerDataset(Dataset dataset) {
        log.info("注册数据集版本: {}, 路径: {}", dataset.getName(), dataset.getPath());

        try {
            // 生成版本号
            String version = generateVersion();
            dataset.setVersion(version);
            dataset.setCreatedAt(LocalDateTime.now());
            dataset.setUpdatedAt(LocalDateTime.now());
            dataset.setStatus("REGISTERED");

            // 存储元数据
            String metadataKey = "dataset:metadata:" + dataset.getName() + ":" + version;
            redisService.set(metadataKey, dataset, 30, java.util.concurrent.TimeUnit.DAYS);

            // 存储版本索引
            String indexKey = "dataset:index:" + dataset.getName();
            redisService.hSet(indexKey, version, metadataKey);

            log.info("数据集版本注册成功: {}, 版本: {}", dataset.getName(), version);
            return version;

        } catch (Exception e) {
            log.error("注册数据集版本失败: {}", e.getMessage(), e);
            throw new BusinessException("注册数据集版本失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据集
     * @param name 数据集名称
     * @param version 版本号
     * @return 数据集
     */
    public Dataset getDataset(String name, String version) {
        log.info("获取数据集: {}, 版本: {}", name, version);

        try {
            String metadataKey = "dataset:metadata:" + name + ":" + version;
            Object datasetObj = redisService.get(metadataKey);

            if (datasetObj == null) {
                throw new BusinessException("数据集不存在: " + name + ":" + version);
            }

            // 这里需要反序列化
            // Dataset dataset = (Dataset) datasetObj;
            // return dataset;

            return null;

        } catch (Exception e) {
            log.error("获取数据集失败: {}", e.getMessage(), e);
            throw new BusinessException("获取数据集失败: " + e.getMessage());
        }
    }

    /**
     * 列出数据集的所有版本
     * @param name 数据集名称
     * @return 版本列表
     */
    public java.util.List<String> listDatasetVersions(String name) {
        log.info("列出数据集版本: {}", name);

        try {
            String indexKey = "dataset:index:" + name;
            Map<Object, Object> versions = redisService.hGetAll(indexKey);

            return versions.keySet().stream()
                    .map(Object::toString)
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("列出数据集版本失败: {}", e.getMessage(), e);
            throw new BusinessException("列出数据集版本失败: " + e.getMessage());
        }
    }

    /**
     * 删除数据集版本
     * @param name 数据集名称
     * @param version 版本号
     */
    public void deleteDataset(String name, String version) {
        log.info("删除数据集版本: {}, 版本: {}", name, version);

        try {
            String metadataKey = "dataset:metadata:" + name + ":" + version;
            Dataset dataset = getDataset(name, version);

            if (dataset != null) {
                dataset.setStatus("DELETED");
                dataset.setUpdatedAt(LocalDateTime.now());

                // 更新元数据
                redisService.set(metadataKey, dataset, 30, java.util.concurrent.TimeUnit.DAYS);

                // 从索引中删除
                String indexKey = "dataset:index:" + name;
                redisService.hDelete(indexKey, version);

                log.info("数据集版本删除成功: {}, 版本: {}", name, version);
            }

        } catch (Exception e) {
            log.error("删除数据集版本失败: {}", e.getMessage(), e);
            throw new BusinessException("删除数据集版本失败: " + e.getMessage());
        }
    }

    /**
     * 比较两个数据集版本
     * @param name 数据集名称
     * @param version1 版本1
     * @param version2 版本2
     * @return 比较结果
     */
    public Map<String, Object> compareDatasets(String name, String version1, String version2) {
        log.info("比较数据集版本: {}, {} vs {}", name, version1, version2);

        try {
            Dataset dataset1 = getDataset(name, version1);
            Dataset dataset2 = getDataset(name, version2);

            Map<String, Object> comparison = new HashMap<>();
            comparison.put("dataset1", dataset1);
            comparison.put("dataset2", dataset2);
            comparison.put("rowDiff", dataset2.getRowCount() - dataset1.getRowCount());
            comparison.put("timeDiff", dataset2.getCreatedAt().compareTo(dataset1.getCreatedAt()));

            return comparison;

        } catch (Exception e) {
            log.error("比较数据集版本失败: {}", e.getMessage(), e);
            throw new BusinessException("比较数据集版本失败: " + e.getMessage());
        }
    }

    /**
     * 生成版本号
     */
    private String generateVersion() {
        return "v" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 获取最新版本
     * @param name 数据集名称
     * @return 最新版本号
     */
    public String getLatestVersion(String name) {
        log.info("获取数据集最新版本: {}", name);

        try {
            java.util.List<String> versions = listDatasetVersions(name);
            if (versions.isEmpty()) {
                throw new BusinessException("数据集不存在: " + name);
            }

            return versions.get(versions.size() - 1);

        } catch (Exception e) {
            log.error("获取数据集最新版本失败: {}", e.getMessage(), e);
            throw new BusinessException("获取数据集最新版本失败: " + e.getMessage());
        }
    }
}