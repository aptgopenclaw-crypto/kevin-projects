package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.AdjustmentType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryAdjustmentRequest {

    @NotNull(message = "庫存為必填")
    private Long inventoryId;

    private Integer actualQuantity;    // for COUNT
    private Long toWarehouseId;        // for TRANSFER
    private Integer quantity;          // for TRANSFER / CORRECTION
    private String reason;
}
