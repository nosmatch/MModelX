package com.mogu.data.api.controller;

import com.mogu.data.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 平台入口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PlatformController {

    /**
     * 平台信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "MModelX");
        info.put("version", "1.0.0-SNAPSHOT");
        info.put("description", "Machine Learning Platform for 100GB Data Scale");
        info.put("author", "Mogu Data Team");
        info.put("currentTime", LocalDateTime.now());
        return Result.success(info);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        Map<String, String> components = new HashMap<>();
        components.put("feature", "UP");
        components.put("sample", "UP");
        components.put("training", "UP");
        components.put("serving", "UP");
        health.put("components", components);

        return Result.success(health);
    }

    /**
     * 模块状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("platform", "MModelX");
        status.put("version", "1.0.0-SNAPSHOT");
        status.put("uptime", System.currentTimeMillis());

        Map<String, String> modules = new HashMap<>();
        modules.put("platform-common", "ACTIVE");
        modules.put("platform-feature", "ACTIVE");
        modules.put("platform-sample", "ACTIVE");
        modules.put("platform-training", "ACTIVE");
        modules.put("platform-serving", "ACTIVE");
        modules.put("platform-api", "ACTIVE");
        status.put("modules", modules);

        return Result.success(status);
    }

    /**
     * API文档入口
     */
    @GetMapping("/docs")
    public Result<Map<String, String>> getApiDocs() {
        Map<String, String> docs = new HashMap<>();
        docs.put("swagger", "/swagger-ui.html");
        docs.put("features", "/api/features/**");
        docs.put("samples", "/api/samples/**");
        docs.put("training", "/api/training/**");
        docs.put("serving", "/api/serving/**");
        docs.put("actuator", "/actuator/**");
        return Result.success(docs);
    }

    /**
     * 配置信息
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("environment", System.getenv().getOrDefault("ENVIRONMENT", "development"));
        config.put("timezone", java.time.ZoneId.systemDefault().toString());
        config.put("javaVersion", System.getProperty("java.version"));
        config.put("springBootVersion", "3.2.0");
        return Result.success(config);
    }
}