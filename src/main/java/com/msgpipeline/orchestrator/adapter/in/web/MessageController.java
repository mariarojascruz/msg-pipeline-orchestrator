package com.msgpipeline.orchestrator.adapter.in.web;

import com.msgpipeline.orchestrator.adapter.in.web.dto.StartWorkflowRequest;
import com.msgpipeline.orchestrator.adapter.in.web.dto.StartWorkflowResponse;
import com.msgpipeline.orchestrator.application.port.in.StartWorkflowPort;
import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * =========================================================================
 * CLASE: MessageController — Adaptador de Entrada HTTP (Local)
 * CAPA: Infraestructura — Adaptador de Entrada
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("local"): Solo activo en desarrollo local.
 *   En Lambda, el adaptador de entrada es OrchestratorHandler.
 *
 * Permite probar el flujo con Swagger UI sin necesidad de API Gateway.
 * Endpoint: POST /messages-s6
 */
@Slf4j
@RestController
@RequestMapping("/messages-s6")
@Profile("local")
@RequiredArgsConstructor
@Tag(name = "Mensajes", description = "API de orquestacion de mensajes — Sesion 06")
public class MessageController {

    private final StartWorkflowPort startWorkflowPort;

    @Operation(
            summary = "Iniciar workflow de procesamiento",
            description = """
                    Inicia el workflow de Step Functions para procesar el mensaje.
                    
                    **Flujo:**
                    1. OrchestratorHandler recibe el POST de API Gateway
                    2. Llama StartExecution a Step Functions
                    3. Step Functions: ValidarMensaje → InvocarLambda → Processor
                    4. Processor: DynamoDB (PENDING) + SNS (email)
                    
                    **Retorna 202 Accepted** con el ARN de la ejecución de Step Functions.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Workflow iniciado exitosamente",
                    content = @Content(schema = @Schema(implementation = StartWorkflowResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "executionArn": "arn:aws:states:us-east-1:...:execution:msg-pipeline-workflow-sesion-06:exec-uuid",
                                        "status": "RUNNING",
                                        "message": "Workflow iniciado. Step Functions coordinara el procesamiento.",
                                        "requestId": "local-uuid",
                                        "startedAt": "2026-01-01T19:00:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping
    public ResponseEntity<StartWorkflowResponse> startWorkflow(
            @Valid @RequestBody StartWorkflowRequest request) {

        log.info("POST /messages-s6 [tipo={}] [canal={}] [destinatario={}]",
                request.getMessageType(), request.getChannel(), request.getRecipientEmail());

        String requestId = "local-" + UUID.randomUUID();

        WorkflowExecution execution = startWorkflowPort.startWorkflow(
                request.getMessageType(),
                request.getChannel(),
                request.getRecipientEmail(),
                request.getContent(),
                requestId
        );

        StartWorkflowResponse response = StartWorkflowResponse.builder()
                .executionArn(execution.getExecutionArn())
                .status(execution.getStatus())
                .message("Workflow iniciado. Step Functions coordinara el procesamiento.")
                .requestId(requestId)
                .startedAt(execution.getStartedAt())
                .build();

        return ResponseEntity.accepted().body(response);
    }
}
