package com.taipei.iot.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KpiIndicatorResponse {
    private Long id;
    private String indicatorCode;
    private String indicatorName;
    private String category;
    private String formulaType;
    private String formula;
    private BigDecimal targetValue;
    private BigDecimal weight;
    private String dataSource;
    private String unit;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
