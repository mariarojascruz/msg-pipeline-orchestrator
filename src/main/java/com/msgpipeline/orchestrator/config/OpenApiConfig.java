package com.msgpipeline.orchestrator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Swagger UI solo en perfil local -- http://localhost:8080/swagger-ui.html */
@Configuration
@Profile("local")
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("msg-pipeline-orchestrator -- Sesion 07")
                        .description("Orchestrator Lambda: Cognito JWT + Step Functions")
                        .version("1.0.0"));
    }
}
