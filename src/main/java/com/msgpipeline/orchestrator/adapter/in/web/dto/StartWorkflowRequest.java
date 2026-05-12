package com.msgpipeline.orchestrator.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de entrada para POST /messages-s7 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartWorkflowRequest {

    @NotBlank(message = "El tipo de mensaje es obligatorio")
    private String messageType;

    @NotBlank(message = "El canal es obligatorio")
    private String channel;

    private String recipientEmail;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;
}
