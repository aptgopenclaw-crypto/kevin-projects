package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.DisposalType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DisposalRequest {

    @NotNull(message = "材料規格為必填")
    private Long materialSpecId;

    @NotNull(message = "數量為必填")
    private Integer quantity;

    @NotNull(message = "處置類型為必填")
    private DisposalType disposalType;

    private String reason;
}
