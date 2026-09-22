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
 * CLASE: StepFunctionsAdapter — Adaptador de Salida (AWS Step Functions)
 * CAPA: Infraestructura — Adaptador de Salida (Output Adapter)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Implementa WorkflowExecutionPort usando AWS SDK v2 Step Functions.
 *
 * @Profile("aws"): Solo activo en Lambda (perfil 'aws').
 *   En 'local': InMemoryWorkflowAdapter (sin conexión a AWS).
 *
 * STEP FUNCTIONS StartExecution API:
 *   - stateMachineArn: ARN de la State Machine a ejecutar
 *   - name: nombre único de la ejecución (aparece en la consola SF)
 *   - input: JSON con los datos del mensaje para los estados
 *
 * SfnClient: thread-safe, se inicializa una sola vez (bean singleton) para warm starts.
 *   No recrear en cada invocación — es costoso en tiempo y memoria.
 *
 * CREDENCIALES AWS:
 *   Lambda usa automáticamente el IAM Role asignado a la función.
 *   No hardcodear access keys — siempre usar roles IAM.
 * =========================================================================
 */
@Slf4j
@Component
@Profile("aws")
public class StepFunctionsAdapter implements WorkflowExecutionPort {

    // SfnClient: thread-safe, se inicializa una sola vez en el cold start
    private final SfnClient sfnClient;

    @Value("${app.aws.state-machine-arn}")
    private String stateMachineArn;

    public StepFunctionsAdapter(@Value("${app.aws.region}") String region) {
        // AWS SDK toma las credenciales del IAM Role del Lambda automáticamente
        this.sfnClient = SfnClient.builder()
                .region(Region.of(region))
                .build();
    }

    /**
     * Inicia una ejecución de Step Functions con el input del mensaje.
     *
     * COMPORTAMIENTO DE StartExecution:
     *   - Es una llamada ASÍNCRONA: retorna inmediatamente con el executionArn
     *   - Step Functions ejecuta la State Machine en segundo plano
     *   - El estado inicial es siempre RUNNING al retornar
     *
     * NOMBRE DE EJECUCIÓN:
     *   Debe ser único dentro de la State Machine en los últimos 90 días.
     *   Usamos "exec-{UUID}" para garantizarlo.
     *   El nombre aparece en la consola de Step Functions para debugging.
     *
     * IDEMPOTENCIA:
     *   Si se llama StartExecution con el mismo nombre dos veces, la segunda
     *   llamada falla si la primera ejecución está en progreso o completada.
     *   Para reintentos, usar un nuevo nombre (UUID diferente).
     *
     * @param executionName Nombre único de la ejecución
     * @param inputJson     Input JSON para la State Machine
     * @return              WorkflowExecution con ARN y status=RUNNING
     */
    @Override
    public WorkflowExecution startExecution(String executionName, String inputJson) {
        log.info("Iniciando ejecución en Step Functions [name={}] [stateMachine={}]",
                executionName, stateMachineArn);

        // ── Construir la request de StartExecution ────────────────────────────
        StartExecutionRequest request = StartExecutionRequest.builder()
                .stateMachineArn(stateMachineArn)   // ARN de la State Machine
                .name(executionName)                 // Nombre único de esta ejecución
                .input(inputJson)                    // JSON que recibirán los estados
                .build();

        // ── Ejecutar StartExecution ───────────────────────────────────────────
        StartExecutionResponse response = sfnClient.startExecution(request);

        log.info("Ejecución iniciada [executionArn={}] [startDate={}]",
                response.executionArn(), response.startDate());

        // ── Construir y retornar el resultado ─────────────────────────────────
        return WorkflowExecution.builder()
                .executionArn(response.executionArn())
                .stateMachineArn(stateMachineArn)
                .status("RUNNING")                              // Estado inicial siempre RUNNING
                .startedAt(response.startDate().toString())     // Timestamp de inicio de SF
                .executionName(executionName)
                .build();
    }
}
