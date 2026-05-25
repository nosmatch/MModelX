package com.mogu.data.serving;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Serving 独立启动类
 * 可独立部署的推理服务，不依赖 platform-training
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.mogu.data.common",
    "com.mogu.data.feature",
    "com.mogu.data.serving"
})
@EnableJpaRepositories(basePackages = {
    "com.mogu.data.common.repository",
    "com.mogu.data.feature.repository",
    "com.mogu.data.serving.repository"
})
@EntityScan(basePackages = "com.mogu.data.common.entity")
public class ServingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServingApplication.class, args);
    }
}
