package com.msgpipeline.orchestrator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Configuración de OpenAPI / Swagger UI para desarrollo local.
 *
 * Solo activo en perfil 'local'.
 * En Lambda no hay servidor HTTP — API Gateway gestiona los endpoints.
 *
 * ACCESO LOCAL:
 *   http://localhost:8084/swagger-ui.html → UI interactiva
 *   http://localhost:8084/v3/api-docs     → JSON de la spec OpenAPI
 */
@Configuration
@Profile("local")
public class OpenApiConfig {

    @Value("${server.port:8084}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("msg-pipeline-orchestrator-sesion-06 API")
                        .version("1.0.0")
                        .description("""
                                ## API del Orchestrator Lambda — Sesion 06
                                
                                **Arquitectura:** Hexagonal + Clean Architecture
                                
                                **Flujo:**
                                ```
                                Postman → POST /messages-s6 → API Gateway
                                  → Lambda Orchestrator
                                  → StartExecution Step Functions (msg-pipeline-workflow-sesion-06)
                                  → ValidarMensaje → InvocarLambda → Processor Lambda
                                  → DynamoDB (PENDING) + SNS (email)
                                ```
                                
                                **Nuevo en Sesion 06:**
                                - Step Functions orquesta el flujo completo
                                - Validacion, reintentos y errores gestionados por la State Machine
                                - CloudWatch Dashboard y Alertas para monitoreo
                                """)
                        .contact(new Contact()
                                .name("Anku Academy")
                                .email("anku@academy.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor local de desarrollo"),
                        new Server()
                                .url("https://REPLACE.execute-api.us-east-1.amazonaws.com/prod")
                                .description("AWS API Gateway (produccion)")
                ));
    }
}
