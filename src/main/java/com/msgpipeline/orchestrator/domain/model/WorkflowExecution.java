package com.msgpipeline.orchestrator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CLASE: WorkflowExecution — Entidad del Dominio
 * CAPA: Dominio — Modelo de Negocio
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Representa una ejecución de la máquina de estados Step Functions.
 *
 * PRINCIPIOS DE CLEAN ARCHITECTURE:
 *   - Sin dependencias de frameworks (ni Spring, ni AWS SDK)
 *   - Solo datos y comportamiento de negocio
 *   - Reutilizable en cualquier contexto
 *
 * CICLO DE VIDA EN SESIÓN 06:
 *   1. OrchestratorHandler crea el payload del mensaje
 *   2. StartWorkflowUseCase llama a StepFunctions.StartExecution
 *   3. Step Functions retorna el executionArn y startDate
 *   4. Se construye WorkflowExecution con los datos de la ejecución
 *   5. Se retorna 202 Accepted al cliente con el executionArn
 *
 * ESTADOS DE LA EJECUCIÓN EN STEP FUNCTIONS:
 *   - RUNNING   → La ejecución está en progreso
 *   - SUCCEEDED → Completó exitosamente (ValidarMensaje → InvocarLambda → OK)
 *   - FAILED    → Error en algún estado (Registra Error activado)
 *   - TIMED_OUT → Superó el timeout configurado
 *   - ABORTED   → Fue cancelada manualmente
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecution {

    /**
     * ARN único de la ejecución de Step Functions.
     * Formato: arn:aws:states:us-east-1:ACCOUNT:execution:msg-pipeline-workflow-sesion-06:EXECUTION_ID
     * Permite consultar el estado en la consola de Step Functions.
     */
    private String executionArn;

    /**
     * ARN de la State Machine que ejecutó este workflow.
     * Formato: arn:aws:states:us-east-1:ACCOUNT:stateMachine:msg-pipeline-workflow-sesion-06
     */
    private String stateMachineArn;

    /**
     * Estado de la ejecución al momento de iniciarla.
     * Generalmente "RUNNING" cuando se llama a StartExecution exitosamente.
     */
    private String status;

    /**
     * Timestamp ISO-8601 de cuando inició la ejecución.
     * Asignado por Step Functions al recibir StartExecution.
     */
    private String startedAt;

    /**
     * ID del request de API Gateway que originó esta ejecución.
     * Para trazabilidad end-to-end: del request HTTP hasta DynamoDB.
     */
    private String requestId;

    /**
     * Nombre de la ejecución generado por el Orchestrator.
     * Formato: exec-{UUID}
     * Debe ser único dentro de la State Machine.
     */
    private String executionName;
}
