package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.PurchaseOrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private Long contractId;
    private LocalDate orderDate;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<PurchaseItemResponse> items;
}
