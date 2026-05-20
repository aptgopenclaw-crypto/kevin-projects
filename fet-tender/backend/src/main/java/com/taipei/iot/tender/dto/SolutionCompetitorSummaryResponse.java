package com.taipei.iot.tender.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Solution 競品分析 — 頂部 KPI 摘要卡片 */
@Data
@Builder
public class SolutionCompetitorSummaryResponse {
    private long totalTenders;
    private BigDecimal totalAmount;
    private long vendorCount;
    private long keywordCount;
}
