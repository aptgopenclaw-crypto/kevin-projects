package com.taipei.iot.workflow.dto;

import com.taipei.iot.workflow.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowInstanceResponse {

    private Long id;
    private String workflowType;
    private String ticketType;
    private Long ticketId;
    private String currentStep;
    private WorkflowStatus status;
    private String assignedTo;
    private String assignedToName;
    private String creatorId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    /** 若為代理案件，顯示「代理 XXX」 */
    private String delegatedFrom;
}
