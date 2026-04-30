package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.WarehouseStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WarehouseRequest {

    @NotBlank(message = "庫別代碼為必填")
    private String warehouseCode;

    @NotBlank(message = "庫別名稱為必填")
    private String warehouseName;

    private String location;
    private WarehouseStatus status;
}
