package com.mogu.data.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Swagger/OpenAPI配置
 * 提供API文档访问和JWT认证支持
 *
 * @author MModelX Team
 * @since 2026-05-20
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:MModelX Platform}")
    private String applicationName;

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    /**
     * 配置OpenAPI文档
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // API基本信息
                .info(new Info()
                        .title("MModelX Machine Learning Platform API")
                        .description("End-to-end ML Platform covering feature engineering, training, and serving")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("MModelX Team")
                                .email("mmodelx@mogu.com")
                                .url("https://github.com/mogu-data/mmodelx"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )

                // 服务器配置
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.mmodelx.com")
                                .description("Production Server")
                ))

                // JWT安全配置
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入JWT token，格式: Bearer {token}")
                        )
                )

                // 全局安全要求
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
