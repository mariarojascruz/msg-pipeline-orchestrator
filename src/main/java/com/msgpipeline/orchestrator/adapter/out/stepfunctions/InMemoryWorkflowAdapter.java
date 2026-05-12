package com.msgpipeline.orchestrator.adapter.out.stepfunctions;

import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;
import com.msgpipeline.orchestrator.domain.port.out.WorkflowExecutionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================================
 * CLASE: InMemoryWorkflowAdapter -- Adaptador de Salida (Local)
 * CAPA: Infraestructura -- Adaptador de Salida
 * PATRON: Fake / Test Double
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Simula Step Functions en memoria para desarrollo local.
 * @Profile("local"): Solo activo en desarrollo local.
 * =========================================================================
 */
@Slf4j
@Component
@Profile("local")
public class InMemoryWorkflowAdapter implements WorkflowExecutionPort {

    private static final Map<String, String> executions = new HashMap<>();

    @Override
    public WorkflowExecution startExecution(String executionName, String inputJson) {
        String fakeArn = "arn:aws:states:us-east-1:629742034427:execution:" +
                "msg-pipeline-workflow-sesion-07:" + executionName;
        executions.put(executionName, inputJson);

        log.info("[LOCAL] Workflow simulado [name={}] [fakeArn={}]", executionName, fakeArn);
        log.info("[LOCAL] Input JSON: {}", inputJson);

        return WorkflowExecution.builder()
                .executionArn(fakeArn)
                .stateMachineArn("arn:aws:states:us-east-1:629742034427:stateMachine:" +
                        "msg-pipeline-workflow-sesion-07")
                .status("RUNNING")
                .startedAt(Instant.now().toString())
                .executionName(executionName)
                .build();
    }
}
