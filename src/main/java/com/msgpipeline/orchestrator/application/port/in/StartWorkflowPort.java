package com.msgpipeline.orchestrator.application.port.in;

import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;

/**
 * =========================================================================
 * INTERFAZ: StartWorkflowPort — Puerto de Entrada
 * CAPA: Aplicación — Puerto de Entrada (Input Port)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Define el contrato del caso de uso principal del Orchestrator.
 *
 * PATRÓN: Use Case Interface (Puerto de Entrada)
 *   - OrchestratorHandler (adaptador de entrada) lo llama
 *   - MessageController (adaptador local) también lo llama
 *   - StartWorkflowUseCase lo implementa con la lógica de negocio
 *
 * IMPLEMENTACIÓN: StartWorkflowUseCase (capa application/usecase)
 * =========================================================================
 */
public interface StartWorkflowPort {

    /**
     * Inicia el workflow de procesamiento del mensaje.
     *
     * FLUJO INTERNO:
     *   1. Genera ID único de ejecución (exec-{UUID})
     *   2. Construye el input JSON para Step Functions
     *   3. Llama a WorkflowExecutionPort.startExecution()
     *   4. Retorna WorkflowExecution con el ARN y estado
     *
     * @param messageType    Tipo de mensaje: EMAIL, SMS, PUSH_NOTIFICATION
     * @param channel        Canal: EMAIL, SMS, WHATSAPP
     * @param recipientEmail Email del destinatario
     * @param content        Contenido del mensaje
     * @param requestId      ID del request de API Gateway (trazabilidad)
     * @return               WorkflowExecution con ARN y status=RUNNING
     */
    WorkflowExecution startWorkflow(
            String messageType,
            String channel,
            String recipientEmail,
            String content,
            String requestId
    );
}
