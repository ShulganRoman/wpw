package com.wpw.pim.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("WPW PIM API")
                .description("Product Information Management for WPW Professional Cutting Tools. " +
                    "Authentication: use POST /api/v1/auth/login to obtain a JWT token, " +
                    "then pass it as `Authorization: Bearer <token>` header.")
                .version("v1"))
            .addServersItem(new Server().url("/").description("Current server"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .addSecurityItem(new SecurityRequirement().addList("X-API-Key"))
            .components(new Components()
                .addSecuritySchemes("Bearer", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token. Obtain via: POST /api/v1/auth/login"))
                .addSecuritySchemes("X-API-Key", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API key for service integrations")));
    }
}
