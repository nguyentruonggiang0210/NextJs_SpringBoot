package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cấu hình OpenAPI/Swagger cho API documentation
 * Cung cấp UI tại /swagger-ui.html và JSON spec tại /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    /**
     * Cấu hình OpenAPI với thông tin về API
     * JWT token không bắt buộc theo cấu hình hiện tại
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NextJs SpringBoot API")
                        .description("API documentation cho hệ thống NextJs SpringBoot với authentication, chat, telemetry và auto pattern processing")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Development Server")
                ));
    }
}
