package com.msgpipeline.orchestrator.adapter.in.web;

import com.msgpipeline.orchestrator.adapter.in.web.dto.StartWorkflowRequest;
import com.msgpipeline.orchestrator.adapter.in.web.dto.StartWorkflowResponse;
import com.msgpipeline.orchestrator.application.port.in.StartWorkflowPort;
import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;
import io.swagger.v3.oas.annotations.Operation;
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
 * CLASE: MessageController -- Adaptador de Entrada HTTP (Local)
 * CAPA: Infraestructura -- Adaptador de Entrada
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("local"): Solo activo en desarrollo local.
 * En Lambda, el adaptador de entrada es OrchestratorHandler.
 *
 * Endpoint: POST /messages-s7
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * =========================================================================
 */
@Slf4j
@RestController
@RequestMapping("/messages-s7")
@Profile("local")
@RequiredArgsConstructor
@Tag(name = "Mensajes S7", description = "Orquestacion -- Sesion 07")
public class MessageController {

    private final StartWorkflowPort startWorkflowPort;

    @Operation(
            summary = "Iniciar workflow (Sesion 07)",
            description = "Inicia Step Functions con: Validator -> SQS -> Processor -> EventBridge -> Audit."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Workflow iniciado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping
    public ResponseEntity<StartWorkflowResponse> startWorkflow(
            @Valid @RequestBody StartWorkflowRequest request) {

        log.info("POST /messages-s7 [tipo={}] [canal={}] [dest={}]",
                request.getMessageType(), request.getChannel(), request.getRecipientEmail());

        String requestId = "local-" + UUID.randomUUID();
        String userEmail = "local-user@msgpipeline.com"; // En AWS viene del JWT Cognito

        WorkflowExecution execution = startWorkflowPort.startWorkflow(
                request.getMessageType(), request.getChannel(),
                request.getRecipientEmail(), request.getContent(),
                requestId, userEmail);

        return ResponseEntity.accepted().body(StartWorkflowResponse.builder()
                .executionArn(execution.getExecutionArn())
                .status(execution.getStatus())
                .message("Workflow iniciado. Step Functions coordinara el procesamiento.")
                .requestId(requestId)
                .startedAt(execution.getStartedAt())
                .userEmail(userEmail)
                .build());
    }
}
