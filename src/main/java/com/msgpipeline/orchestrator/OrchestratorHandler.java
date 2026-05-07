package com.msgpipeline.orchestrator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgpipeline.orchestrator.adapter.in.web.dto.StartWorkflowRequest;
import com.msgpipeline.orchestrator.adapter.in.web.dto.StartWorkflowResponse;
import com.msgpipeline.orchestrator.application.port.in.StartWorkflowPort;
import com.msgpipeline.orchestrator.config.OrchestratorApplication;
import com.msgpipeline.orchestrator.domain.model.WorkflowExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * =========================================================================
 * CLASE: OrchestratorHandler — Lambda Entry Point (API Gateway Proxy)
 * CAPA: Infraestructura — Adaptador de Entrada (Input Adapter)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD:
 *   Es el punto de entrada del Lambda cuando API Gateway invoca la función.
 *   Convierte el evento HTTP (APIGatewayProxyRequestEvent) en una llamada
 *   al caso de uso de negocio (StartWorkflowPort), y la respuesta del caso
 *   de uso en una respuesta HTTP (APIGatewayProxyResponseEvent).
 *
 * FLUJO COMPLETO SESIÓN 06:
 *   ┌─────────┐   POST /messages-s6   ┌────────────────┐
 *   │ Postman │ ─────────────────────▶ │  API Gateway   │
 *   └─────────┘                        │ msg-pipeline-api│
 *                                      └───────┬────────┘
 *                                              │ Lambda Proxy
 *                                              ▼
 *                              ┌─────────────────────────────────┐
 *                              │ msg-pipeline-orchestrator-s06   │
 *                              │ OrchestratorHandler             │ ← (esta clase)
 *                              └─────────────┬───────────────────┘
 *                                            │ StartExecution
 *                                            ▼
 *                              ┌─────────────────────────────────┐
 *                              │ Step Functions                  │
 *                              │ msg-pipeline-workflow-sesion-06 │
 *                              └─────────────────────────────────┘
 *
 * DIFERENCIA CON SESIÓN 05:
 *   Sesión 05: Orchestrator llamaba directamente a DynamoDB + EventBridge
 *   Sesión 06: Orchestrator delega TODO a Step Functions.
 *              Step Functions coordina la validación y el procesamiento.
 *              Ventaja: flujo visible en la consola de Step Functions con
 *              reintentos automáticos y manejo de errores centralizado.
 *
 * HANDLER A CONFIGURAR EN LAMBDA CONSOLE:
 *   com.msgpipeline.orchestrator.OrchestratorHandler::handleRequest
 *
 * VARIABLES DE ENTORNO REQUERIDAS:
 *   STATE_MACHINE_ARN = arn:aws:states:us-east-1:ACCOUNT:stateMachine:msg-pipeline-workflow-sesion-06
 *
 * COLD START vs WARM START:
 *   - Cold Start: bloque static → inicializa Spring una sola vez (~3-5 seg)
 *   - Warm Start: el contexto Spring se reutiliza (~50-100 ms)
 *   - SnapStart (Sesión 08) puede eliminar el cold start completamente
 * =========================================================================
 */
@Slf4j
public class OrchestratorHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // ── Bloque static — Inicialización en el Cold Start ──────────────────────
    //
    // IMPORTANTE: En Lambda, el bloque static se ejecuta UNA SOLA VEZ cuando
    // el container se inicializa. En warm starts se reutiliza el mismo
    // contexto Spring. Esto mejora dramáticamente la latencia en producción.
    //
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final StartWorkflowPort startWorkflowPort;

    static {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  OrchestratorHandler — Cold Start (Sesión 06)               ║");
        log.info("║  Trigger: API Gateway → StartExecution Step Functions       ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        // Inicializamos el contexto Spring con perfil 'aws'
        // WebApplicationType.SERVLET: requerido en Spring Boot 3.5 para que
        // los filtros y la cadena de beans de Spring funcionen correctamente.
        // Sin esto aparece ClassCastException en runtime.
        ConfigurableApplicationContext context = new SpringApplicationBuilder(OrchestratorApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("aws")
                .run();

        // Obtenemos el puerto de entrada del caso de uso desde el contexto Spring
        startWorkflowPort = context.getBean(StartWorkflowPort.class);

        log.info("Contexto Spring inicializado correctamente.");
        log.info("StateMachine ARN: {}",
                context.getEnvironment().getProperty("app.aws.state-machine-arn"));
    }

    /**
     * Constructor público sin argumentos — OBLIGATORIO para AWS Lambda.
     * Lambda instancia el handler via reflexión: new OrchestratorHandler()
     * Si se declara solo un constructor con parámetros, Lambda falla con
     * "No public zero-argument constructor found".
     */
    public OrchestratorHandler() {
        // Requerido por el runtime de Lambda
    }

    /**
     * handleRequest — Método invocado por Lambda para cada request HTTP.
     *
     * API Gateway en modo Proxy envía el request completo al Lambda.
     * Este método:
     *   1. Valida que el método HTTP sea POST
     *   2. Parsea el body JSON al DTO de entrada
     *   3. Convierte el DTO a la entidad del dominio
     *   4. Llama al caso de uso (StartWorkflowPort)
     *   5. Retorna la respuesta HTTP con 202 Accepted
     *
     * @param event   Request HTTP recibido de API Gateway (modo Proxy)
     * @param context Contexto Lambda (requestId, timeout restante, logs)
     * @return        Respuesta HTTP completa para API Gateway
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {

        log.info("Request recibido [httpMethod={}] [path={}] [requestId={}]",
                event.getHttpMethod(),
                event.getPath(),
                context.getAwsRequestId());

        try {
            // ── Paso 1: Validar método HTTP ───────────────────────────────────
            if (!"POST".equalsIgnoreCase(event.getHttpMethod())) {
                log.warn("Método HTTP no permitido: {}", event.getHttpMethod());
                return buildResponse(405,
                        "{\"error\":\"Metodo no permitido\",\"metodo\":\"" + event.getHttpMethod() + "\"}");
            }

            // ── Paso 2: Parsear body del request ──────────────────────────────
            //
            // API Gateway en modo Proxy envía el body como String JSON.
            // Jackson lo deserializa automáticamente a nuestro DTO de entrada.
            //
            String body = event.getBody();
            if (body == null || body.isBlank()) {
                log.warn("Body del request está vacío o es null");
                return buildResponse(400,
                        "{\"error\":\"El body del request no puede estar vacio\"}");
            }

            StartWorkflowRequest request = objectMapper.readValue(body, StartWorkflowRequest.class);

            log.info("Request deserializado [tipo={}] [canal={}] [destinatario={}]",
                    request.getMessageType(),
                    request.getChannel(),
                    request.getRecipientEmail());

            // ── Paso 3: Ejecutar el caso de uso ──────────────────────────────
            //
            // El caso de uso (StartWorkflowUseCase) toma los datos del request,
            // construye el input JSON para Step Functions y llama StartExecution.
            // Retorna un WorkflowExecution con el ARN de ejecución y el estado.
            //
            WorkflowExecution execution = startWorkflowPort.startWorkflow(
                    request.getMessageType(),
                    request.getChannel(),
                    request.getRecipientEmail(),
                    request.getContent(),
                    context.getAwsRequestId()
            );

            log.info("Step Functions iniciado [executionArn={}] [status={}]",
                    execution.getExecutionArn(), execution.getStatus());

            // ── Paso 4: Construir respuesta HTTP ──────────────────────────────
            //
            // 202 Accepted: el mensaje fue aceptado pero el procesamiento
            // es ASÍNCRONO — Step Functions lo coordina en segundo plano.
            //
            StartWorkflowResponse response = StartWorkflowResponse.builder()
                    .executionArn(execution.getExecutionArn())
                    .status(execution.getStatus())
                    .message("Workflow iniciado. Step Functions coordinara el procesamiento.")
                    .requestId(context.getAwsRequestId())
                    .startedAt(execution.getStartedAt())
                    .build();

            return buildResponse(202, objectMapper.writeValueAsString(response));

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Error al parsear el body JSON: {}", e.getMessage());
            return buildResponse(400,
                    "{\"error\":\"JSON invalido en el body\",\"detalle\":\"" + e.getMessage() + "\"}");

        } catch (Exception e) {
            log.error("Error inesperado [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage(), e);
            return buildResponse(500,
                    "{\"error\":\"Error interno del servidor\",\"requestId\":\"" +
                            context.getAwsRequestId() + "\"}");
        }
    }

    /**
     * buildResponse — Construye la respuesta HTTP para API Gateway (Proxy mode).
     *
     * API Gateway en modo Proxy espera un objeto con:
     *   - statusCode: código HTTP (200, 202, 400, 500, etc.)
     *   - headers: cabeceras de respuesta
     *   - body: cuerpo JSON como String
     *
     * CORS headers incluidos para permitir llamadas desde navegadores.
     *
     * @param statusCode Código HTTP de la respuesta
     * @param body       JSON de la respuesta como String
     * @return           Objeto APIGatewayProxyResponseEvent para API Gateway
     */
    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Headers", "Content-Type,Authorization",
                        "Access-Control-Allow-Methods", "POST,OPTIONS"
                ))
                .withBody(body);
    }
}
