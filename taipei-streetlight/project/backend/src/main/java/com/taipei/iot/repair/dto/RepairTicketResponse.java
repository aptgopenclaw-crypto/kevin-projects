package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RepairTicketResponse {

    private Long id;
    private String ticketNumber;
    private Long faultTicketId;
    private Long deviceId;
    private Long circuitId;
    private Long contractId;

    private RepairTicketSource source;
    private String reporterName;
    private String reporterPhone;
    private String reporterEmail;
    private String reportAddress;
    private String reportDescription;
    private LocalDateTime reportedAt;

    private String faultCategory;
    private String faultCause;
    private String repairDescription;
    private LocalDateTime completedAt;

    private RepairTicketStatus status;
    private RepairTicketPriority priority;
    private Long deptId;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 關聯資訊（在詳情頁載入）
    private String currentStep;
    private List<DispatchResponse> dispatches;
    private List<AttachmentResponse> attachments;
}
