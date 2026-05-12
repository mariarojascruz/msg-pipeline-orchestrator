package com.msgpipeline.orchestrator.adapter.out.stepfunctions;

import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;
import com.msgpipeline.orchestrator.domain.port.out.WorkflowExecutionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

/**
 * =========================================================================
 * CLASE: StepFunctionsAdapter -- Adaptador de Salida (AWS Step Functions)
 * CAPA: Infraestructura -- Adaptador de Salida
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Implementa WorkflowExecutionPort usando AWS SDK v2 Step Functions.
 * @Profile("aws"): Solo activo en Lambda.
 * SfnClient: thread-safe, inicializado UNA VEZ en el cold start.
 * =========================================================================
 */
@Slf4j
@Component
@Profile("aws")
public class StepFunctionsAdapter implements WorkflowExecutionPort {

    // SfnClient thread-safe -- se inicializa una sola vez en el cold start
    private static final SfnClient sfnClient;

    static {
        sfnClient = SfnClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    @Value("${app.aws.state-machine-arn}")
    private String stateMachineArn;

    @Override
    public WorkflowExecution startExecution(String executionName, String inputJson) {
        log.info("StartExecution Step Functions [name={}] [arn={}]",
                executionName, stateMachineArn);

        StartExecutionResponse response = sfnClient.startExecution(
                StartExecutionRequest.builder()
                        .stateMachineArn(stateMachineArn)
                        .name(executionName)
                        .input(inputJson)
                        .build());

        log.info("Ejecucion iniciada [executionArn={}]", response.executionArn());

        return WorkflowExecution.builder()
                .executionArn(response.executionArn())
                .stateMachineArn(stateMachineArn)
                .status("RUNNING")
                .startedAt(response.startDate().toString())
                .executionName(executionName)
                .build();
    }
}
