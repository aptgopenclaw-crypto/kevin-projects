package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KpiSummaryResponse {

    private List<KpiIndicatorSummary> indicators;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class KpiIndicatorSummary {
        private String code;
        private String name;
        private BigDecimal value;
        private BigDecimal target;
        private BigDecimal achievement;
        private String grade;   // A / B / C / D / F
    }
}
