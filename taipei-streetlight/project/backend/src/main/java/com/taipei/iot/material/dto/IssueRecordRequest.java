package com.taipei.iot.material.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IssueRecordRequest {

    @NotNull(message = "庫存為必填")
    private Long inventoryId;

    @NotNull(message = "材料規格為必填")
    private Long materialSpecId;

    @NotNull(message = "數量為必填")
    private Integer quantity;
}
