package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.AdjustmentType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryAdjustmentResponse {
    private Long id;
    private Long inventoryId;
    private AdjustmentType adjustmentType;
    private Integer quantityChange;
    private String reason;
    private String adjustedBy;
    private LocalDateTime adjustedAt;
}
