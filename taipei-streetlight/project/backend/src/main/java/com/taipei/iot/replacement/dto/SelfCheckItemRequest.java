package com.taipei.iot.replacement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SelfCheckItemRequest {

    @NotNull(message = "明細 ID 為必填")
    private Long itemId;

    @NotBlank(message = "新設備代碼為必填（掃描實體標籤）")
    private String deviceCode;

    private Long newDeviceId;

    private String notes;
}
