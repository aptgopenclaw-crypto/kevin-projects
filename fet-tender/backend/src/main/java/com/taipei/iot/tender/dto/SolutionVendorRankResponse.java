package com.taipei.iot.tender.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Solution 競品分析 — 廠商排行榜一列 */
@Data
@Builder
public class SolutionVendorRankResponse {
    private int rank;
    private String vendorName;
    private String vendorTaxId;
    private long winCount;
    private BigDecimal totalAmount;
}
