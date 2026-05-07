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
import java.util.Map;
import java.util.UUID;

/**
 * =========================================================================
 * CLASE: StartWorkflowUseCase — Caso de Uso de Orquestación
 * CAPA: Aplicación — Caso de Uso
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD ÚNICA (SRP — SOLID):
 *   Construye el input JSON para Step Functions e inicia la ejecución.
 *   No sabe de API Gateway, no sabe de Step Functions directamente.
 *   Solo conoce el puerto de salida (WorkflowExecutionPort).
 *
 * FLUJO DETALLADO (Sesión 06):
 *   OrchestratorHandler → startWorkflow()
 *   → Paso 1: Generar nombre único de ejecución
 *   → Paso 2: Construir el input JSON para Step Functions
 *   → Paso 3: Llamar a WorkflowExecutionPort.startExecution()
 *   → Retornar WorkflowExecution con ARN y status
 *
 * INPUT JSON QUE RECIBE STEP FUNCTIONS:
 *   Step Functions pasa este JSON a cada estado como "input":
 *   {
 *     "messageId": "uuid...",
 *     "messageType": "EMAIL",
 *     "channel": "EMAIL",
 *     "recipientEmail": "test@test.com",
 *     "content": "...",
 *     "requestId": "api-gw-request-id",
 *     "submittedAt": "2026-01-01T...",
 *     "source": "msg-pipeline.orchestrator"
 *   }
 *
 * PATRONES APLICADOS:
 *   - Use Case (Application Service): orquesta la lógica de negocio
 *   - Builder: construye el payload JSON paso a paso
 *   - Dependency Injection: @RequiredArgsConstructor + @Service
 * =========================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartWorkflowUseCase implements StartWorkflowPort {

    // Puerto de salida — Step Functions
    // 'local' → InMemoryWorkflowAdapter
    // 'aws'   → StepFunctionsAdapter
    private final WorkflowExecutionPort workflowExecutionPort;

    // ObjectMapper para serializar el input JSON del workflow
    // NOTA: No se declara como @Bean para evitar conflicto con JacksonAutoConfiguration
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Caso de uso: Iniciar el workflow de procesamiento del mensaje.
     *
     * NOMBRE DE EJECUCIÓN:
     *   Step Functions requiere que el nombre de ejecución sea único
     *   dentro de la State Machine. Usamos "exec-{UUID}" para garantizarlo.
     *   El nombre aparece en la consola de Step Functions como identificador.
     *
     * INPUT JSON PARA STEP FUNCTIONS:
     *   La State Machine recibe el input JSON y lo pasa a cada estado.
     *   El estado ValidarMensaje lo lee para validar los campos.
     *   El estado InvocarLambda lo pasa al Processor Lambda.
     *
     * @param messageType    Tipo de mensaje
     * @param channel        Canal de entrega
     * @param recipientEmail Email del destinatario
     * @param content        Contenido del mensaje
     * @param requestId      ID del request API Gateway (para trazabilidad)
     * @return               WorkflowExecution con ARN y status=RUNNING
     */
    @Override
    public WorkflowExecution startWorkflow(
            String messageType,
            String channel,
            String recipientEmail,
            String content,
            String requestId) {

        // ── Paso 1: Generar identificadores únicos ────────────────────────────
        String messageId     = UUID.randomUUID().toString();
        String executionName = "exec-" + messageId;
        String submittedAt   = Instant.now().toString();

        log.info("Iniciando workflow [messageId={}] [executionName={}] [requestId={}]",
                messageId, executionName, requestId);

        // ── Paso 2: Construir el input JSON para Step Functions ───────────────
        //
        // Este JSON es el "input" que Step Functions inyecta al primer estado.
        // Cada estado de la máquina puede leer y modificar este JSON.
        // JsonPath ($.) permite que los estados referencien campos específicos.
        //
        String inputJson = buildInputJson(
                messageId, messageType, channel,
                recipientEmail, content, requestId, submittedAt
        );

        log.info("Input JSON construido para Step Functions [messageId={}] [size={}]",
                messageId, inputJson.length());

        // ── Paso 3: Iniciar ejecución de Step Functions ───────────────────────
        //
        // WorkflowExecutionPort.startExecution() llama a la API de AWS
        // Step Functions para iniciar la ejecución de la State Machine.
        // En perfil 'local' usa una simulación en memoria.
        //
        WorkflowExecution execution = workflowExecutionPort.startExecution(executionName, inputJson);
        execution.setRequestId(requestId);
        execution.setExecutionName(executionName);

        log.info("Workflow iniciado exitosamente [executionArn={}] [status={}]",
                execution.getExecutionArn(), execution.getStatus());

        return execution;
    }

    // ── Método auxiliar — Construye el input JSON ─────────────────────────────

    /**
     * Construye el payload JSON que Step Functions recibe como input.
     *
     * Todos los campos del mensaje se incluyen para que los estados de
     * la State Machine puedan acceder a ellos sin llamadas adicionales.
     *
     * NOTA: El Processor Lambda recibe EXACTAMENTE este JSON como input,
     * por eso todos los campos que Processor necesita deben estar aquí.
     */
    private String buildInputJson(
            String messageId, String messageType, String channel,
            String recipientEmail, String content,
            String requestId, String submittedAt) {
        try {
            Map<String, Object> input = Map.of(
                    "messageId",     messageId,
                    "messageType",   messageType != null ? messageType : "",
                    "channel",       channel != null ? channel : "",
                    "recipientEmail",recipientEmail != null ? recipientEmail : "",
                    "content",       content != null ? content : "",
                    "requestId",     requestId != null ? requestId : "",
                    "submittedAt",   submittedAt,
                    "source",        "msg-pipeline.orchestrator"
            );
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            log.error("Error construyendo input JSON para Step Functions: {}", e.getMessage());
            // Fallback con JSON mínimo
            return String.format(
                    "{\"messageId\":\"%s\",\"messageType\":\"%s\",\"channel\":\"%s\"," +
                    "\"recipientEmail\":\"%s\",\"source\":\"msg-pipeline.orchestrator\"}",
                    messageId, messageType, channel, recipientEmail
            );
        }
    }
}
