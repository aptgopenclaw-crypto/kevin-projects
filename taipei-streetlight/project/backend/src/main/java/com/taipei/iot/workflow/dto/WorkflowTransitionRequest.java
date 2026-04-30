package com.taipei.iot.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowTransitionRequest {

    @NotBlank(message = "目標步驟為必填")
    private String targetStep;

    @NotBlank(message = "操作為必填")
    private String action;

    private String comment;
    private List<Map<String, Object>> attachments;
}
