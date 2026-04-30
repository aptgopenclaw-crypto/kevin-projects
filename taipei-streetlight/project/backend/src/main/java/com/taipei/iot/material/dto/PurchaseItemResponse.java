package com.taipei.iot.material.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseItemResponse {
    private Long id;
    private Long materialSpecId;
    private String specCode;
    private String specName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String notes;
}
