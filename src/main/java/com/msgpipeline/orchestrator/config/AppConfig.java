package com.msgpipeline.orchestrator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración centralizada del Orchestrator Lambda.
 *
 * VARIABLES DE ENTORNO EN LAMBDA:
 *   STATE_MACHINE_ARN = arn:aws:states:us-east-1:ACCOUNT_ID:stateMachine:msg-pipeline-workflow-sesion-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Aws aws = new Aws();

    @Data
    public static class Aws {
        private String region = "us-east-1";
        /** ARN de la State Machine de Step Functions */
        private String stateMachineArn = "";
    }
}
