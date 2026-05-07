package com.msgpipeline.orchestrator.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =========================================================================
 * CLASE: StartWorkflowRequest — DTO de Entrada
 * CAPA: Infraestructura — Adaptador de Entrada (DTO)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * DTO (Data Transfer Object) para el request HTTP POST /messages-s6.
 *
 * DIFERENCIA con el dominio:
 *   - Este DTO: lo que el cliente HTTP envía (sin IDs, sin timestamps)
 *   - WorkflowExecution: la entidad completa del dominio (con IDs, ARNs)
 *
 * VALIDACIONES Bean Validation (JSR-380):
 *   @NotBlank: campo obligatorio, no puede ser null, vacío o solo espacios
 *   @Email: debe ser un email válido (formato RFC 5322)
 *   @Pattern: debe coincidir con el regex dado
 *
 * EJEMPLO DE REQUEST:
 * {
 *   "messageType": "EMAIL",
 *   "channel": "EMAIL",
 *   "recipientEmail": "estudiante@anku.com",
 *   "content": "Mensaje de prueba Sesion 06"
 * }
 * =========================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos del mensaje para iniciar el workflow de procesamiento")
public class StartWorkflowRequest {

    @NotBlank(message = "El tipo de mensaje es obligatorio")
    @Pattern(regexp = "EMAIL|SMS|PUSH_NOTIFICATION",
            message = "messageType debe ser: EMAIL, SMS o PUSH_NOTIFICATION")
    @Schema(description = "Tipo del mensaje a procesar",
            example = "EMAIL",
            allowableValues = {"EMAIL", "SMS", "PUSH_NOTIFICATION"})
    private String messageType;

    @NotBlank(message = "El canal es obligatorio")
    @Pattern(regexp = "EMAIL|SMS|WHATSAPP",
            message = "channel debe ser: EMAIL, SMS o WHATSAPP")
    @Schema(description = "Canal de entrega del mensaje",
            example = "EMAIL",
            allowableValues = {"EMAIL", "SMS", "WHATSAPP"})
    private String channel;

    @NotBlank(message = "El email del destinatario es obligatorio")
    @Email(message = "recipientEmail debe ser un email valido")
    @Schema(description = "Email del destinatario del mensaje",
            example = "estudiante@anku.com")
    private String recipientEmail;

    @NotBlank(message = "El contenido del mensaje es obligatorio")
    @Schema(description = "Contenido del mensaje",
            example = "Hola! Este es un mensaje de prueba de la Sesion 06 — Step Functions")
    private String content;
}
