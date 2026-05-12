package com.msgpipeline.orchestrator.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta 202 Accepted del Orchestrator */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartWorkflowResponse {
    private String executionArn;
    private String status;
    private String message;
    private String requestId;
    private String startedAt;
    private String userEmail;
}
