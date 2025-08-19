package com.github.dennisoliveira.portfolio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("Project Portfolio Manager API")
                        .version("v1")
                        .description("Endpoints para gestão de projetos, alocações e relatório"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}