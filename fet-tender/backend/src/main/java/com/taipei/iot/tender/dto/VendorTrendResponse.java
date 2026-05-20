package com.taipei.iot.tender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorTrendResponse {

    /** 時間粒度：DAY / MONTH / QUARTER */
    private String granularity;
    private List<TrendPoint> points;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        /** 期間標籤，例如 "2025-01"、"2025-Q2"、"2025-03-15" */
        private String period;
        private long count;
        private BigDecimal totalAmount;
    }
}
