package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OutageTrendResponse {

    private List<MonthlyOutage> months;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyOutage {
        private String month;
        private int outageCount;
        private BigDecimal avgRecoveryHours;
    }
}
