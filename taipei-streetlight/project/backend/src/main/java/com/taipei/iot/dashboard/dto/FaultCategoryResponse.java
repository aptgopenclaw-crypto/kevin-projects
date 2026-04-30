package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FaultCategoryResponse {

    private List<CategoryItem> categories;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryItem {
        private String category;
        private long count;
        private BigDecimal percentage;
    }
}
