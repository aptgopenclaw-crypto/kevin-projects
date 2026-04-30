package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MaintenanceTrendResponse {

    private List<MonthlyPoint> months;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MonthlyPoint {
        private String month;       // "2026-01"
        private long repairCount;
        private BigDecimal completionRate;
    }
}
