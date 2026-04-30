package com.taipei.iot.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CompareReportResponse {
    private Integer periodYear;
    private Integer periodMonth;
    private List<ContractScore> contracts;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ContractScore {
        private Long contractId;
        private String contractName;
        private BigDecimal totalScore;
        private List<MonthlyReportResponse.IndicatorScore> indicators;
    }
}
