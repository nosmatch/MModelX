package com.mogu.data.sample.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.Dataset;
import com.mogu.data.common.entity.DatasetVersion;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.logger.Logger;
import com.mogu.data.common.repository.DatasetRepository;
import com.mogu.data.common.repository.DatasetVersionRepository;
import com.mogu.data.common.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据集版本管理服务
 * 使用 PostgreSQL 进行数据版本控制
 *
 * @author MModelX Team
 * @since 2026-05-23
 */
@Service
@RequiredArgsConstructor
public class DatasetVersioningService {

    private static final Logger log = Logger.getLogger(DatasetVersioningService.class);

    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    private static final String SAMPLES_BUCKET = "samples";

    /**
     * 从构建任务注册数据集版本
     */
    @Transactional
    public String registerFromBuild(String sampleName,
                                     String labelColumn,
                                     Map<String, String> splitPaths,
                                     int rowCount,
                                     Integer featureCount,
                                     Map<String, Object> qualityReport) {
        log.info("注册数据集版本: {}, 样本数: {}", sampleName, rowCount);

        try {
            // 1. 查找或创建数据集
            List<Dataset> existing = datasetRepository.findByName(sampleName);
            Dataset dataset;
            if (existing.isEmpty()) {
                dataset = new Dataset();
                dataset.setName(sampleName);
                dataset.setVersion("latest");
                dataset.setLabelColumn(labelColumn != null ? labelColumn : "label");
                dataset.setDescription("Auto-generated from sample build");
                dataset.setStatus(Dataset.DatasetStatus.COMPLETED);
                dataset.setPointInTimeEnabled(true);
                try {
                    dataset.setFeatureViewIds(objectMapper.valueToTree(new ArrayList<>()));
                    dataset.setSplitRatio(objectMapper.valueToTree(new HashMap<>()));
                } catch (Exception e) {
                    log.warn("设置默认 JSON 字段失败: {}", e.getMessage());
                }
                dataset = datasetRepository.save(dataset);
            } else {
                dataset = existing.get(0);
            }

            // 2. 生成版本号
            String version = generateVersion();

            // 3. 创建版本记录
            DatasetVersion datasetVersion = new DatasetVersion();
            datasetVersion.setDatasetId(dataset.getId());
            datasetVersion.setVersion(version);
            datasetVersion.setVersionTag("latest");
            datasetVersion.setDataPath(splitPaths.get("output"));
            datasetVersion.setTrainPath(splitPaths.get("train"));
            datasetVersion.setValPath(splitPaths.get("val"));
            datasetVersion.setTestPath(splitPaths.get("test"));
            datasetVersion.setRowCount((long) rowCount);
            datasetVersion.setFeatureCount(featureCount);
            datasetVersion.setStatus(DatasetVersion.VersionStatus.READY);

            // 清除旧版本标签
            clearLatestTag(dataset.getId());

            datasetVersionRepository.save(datasetVersion);

            // 4. 更新数据集统计
            dataset.setSampleCount((long) rowCount);
            dataset.setFeatureCount(featureCount);
            dataset.setUpdatedAt(LocalDateTime.now());
            datasetRepository.save(dataset);

            log.info("数据集版本注册成功: {}, 版本: {}", sampleName, version);
            return version;

        } catch (Exception e) {
            log.error("注册数据集版本失败: {}", e.getMessage(), e);
            throw new BusinessException("注册数据集版本失败: " + e.getMessage());
        }
    }

    /**
     * 注册数据集（兼容旧接口）
     */
    @Transactional
    public String registerDataset(Dataset dataset) {
        log.info("注册数据集: {}", dataset.getName());

        try {
            // 保存数据集
            dataset.setStatus(Dataset.DatasetStatus.BUILDING);
            dataset.setCreatedAt(LocalDateTime.now());
            dataset.setUpdatedAt(LocalDateTime.now());
            Dataset saved = datasetRepository.save(dataset);

            // 创建初始版本
            String version = generateVersion();
            DatasetVersion datasetVersion = new DatasetVersion();
            datasetVersion.setDatasetId(saved.getId());
            datasetVersion.setVersion(version);
            datasetVersion.setVersionTag("latest");
            datasetVersion.setStatus(DatasetVersion.VersionStatus.CREATED);
            datasetVersionRepository.save(datasetVersion);

            return version;

        } catch (Exception e) {
            log.error("注册数据集失败: {}", e.getMessage(), e);
            throw new BusinessException("注册数据集失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据集
     */
    public Dataset getDataset(String name, String version) {
        log.info("获取数据集: {}, 版本: {}", name, version);

        return datasetRepository.findByNameAndVersion(name, version)
                .orElseThrow(() -> new BusinessException("数据集不存在: " + name + ":" + version));
    }

    /**
     * 列出数据集的所有版本
     */
    public List<String> listDatasetVersions(String name) {
        log.info("列出数据集版本: {}", name);

        List<Dataset> datasets = datasetRepository.findByName(name);
        if (datasets.isEmpty()) {
            return Collections.emptyList();
        }

        Long datasetId = datasets.get(0).getId();
        List<DatasetVersion> versions = datasetVersionRepository.findByDatasetIdOrderByCreatedAtDesc(datasetId);

        List<String> versionStrings = new ArrayList<>();
        for (DatasetVersion v : versions) {
            versionStrings.add(v.getVersion());
        }

        return versionStrings;
    }

    /**
     * 删除数据集版本
     */
    @Transactional
    public void deleteDataset(String name, String version) {
        log.info("删除数据集版本: {}, 版本: {}", name, version);

        Dataset dataset = datasetRepository.findByNameAndVersion(name, version)
                .orElseThrow(() -> new BusinessException("数据集不存在: " + name + ":" + version));

        dataset.setStatus(Dataset.DatasetStatus.BUILDING);
        datasetRepository.save(dataset);

        Optional<DatasetVersion> versionOpt = datasetVersionRepository
                .findByDatasetIdAndVersion(dataset.getId(), version);

        if (versionOpt.isPresent()) {
            DatasetVersion dv = versionOpt.get();
            dv.setStatus(DatasetVersion.VersionStatus.DELETED);
            datasetVersionRepository.save(dv);
        }
    }

    /**
     * 比较两个数据集版本
     */
    public Map<String, Object> compareDatasets(String name,
                                                String version1,
                                                String version2) {
        log.info("比较数据集版本: {}, {} vs {}", name, version1, version2);

        Dataset dataset1 = getDataset(name, version1);
        Dataset dataset2 = getDataset(name, version2);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("dataset1", dataset1);
        comparison.put("dataset2", dataset2);
        comparison.put("rowDiff",
                (dataset2.getSampleCount() != null ? dataset2.getSampleCount() : 0)
                        - (dataset1.getSampleCount() != null ? dataset1.getSampleCount() : 0));
        comparison.put("timeDiff", dataset2.getCreatedAt().compareTo(dataset1.getCreatedAt()));

        return comparison;
    }

    /**
     * 获取最新版本
     */
    public String getLatestVersion(String name) {
        log.info("获取数据集最新版本: {}", name);

        List<Dataset> datasets = datasetRepository.findByName(name);
        if (datasets.isEmpty()) {
            throw new BusinessException("数据集不存在: " + name);
        }

        Long datasetId = datasets.get(0).getId();
        Optional<DatasetVersion> latest = datasetVersionRepository
                .findTopByDatasetIdOrderByCreatedAtDesc(datasetId);

        return latest.map(DatasetVersion::getVersion)
                .orElseThrow(() -> new BusinessException("数据集没有版本记录: " + name));
    }

    /**
     * 生成版本号
     */
    private String generateVersion() {
        return "v" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 清除旧版本标签
     */
    private void clearLatestTag(Long datasetId) {
        Optional<DatasetVersion> oldLatest = datasetVersionRepository
                .findByDatasetIdAndVersionTag(datasetId, "latest");
        oldLatest.ifPresent(v -> {
            v.setVersionTag(null);
            datasetVersionRepository.save(v);
        });
    }

    /**
     * 预览数据集样本数据
     *
     * @param name     数据集名称
     * @param version  版本号
     * @param split    划分类型：train/val/test
     * @param limit    返回条数上限
     * @return 样本数据列表 + 列名
     */
    public Map<String, Object> previewDataset(String name, String version, String split, int limit) {
        log.info("预览数据集: {}, 版本: {}, 划分: {}", name, version, split);

        // 1. 查找数据集
        List<Dataset> datasets = datasetRepository.findByName(name);
        if (datasets.isEmpty()) {
            throw new BusinessException("数据集不存在: " + name);
        }

        Dataset dataset = datasets.get(0);
        DatasetVersion datasetVersion = datasetVersionRepository
                .findByDatasetIdAndVersion(dataset.getId(), version)
                .orElseThrow(() -> new BusinessException("版本不存在: " + version));

        // 2. 确定读取路径
        String path;
        switch (split.toLowerCase()) {
            case "train":
                path = datasetVersion.getTrainPath();
                break;
            case "val":
                path = datasetVersion.getValPath();
                break;
            case "test":
                path = datasetVersion.getTestPath();
                break;
            default:
                throw new BusinessException("不支持的划分类型: " + split);
        }

        if (path == null || path.isEmpty()) {
            throw new BusinessException("该划分类型无数据文件: " + split);
        }

        // 3. 解析 MinIO 路径 (格式: bucket/object/path)
        String[] parts = path.split("/", 2);
        if (parts.length != 2) {
            throw new BusinessException("无效的数据路径: " + path);
        }
        String bucket = parts[0];
        String objectName = parts[1];

        // 4. 从 MinIO 读取并解析
        List<Map<String, Object>> previewData;
        List<String> columns;
        try (InputStream is = minioService.downloadFile(bucket, objectName)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            String content = baos.toString(StandardCharsets.UTF_8.name());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allData = objectMapper.readValue(content, List.class);

            int end = Math.min(limit, allData.size());
            previewData = allData.subList(0, end);

            // 提取列名（从第一条数据推断）
            columns = !previewData.isEmpty() ? new ArrayList<>(previewData.get(0).keySet()) : Collections.emptyList();
        } catch (Exception e) {
            log.error("读取样本数据失败: {}", e.getMessage(), e);
            throw new BusinessException("读取样本数据失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("datasetName", name);
        result.put("version", version);
        result.put("split", split);
        result.put("totalCount", datasetVersion.getRowCount());
        result.put("previewCount", previewData.size());
        result.put("columns", columns);
        result.put("data", previewData);
        return result;
    }
}
