package com.taipei.iot.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KpiRawDataResponse {
    private Long id;
    private Long indicatorId;
    private String indicatorCode;
    private String indicatorName;
    private Integer periodYear;
    private Integer periodMonth;
    private Long contractId;
    private BigDecimal rawValue;
    private String source;
    private LocalDateTime importedAt;
    private String importedBy;
}
