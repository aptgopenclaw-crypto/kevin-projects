package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.RepairTicketStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PublicRepairStatusResponse {

    private String ticketNumber;
    private RepairTicketStatus status;
    private String statusLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
