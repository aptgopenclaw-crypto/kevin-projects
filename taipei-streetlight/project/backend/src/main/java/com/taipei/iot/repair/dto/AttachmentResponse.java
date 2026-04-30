package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.AttachmentPhase;
import com.taipei.iot.repair.enums.ScanStatus;
import com.taipei.iot.repair.enums.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttachmentResponse {

    private Long id;
    private TicketType ticketType;
    private Long ticketId;
    private String fileType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String description;
    private BigDecimal gpsLat;
    private BigDecimal gpsLng;
    private LocalDateTime takenAt;
    private AttachmentPhase phase;
    private ScanStatus scanStatus;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
