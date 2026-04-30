package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.IssueRequestStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IssueRequestResponse {
    private Long id;
    private String requestNumber;
    private Long repairTicketId;
    private Long replacementOrderId;
    private String requestedBy;
    private IssueRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
