package com.msgpipeline.orchestrator.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CLASE: StartWorkflowResponse — DTO de Salida
 * CAPA: Infraestructura — Adaptador de Entrada (DTO)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * DTO de respuesta para el endpoint POST /messages-s6.
 *
 * CÓDIGO HTTP: 202 Accepted
 *   El procesamiento es ASÍNCRONO: Step Functions lo coordina en segundo
 *   plano. El cliente recibe el executionArn para consultar el estado.
 *
 * EJEMPLO DE RESPONSE:
 * {
 *   "executionArn": "arn:aws:states:us-east-1:...:execution:msg-pipeline-workflow-sesion-06:exec-uuid",
 *   "status": "RUNNING",
 *   "message": "Workflow iniciado. Step Functions coordinara el procesamiento.",
 *   "requestId": "api-gw-request-id",
 *   "startedAt": "2026-01-01T19:00:00Z"
 * }
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del inicio del workflow de Step Functions")
public class StartWorkflowResponse {

    @Schema(description = "ARN único de la ejecución de Step Functions. " +
            "Usar para consultar el estado en la consola de AWS.",
            example = "arn:aws:states:us-east-1:123456789012:execution:msg-pipeline-workflow-sesion-06:exec-uuid")
    private String executionArn;

    @Schema(description = "Estado de la ejecución al momento de iniciarla",
            example = "RUNNING",
            allowableValues = {"RUNNING", "FAILED"})
    private String status;

    @Schema(description = "Mensaje informativo del resultado",
            example = "Workflow iniciado. Step Functions coordinara el procesamiento.")
    private String message;

    @Schema(description = "ID del request de API Gateway para trazabilidad",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String requestId;

    @Schema(description = "Timestamp ISO-8601 de cuando inició la ejecución",
            example = "2026-01-01T19:00:00Z")
    private String startedAt;
}
