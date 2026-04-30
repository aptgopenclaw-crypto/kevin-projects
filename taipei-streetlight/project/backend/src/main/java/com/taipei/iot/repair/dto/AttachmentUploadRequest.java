package com.taipei.iot.repair.dto;

import com.taipei.iot.repair.enums.AttachmentPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttachmentUploadRequest {

    private String fileType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String description;
    private BigDecimal gpsLat;
    private BigDecimal gpsLng;
    private LocalDateTime takenAt;
    private AttachmentPhase phase;
}
