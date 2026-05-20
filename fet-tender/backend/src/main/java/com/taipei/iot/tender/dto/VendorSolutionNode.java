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
public class VendorSolutionNode {
    private String solution;
    private long count;
    private BigDecimal totalAmount;
    private List<KeywordNode> keywords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordNode {
        private String keyword;
        private long count;
        private BigDecimal totalAmount;
    }
}
