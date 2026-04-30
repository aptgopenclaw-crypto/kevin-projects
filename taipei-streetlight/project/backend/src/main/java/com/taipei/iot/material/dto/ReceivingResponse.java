package com.taipei.iot.material.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReceivingResponse {
    private Long id;
    private Long poId;
    private String poNumber;
    private Long warehouseId;
    private String warehouseName;
    private Long materialSpecId;
    private String specName;
    private Integer quantity;
    private LocalDate receivedDate;
    private String deliveryNote;
    private String receivedBy;
    private LocalDateTime createdAt;
}
