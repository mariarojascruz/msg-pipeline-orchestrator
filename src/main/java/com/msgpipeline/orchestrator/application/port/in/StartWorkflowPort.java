package com.msgpipeline.orchestrator.application.port.in;

import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;

/**
 * =========================================================================
 * INTERFAZ: StartWorkflowPort -- Puerto de Entrada
 * CAPA: Aplicacion -- Puerto de Entrada (Input Port)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Define el contrato del caso de uso principal del Orchestrator.
 *
 * IMPLEMENTACION: StartWorkflowUseCase
 * CALLERS: OrchestratorHandler (Lambda/AWS) + MessageController (local)
 * =========================================================================
 */
public interface StartWorkflowPort {

    /**
     * Inicia el workflow de procesamiento del mensaje en Step Functions.
     *
     * @param messageType    EMAIL | SMS | PUSH_NOTIFICATION
     * @param channel        EMAIL | SMS | PUSH
     * @param recipientEmail Email del destinatario
     * @param content        Contenido del mensaje
     * @param requestId      ID del request (API GW o local)
     * @param userEmail      Email del usuario autenticado (del JWT Cognito)
     * @return               WorkflowExecution con ARN y status=RUNNING
     */
    WorkflowExecution startWorkflow(
            String messageType, String channel, String recipientEmail,
            String content, String requestId, String userEmail);
}
