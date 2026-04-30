package com.taipei.iot.fault.dto;

import com.taipei.iot.fault.enums.RootCauseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FaultCorrelationResponse {

    private Long id;
    private RootCauseType rootCauseType;
    private Long rootCauseId;
    private Integer affectedCount;
    private String status;
    private LocalDateTime detectedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
}
