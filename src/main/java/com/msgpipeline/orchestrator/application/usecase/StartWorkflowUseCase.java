package com.msgpipeline.orchestrator.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgpipeline.orchestrator.application.port.in.StartWorkflowPort;
import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;
import com.msgpipeline.orchestrator.domain.port.out.WorkflowExecutionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * =========================================================================
 * CLASE: StartWorkflowUseCase -- Caso de Uso de Orquestacion
 * CAPA: Aplicacion -- Caso de Uso (Application Service)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD (SRP): Construye el input JSON e inicia Step Functions.
 * No sabe de API Gateway, JWT ni Step Functions directamente.
 *
 * FLUJO SESION 07:
 *   OrchestratorHandler --> startWorkflow()
 *   --> Generar messageId + executionName (UUID)
 *   --> Construir input JSON con userEmail del JWT Cognito
 *   --> WorkflowExecutionPort.startExecution()
 *   --> Retornar WorkflowExecution (ARN + status=RUNNING)
 *
 * PATRONES:
 *   - Use Case (Application Service)
 *   - Builder: construye el payload JSON paso a paso
 *   - DIP: depende de WorkflowExecutionPort (abstraccion)
 * =========================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartWorkflowUseCase implements StartWorkflowPort {

    // Puerto de salida: StepFunctionsAdapter (aws) o InMemoryWorkflowAdapter (local)
    private final WorkflowExecutionPort workflowExecutionPort;

    // ObjectMapper para serializar el input JSON
    // NOTA: No declarar como @Bean para evitar conflicto con JacksonAutoConfiguration
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WorkflowExecution startWorkflow(
            String messageType, String channel, String recipientEmail,
            String content, String requestId, String userEmail) {

        // -- Paso 1: Generar identificadores unicos --------------------------
        String messageId     = UUID.randomUUID().toString();
        String executionName = "exec-" + messageId;
        String submittedAt   = Instant.now().toString();

        log.info("Iniciando workflow [messageId={}] [executionName={}] [user={}]",
                messageId, executionName, userEmail);

        // -- Paso 2: Construir el input JSON para Step Functions -------------
        // Este JSON lo recibe el estado ValidarMensaje como input.
        // Incluye userEmail del JWT para trazabilidad end-to-end.
        String inputJson = buildInputJson(
                messageId, messageType, channel,
                recipientEmail, content, requestId, submittedAt, userEmail);

        // -- Paso 3: Iniciar ejecucion de Step Functions --------------------
        WorkflowExecution execution = workflowExecutionPort.startExecution(executionName, inputJson);
        execution.setRequestId(requestId);
        execution.setExecutionName(executionName);
        execution.setUserEmail(userEmail);

        log.info("Workflow iniciado [executionArn={}] [status={}]",
                execution.getExecutionArn(), execution.getStatus());

        return execution;
    }

    /**
     * Construye el payload JSON que Step Functions recibe como input.
     * Incluye todos los campos que Validator y Processor necesitan.
     */
    private String buildInputJson(
            String messageId, String messageType, String channel,
            String recipientEmail, String content,
            String requestId, String submittedAt, String userEmail) {
        try {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("messageId",      messageId);
            input.put("messageType",    messageType  != null ? messageType  : "");
            input.put("channel",        channel      != null ? channel      : "");
            input.put("recipientEmail", recipientEmail != null ? recipientEmail : "");
            input.put("content",        content      != null ? content      : "");
            input.put("requestId",      requestId    != null ? requestId    : "");
            input.put("userEmail",      userEmail    != null ? userEmail    : "");
            input.put("submittedAt",    submittedAt);
            input.put("source",         "msg-pipeline.orchestrator");
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            log.error("Error construyendo input JSON: {}", e.getMessage());
            return "{\"messageId\":\"" + messageId + "\",\"messageType\":\"" + messageType + "\"}";
        }
    }
}
