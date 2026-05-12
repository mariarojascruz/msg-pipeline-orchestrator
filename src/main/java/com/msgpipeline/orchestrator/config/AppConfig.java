package com.msgpipeline.orchestrator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracion centralizada del Orchestrator Lambda -- Sesion 07.
 * VARIABLE DE ENTORNO: STATE_MACHINE_ARN
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Aws aws = new Aws();

    @Data
    public static class Aws {
        private String region = "us-east-1";
        private String stateMachineArn = "";
    }
}
