package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.RepairDispatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DispatchResponse {

    private Long id;
    private Long repairTicketId;
    private Long contractId;
    private Long assignedTo;
    private String assignedOrg;
    private String dispatchNote;
    private LocalDateTime dispatchedAt;
    private Long dispatchedBy;
    private LocalDate dueDate;
    private RepairDispatchStatus status;
    private LocalDateTime createdAt;
}
