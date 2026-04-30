package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.WarehouseStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WarehouseResponse {
    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private String location;
    private WarehouseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
