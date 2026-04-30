package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.MaterialCategory;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InventorySummaryResponse {
    private MaterialCategory category;
    private long itemCount;
    private long totalQuantity;
}
