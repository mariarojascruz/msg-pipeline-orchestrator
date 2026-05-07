package com.msgpipeline.orchestrator.domain.port.out;

import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;

/**
 * =========================================================================
 * INTERFAZ: WorkflowExecutionPort — Puerto de Salida
 * CAPA: Dominio — Puerto de Salida (Output Port)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Define el contrato para iniciar una ejecución de la máquina de estados.
 *
 * PRINCIPIO DE INVERSIÓN DE DEPENDENCIAS (DIP — SOLID):
 *   El dominio define QUÉ necesita (iniciar un workflow) pero NO CÓMO.
 *   La implementación concreta (Step Functions o en memoria) vive en
 *   la capa de infraestructura (adapters/out).
 *
 * IMPLEMENTACIONES:
 *   - StepFunctionsAdapter → perfil 'aws' (Step Functions real)
 *   - InMemoryWorkflowAdapter → perfil 'local' (simulación en memoria)
 *
 * El caso de uso (StartWorkflowUseCase) solo conoce esta interfaz.
 * Puede cambiarse el motor de orquestación sin modificar el negocio.
 * =========================================================================
 */
public interface WorkflowExecutionPort {

    /**
     * Inicia una ejecución de la State Machine de Step Functions.
     *
     * En perfil 'aws':
     *   Llama a StepFunctionsClient.startExecution() con el ARN de la
     *   máquina de estados y el input JSON del mensaje.
     *
     * En perfil 'local':
     *   Simula la ejecución generando un ARN ficticio y devuelve
     *   status=RUNNING (para desarrollo sin conexión a AWS).
     *
     * @param executionName  Nombre único de la ejecución (exec-{UUID})
     * @param inputJson      Payload JSON del mensaje para Step Functions
     * @return               WorkflowExecution con ARN y estado de la ejecución
     * @throws RuntimeException si Step Functions no puede iniciar la ejecución
     */
    WorkflowExecution startExecution(String executionName, String inputJson);
}
