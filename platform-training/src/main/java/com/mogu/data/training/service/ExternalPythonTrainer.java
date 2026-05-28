package com.mogu.data.training.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogu.data.common.entity.Dataset;
import com.mogu.data.common.entity.DatasetVersion;
import com.mogu.data.common.exception.BusinessException;
import com.mogu.data.common.logger.Logger;
import com.mogu.data.common.repository.DatasetRepository;
import com.mogu.data.common.repository.DatasetVersionRepository;
import com.mogu.data.common.storage.MinioService;
import com.mogu.data.training.entity.TrainingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 外部Python训练器基类
 * 封装Java调用Python脚本执行真实模型训练的通用逻辑
 */
@RequiredArgsConstructor
public abstract class ExternalPythonTrainer {

    private static final Logger log = Logger.getLogger(ExternalPythonTrainer.class);
    private static final String MODELS_BUCKET = "models";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    protected final MinioService minioService;
    protected final DatasetRepository datasetRepository;
    protected final DatasetVersionRepository datasetVersionRepository;
    protected final ObjectMapper objectMapper;

    // ThreadLocal 缓存每次训练的真实指标，供 validate() 使用
    private final ThreadLocal<Map<String, Object>> lastMetrics = new ThreadLocal<>();

    /**
     * 获取上次训练缓存的真实指标
     */
    protected Map<String, Object> getLastMetrics() {
        Map<String, Object> cached = lastMetrics.get();
        if (cached != null) {
            return new HashMap<>(cached);
        }
        return null;
    }

    protected String trainInternal(TrainingConfig config, String modelType, String modelExtension) {
        log.info("开始{}训练: {}, dataset={}", modelType, config.getExperimentName(), config.getDatasetName());

        // 1. 查找数据集版本
        DatasetVersion datasetVersion = resolveDatasetVersion(config);
        String trainPath = datasetVersion.getTrainPath();
        String valPath = datasetVersion.getValPath();
        String testPath = datasetVersion.getTestPath();

        if (trainPath == null || valPath == null || testPath == null) {
            throw new BusinessException("数据集划分路径不完整");
        }

        // 2. 创建临时目录
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("mmodelx_train_");
        } catch (IOException e) {
            throw new BusinessException("创建临时目录失败: " + e.getMessage());
        }

        try {
            // 3. 从MinIO下载样本数据
            Path localTrain = downloadFromMinio(trainPath, tempDir, "train.json");
            Path localVal = downloadFromMinio(valPath, tempDir, "val.json");
            Path localTest = downloadFromMinio(testPath, tempDir, "test.json");

            log.info("样本数据已下载到临时目录: {}", tempDir);

            // 4. 准备Python脚本和输出路径
            Path scriptPath = extractPythonScript(tempDir);
            Path modelOutput = tempDir.resolve("model." + modelExtension);
            Path metricsOutput = tempDir.resolve("metrics.json");

            // 5. 构建命令
            List<String> cmd = new ArrayList<>();
            cmd.add("python3");
            cmd.add(scriptPath.toString());
            cmd.add("--train");
            cmd.add(localTrain.toString());
            cmd.add("--val");
            cmd.add(localVal.toString());
            cmd.add("--test");
            cmd.add(localTest.toString());
            cmd.add("--model-type");
            cmd.add(modelType);
            cmd.add("--label-col");
            cmd.add(resolveLabelColumn(config));
            cmd.add("--params");
            cmd.add(buildParamsJson(config));
            cmd.add("--output");
            cmd.add(modelOutput.toString());
            cmd.add("--metrics-output");
            cmd.add(metricsOutput.toString());

            // 6. 执行Python脚本
            executePython(cmd, tempDir);

            // 7. 读取指标并缓存到 ThreadLocal，供 validate() 返回真实值
            Map<String, Object> metrics = readMetrics(metricsOutput);
            lastMetrics.set(new HashMap<>(metrics));
            log.info("训练指标: {}", metrics);

            // 8. 上传模型到MinIO
            String modelKey = uploadModelToMinio(modelOutput, config.getExperimentName(), modelType, modelExtension);

            log.info("{}训练完成: {}, 模型路径: {}", modelType, config.getExperimentName(), modelKey);
            return modelKey;

        } finally {
            cleanupTempDir(tempDir);
        }
    }

    // ========== 私有辅助方法 ==========

    private DatasetVersion resolveDatasetVersion(TrainingConfig config) {
        String datasetName = config.getDatasetName();
        String version = config.getDatasetVersion();

        if (datasetName == null || datasetName.isEmpty()) {
            throw new BusinessException("数据集名称不能为空");
        }

        List<Dataset> datasets = datasetRepository.findByName(datasetName);
        if (datasets.isEmpty()) {
            throw new BusinessException("数据集不存在: " + datasetName);
        }

        Long datasetId = datasets.get(0).getId();
        Optional<DatasetVersion> dvOpt = datasetVersionRepository.findByDatasetIdAndVersion(datasetId, version);
        if (!dvOpt.isPresent() && "latest".equalsIgnoreCase(version)) {
            dvOpt = datasetVersionRepository.findTopByDatasetIdOrderByCreatedAtDesc(datasetId);
        }

        return dvOpt.orElseThrow(() ->
                new BusinessException("数据集版本不存在: " + datasetName + "@" + version));
    }

    private Path downloadFromMinio(String path, Path tempDir, String fileName) {
        try {
            String[] parts = path.split("/", 2);
            if (parts.length != 2) {
                throw new BusinessException("无效的MinIO路径: " + path);
            }
            String bucket = parts[0];
            String objectName = parts[1];

            InputStream is = minioService.downloadFile(bucket, objectName);
            Path localPath = tempDir.resolve(fileName);
            Files.copy(is, localPath, StandardCopyOption.REPLACE_EXISTING);
            is.close();
            return localPath;
        } catch (Exception e) {
            throw new BusinessException("下载样本数据失败: " + path + ", " + e.getMessage());
        }
    }

    private Path extractPythonScript(Path tempDir) {
        try {
            ClassPathResource resource = new ClassPathResource("scripts/train_model.py");
            Path scriptPath = tempDir.resolve("train_model.py");
            try (InputStream is = resource.getInputStream()) {
                Files.copy(is, scriptPath, StandardCopyOption.REPLACE_EXISTING);
            }
            scriptPath.toFile().setExecutable(true);
            return scriptPath;
        } catch (IOException e) {
            throw new BusinessException("提取Python脚本失败: " + e.getMessage());
        }
    }

    private String buildParamsJson(TrainingConfig config) {
        Map<String, Object> params = new HashMap<>();
        if (config.getModel() != null && config.getModel().getParams() != null) {
            params.putAll(config.getModel().getParams());
        }
        if (config.getTrainingParams() != null) {
            params.put("num_rounds", config.getTrainingParams().getNumRounds());
            params.put("early_stopping_rounds", config.getTrainingParams().getEarlyStoppingRounds());
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String resolveLabelColumn(TrainingConfig config) {
        String datasetName = config.getDatasetName();
        if (datasetName != null && !datasetName.isEmpty()) {
            List<Dataset> datasets = datasetRepository.findByName(datasetName);
            if (!datasets.isEmpty() && datasets.get(0).getLabelColumn() != null) {
                return datasets.get(0).getLabelColumn();
            }
        }
        return "is_churned";
    }

    private void executePython(List<String> cmd, Path workingDir) {
        log.info("执行Python命令: {}", String.join(" ", cmd));
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python] {}", line);
                }
            }

            boolean finished = process.waitFor(600, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("Python训练脚本执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new BusinessException("Python训练脚本执行失败，退出码: " + exitCode);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("500", "执行Python训练脚本失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> readMetrics(Path metricsPath) {
        try {
            String content = new String(Files.readAllBytes(metricsPath), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = objectMapper.readValue(content, Map.class);
            return metrics;
        } catch (IOException e) {
            throw new BusinessException("读取训练指标失败: " + e.getMessage());
        }
    }

    private String uploadModelToMinio(Path localModelPath, String experimentName, String modelType, String ext) {
        try {
            String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
            String objectName = String.format("%s/%s/%s/model.%s", modelType, experimentName, dateStr, ext);

            try (InputStream is = Files.newInputStream(localModelPath)) {
                long size = Files.size(localModelPath);
                minioService.uploadFile(MODELS_BUCKET, objectName, is, size, "application/octet-stream");
            }

            return MODELS_BUCKET + "/" + objectName;
        } catch (Exception e) {
            throw new BusinessException("上传模型到MinIO失败: " + e.getMessage());
        }
    }

    private void cleanupTempDir(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", tempDir);
        }
    }
}
