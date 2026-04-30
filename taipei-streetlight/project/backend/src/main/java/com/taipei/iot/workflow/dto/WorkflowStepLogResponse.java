package com.taipei.iot.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowStepLogResponse {

    private Long id;
    private String stepCode;
    private String action;
    private String actorId;
    private String actorName;
    private String originalAssigneeId;
    private Boolean isDelegated;
    private String comment;
    private List<Map<String, Object>> attachments;
    private LocalDateTime actedAt;
}
