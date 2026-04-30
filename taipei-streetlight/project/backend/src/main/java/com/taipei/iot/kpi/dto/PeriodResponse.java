package com.taipei.iot.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PeriodResponse {
    private Long id;
    private Integer periodYear;
    private Integer periodMonth;
    private Boolean locked;
    private LocalDateTime lockedAt;
    private String lockedBy;
    private String unlockReason;
}
