package com.taipei.iot.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class YearlyReportResponse {
    private Integer periodYear;
    private Long contractId;
    private List<MonthSummary> months;
    private List<IndicatorTrend> indicators;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthSummary {
        private Integer month;
        private BigDecimal totalScore;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IndicatorTrend {
        private String indicatorCode;
        private String indicatorName;
        private List<BigDecimal> monthlyValues; // 12 elements (index 0 = Jan)
    }
}
