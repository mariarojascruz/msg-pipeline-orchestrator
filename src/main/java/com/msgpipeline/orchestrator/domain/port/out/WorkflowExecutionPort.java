package com.msgpipeline.orchestrator.domain.port.out;

import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;

/**
 * =========================================================================
 * INTERFAZ: WorkflowExecutionPort -- Puerto de Salida
 * CAPA: Dominio -- Puerto de Salida (Output Port)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Define el contrato para iniciar una ejecucion de Step Functions.
 *
 * PRINCIPIO DIP (SOLID): El dominio define QUE necesita, no COMO.
 *
 * IMPLEMENTACIONES:
 *   - StepFunctionsAdapter    --> perfil 'aws'
 *   - InMemoryWorkflowAdapter --> perfil 'local'
 * =========================================================================
 */
public interface WorkflowExecutionPort {

    /**
     * Inicia una ejecucion de la State Machine de Step Functions.
     *
     * @param executionName Nombre unico: exec-{UUID}
     * @param inputJson     Payload JSON del mensaje para Step Functions
     * @return              WorkflowExecution con ARN y status=RUNNING
     */
    WorkflowExecution startExecution(String executionName, String inputJson);
}
