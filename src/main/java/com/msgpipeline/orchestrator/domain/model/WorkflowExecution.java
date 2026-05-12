package com.msgpipeline.orchestrator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CLASE: WorkflowExecution -- Entidad del Dominio
 * CAPA: Dominio -- Modelo de Negocio
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * Representa una ejecucion de la maquina de estados Step Functions.
 * Sin dependencias de frameworks ni AWS SDK.
 *
 * CICLO DE VIDA SESION 07:
 *   1. OrchestratorHandler recibe POST con JWT validado por Cognito
 *   2. Extrae email del usuario de los claims JWT
 *   3. StartWorkflowUseCase llama a StepFunctions.StartExecution
 *   4. Retorna 202 Accepted con el executionArn
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecution {

    /** ARN unico de la ejecucion en Step Functions */
    private String executionArn;

    /** ARN de la State Machine ejecutada */
    private String stateMachineArn;

    /** Estado al iniciar: siempre RUNNING */
    private String status;

    /** Timestamp ISO-8601 de inicio de la ejecucion */
    private String startedAt;

    /** ID del request de API Gateway (trazabilidad) */
    private String requestId;

    /** Nombre de la ejecucion: exec-{UUID} */
    private String executionName;

    /** Email del usuario autenticado con Cognito JWT */
    private String userEmail;
}
