package com.msgpipeline.orchestrator.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada Spring Boot para perfil 'local'.
 * En Lambda, el entry point es OrchestratorHandler.java.
 *
 * USO LOCAL:
 *   ./gradlew bootRun --args='--spring.profiles.active=local'
 *   Swagger UI: http://localhost:8084/swagger-ui.html
 */
@SpringBootApplication(scanBasePackages = "com.msgpipeline.orchestrator")
@EnableConfigurationProperties(AppConfig.class)
public class OrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}
