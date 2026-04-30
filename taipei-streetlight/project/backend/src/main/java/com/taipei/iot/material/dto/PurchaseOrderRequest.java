package com.taipei.iot.material.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseOrderRequest {

    @NotNull(message = "廠商為必填")
    private Long supplierId;

    private Long contractId;
    private String notes;

    @NotNull(message = "採購明細為必填")
    private List<PurchaseItemRequest> items;
}
