package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MaintenanceStatsResponse {

    private long totalRepairs;
    private long completedRepairs;
    private long pendingRepairs;
    private BigDecimal completionRate;
    private BigDecimal avgRepairHours;
    private BigDecimal illuminationRate;
    private Map<String, Long> sourceDistribution;
    private Map<String, Long> faultCategoryDistribution;
}
