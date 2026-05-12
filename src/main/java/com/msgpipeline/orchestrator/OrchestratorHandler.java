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
 * CLASE: OrchestratorHandler -- Lambda Entry Point (API Gateway Proxy)
 * CAPA: Infraestructura -- Adaptador de Entrada (Input Adapter)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD:
 *   Punto de entrada cuando API Gateway (con Cognito JWT Authorizer) invoca
 *   el Lambda. Extrae el usuario del JWT y arranca Step Functions.
 *
 * FLUJO SESION 07:
 *   Postman --> GET id_token Cognito (USER_PASSWORD_AUTH)
 *   Postman --> POST /messages-s7  Authorization: Bearer {id_token}
 *   API Gateway (Cognito JWT Authorizer valida el token)
 *   --> OrchestratorHandler::handleRequest
 *   --> StartWorkflowPort --> StepFunctionsAdapter --> StartExecution
 *   --> msg-pipeline-workflow-sesion-07
 *
 * COGNITO JWT CLAIMS:
 *   API Gateway valida el JWT con el User Pool de Cognito.
 *   Los claims llegan en requestContext.authorizer.claims.
 *   El claim 'email' contiene el email del usuario autenticado.
 *   NO necesitamos validar el JWT -- API Gateway ya lo valido.
 *
 * HANDLER: com.msgpipeline.orchestrator.OrchestratorHandler::handleRequest
 * ENV:     STATE_MACHINE_ARN
 *
 * IMPORTANTE -- WebApplicationType.SERVLET:
 *   Requerido en Spring Boot 3.5. NONE causa ClassCastException en runtime.
 * =========================================================================
 */
@Slf4j
public class OrchestratorHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // -- Bloque static -- Cold Start -----------------------------------------
    // Se ejecuta UNA SOLA VEZ cuando Lambda inicializa el container.
    // Warm starts reutilizan el contexto Spring (~50ms vs ~3-5s cold start).
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final StartWorkflowPort startWorkflowPort;

    static {
        log.info("OrchestratorHandler -- Cold Start (Sesion 07)");
        log.info("Trigger: API Gateway con Cognito JWT Authorizer --> Step Functions");

        // WebApplicationType.SERVLET es OBLIGATORIO en Spring Boot 3.5
        // NONE causa ClassCastException en runtime
        ConfigurableApplicationContext context = new SpringApplicationBuilder(OrchestratorApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("aws")
                .run();

        startWorkflowPort = context.getBean(StartWorkflowPort.class);

        log.info("Contexto Spring inicializado. StateMachine ARN: {}",
                context.getEnvironment().getProperty("app.aws.state-machine-arn"));
    }

    /** Constructor publico sin argumentos -- OBLIGATORIO para AWS Lambda. */
    public OrchestratorHandler() {
        // Lambda instancia el handler via reflexion: new OrchestratorHandler()
    }

    /**
     * handleRequest -- Invocado para cada request HTTP de API Gateway.
     *
     * FLUJO:
     *   1. Validar metodo HTTP (debe ser POST)
     *   2. Extraer email del usuario del JWT (requestContext.authorizer.claims)
     *   3. Parsear el body JSON al DTO StartWorkflowRequest
     *   4. Llamar al caso de uso StartWorkflowPort
     *   5. Retornar 202 Accepted con el ARN de la ejecucion Step Functions
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        log.info("Request recibido [httpMethod={}] [path={}] [requestId={}]",
                event.getHttpMethod(), event.getPath(), context.getAwsRequestId());

        try {
            // -- Paso 1: Validar metodo HTTP ----------------------------------
            if (!"POST".equalsIgnoreCase(event.getHttpMethod())) {
                return buildResponse(405, "{\"error\":\"Metodo no permitido\"}");
            }

            // -- Paso 2: Extraer email del JWT de Cognito --------------------
            // API Gateway con Cognito JWT Authorizer valida el token.
            // Los claims del JWT llegan en requestContext.authorizer.claims.
            String userEmail = "unknown@msgpipeline.com";
            try {
                // En aws-lambda-java-events 3.x, getAuthorizer() retorna Map<String,Object>.
                // Con Cognito JWT Authorizer los claims vienen en authorizer.get("claims").
                // Estructura: requestContext.authorizer = { "claims": { "email": "..." } }
                java.util.Map<String, Object> authorizer = event.getRequestContext().getAuthorizer();
                if (authorizer != null) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> claims =
                            (java.util.Map<String, Object>) authorizer.get("claims");
                    if (claims != null && claims.containsKey("email")) {
                        userEmail = (String) claims.get("email");
                    }
                }
                log.info("Usuario autenticado con Cognito JWT: {}", userEmail);
            } catch (Exception e) {
                log.warn("No se pudo extraer email del JWT: {}", e.getMessage());
            }

            // -- Paso 3: Validar y parsear el body ---------------------------
            String body = event.getBody();
            if (body == null || body.isBlank()) {
                return buildResponse(400, "{\"error\":\"El body no puede estar vacio\"}");
            }

            StartWorkflowRequest request = objectMapper.readValue(body, StartWorkflowRequest.class);

            log.info("Request [tipo={}] [canal={}] [dest={}]",
                    request.getMessageType(), request.getChannel(), request.getRecipientEmail());

            // -- Paso 4: Ejecutar el caso de uso ----------------------------
            WorkflowExecution execution = startWorkflowPort.startWorkflow(
                    request.getMessageType(), request.getChannel(),
                    request.getRecipientEmail(), request.getContent(),
                    context.getAwsRequestId(), userEmail);

            // -- Paso 5: Construir respuesta 202 Accepted -------------------
            StartWorkflowResponse response = StartWorkflowResponse.builder()
                    .executionArn(execution.getExecutionArn())
                    .status(execution.getStatus())
                    .message("Workflow iniciado. Step Functions coordinara el procesamiento.")
                    .requestId(context.getAwsRequestId())
                    .startedAt(execution.getStartedAt())
                    .userEmail(userEmail)
                    .build();

            log.info("Step Functions iniciado [executionArn={}]", execution.getExecutionArn());

            return buildResponse(202, objectMapper.writeValueAsString(response));

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON invalido en el body: {}", e.getMessage());
            return buildResponse(400, "{\"error\":\"JSON invalido en el body\"}");
        } catch (Exception e) {
            log.error("Error inesperado [requestId={}]: {}", context.getAwsRequestId(), e.getMessage(), e);
            return buildResponse(500, "{\"error\":\"Error interno\",\"requestId\":\"" +
                    context.getAwsRequestId() + "\"}");
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of(
                        "Content-Type",                "application/json",
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Headers","Content-Type,Authorization",
                        "Access-Control-Allow-Methods","POST,OPTIONS"
                ))
                .withBody(body);
    }
}
