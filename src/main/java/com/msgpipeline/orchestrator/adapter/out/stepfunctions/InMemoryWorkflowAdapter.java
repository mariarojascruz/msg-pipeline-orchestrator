package com.msgpipeline.orchestrator.adapter.out.stepfunctions;

import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;
import com.msgpipeline.orchestrator.domain.port.out.WorkflowExecutionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * CLASE: InMemoryWorkflowAdapter — Adaptador de Salida (Memoria)
 * CAPA: Infraestructura — Adaptador de Salida (Output Adapter)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Simula la ejecución de Step Functions en memoria para desarrollo local.
 *
 * @Profile("local"): Solo activo en desarrollo.
 *   Permite probar el flujo sin conexión a AWS y sin Step Functions real.
 *
 * PATRÓN: Strategy
 *   Spring inyecta esta implementación en 'local' o StepFunctionsAdapter
 *   en 'aws'. StartWorkflowUseCase no sabe cuál está activa.
 *
 * VENTAJAS DEL PERFIL LOCAL:
 *   - Sin conexión a internet
 *   - Sin costos de AWS
 *   - Respuestas instantáneas (sin latencia de red)
 *   - Ideal para TDD y desarrollo rápido
 * =========================================================================
 */
@Slf4j
@Component
@Profile("local")
public class InMemoryWorkflowAdapter implements WorkflowExecutionPort {

    // Lista de ejecuciones simuladas (útil para tests y debugging)
    private final List<WorkflowExecution> executions = new ArrayList<>();

    @Override
    public WorkflowExecution startExecution(String executionName, String inputJson) {
        String fakeArn = "arn:aws:states:us-east-1:123456789012:execution:" +
                "msg-pipeline-workflow-sesion-06:" + executionName;

        log.info("[LOCAL] Simulando StartExecution en Step Functions:");
        log.info("[LOCAL]   stateMachine: msg-pipeline-workflow-sesion-06 (SIMULADO)");
        log.info("[LOCAL]   executionName: {}", executionName);
        log.info("[LOCAL]   executionArn:  {}", fakeArn);
        log.info("[LOCAL]   input ({}): {}", inputJson.length(), inputJson.substring(0, Math.min(100, inputJson.length())));
        log.info("[LOCAL] En AWS real: Step Functions ejecutaría ValidarMensaje → InvocarLambda → Processor");

        WorkflowExecution execution = WorkflowExecution.builder()
                .executionArn(fakeArn)
                .stateMachineArn("arn:aws:states:us-east-1:123456789012:stateMachine:msg-pipeline-workflow-sesion-06")
                .status("RUNNING")
                .startedAt(Instant.now().toString())
                .executionName(executionName)
                .build();

        executions.add(execution);
        return execution;
    }

    /** Para tests: retorna todas las ejecuciones simuladas */
    public List<WorkflowExecution> getExecutions() {
        return new ArrayList<>(executions);
    }

    /** Para tests: limpia las ejecuciones */
    public void clear() {
        executions.clear();
    }
}
