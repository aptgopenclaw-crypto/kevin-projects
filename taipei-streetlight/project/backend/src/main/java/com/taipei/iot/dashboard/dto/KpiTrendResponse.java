package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KpiTrendResponse {

    private List<MonthlyKpi> months;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyKpi {
        private String month;
        private List<IndicatorValue> indicators;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IndicatorValue {
        private String code;
        private BigDecimal value;
    }
}
