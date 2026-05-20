package com.taipei.iot.tender.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Solution 競品分析 — 關鍵字分布一列 */
@Data
@Builder
public class SolutionKeywordSummaryResponse {
    private String keyword;
    private long vendorCount;
    private long winCount;
    private BigDecimal totalAmount;
}
