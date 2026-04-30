package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.MaterialCategory;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryResponse {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long materialSpecId;
    private String specCode;
    private String specName;
    private MaterialCategory category;
    private Integer quantityOnHand;
    private Integer safetyStock;
    private boolean belowSafetyStock;
}
