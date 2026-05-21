package com.mogu.data.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * MModelX平台启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.mogu.data")
@EnableJpaRepositories(basePackages = {
    "com.mogu.data.common.repository",  // Dataset, Experiment, Model, User, Deployment等
    "com.mogu.data.feature.repository", // Feature, FeatureView等
    "com.mogu.data.sample.repository",
    "com.mogu.data.training.repository",
    "com.mogu.data.serving.repository"
})
@EntityScan(basePackages = "com.mogu.data.common.entity")
public class MModelXApplication {

    public static void main(String[] args) {
        SpringApplication.run(MModelXApplication.class, args);
    }
}