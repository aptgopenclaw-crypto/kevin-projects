package com.taipei.iot.material.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseItemRequest {

    @NotNull(message = "材料規格為必填")
    private Long materialSpecId;

    @NotNull(message = "數量為必填")
    private Integer quantity;

    private BigDecimal unitPrice;
    private String notes;
}
