package com.taipei.iot.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyReportResponse {
    private Integer periodYear;
    private Integer periodMonth;
    private Long contractId;
    private BigDecimal totalWeightedScore;
    private List<IndicatorScore> indicators;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IndicatorScore {
        private String indicatorCode;
        private String indicatorName;
        private BigDecimal rawValue;
        private BigDecimal resultValue;
        private BigDecimal targetValue;
        private BigDecimal achievement;
        private BigDecimal weight;
        private BigDecimal weightedScore;
    }
}
