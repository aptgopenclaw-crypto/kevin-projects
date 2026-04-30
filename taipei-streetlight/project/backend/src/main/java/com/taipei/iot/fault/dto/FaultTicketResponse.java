package com.taipei.iot.fault.dto;

import com.taipei.iot.fault.enums.FaultTicketSource;
import com.taipei.iot.fault.enums.FaultTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FaultTicketResponse {

    private Long id;
    private Long deviceId;
    private Long circuitId;
    private Long correlationId;
    private FaultTicketSource source;
    private FaultTicketStatus status;
    private String priority;
    private String description;
    private String reportedBy;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
