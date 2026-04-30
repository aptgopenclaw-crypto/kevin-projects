package com.taipei.iot.replacement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementItemRequest {

    @NotNull(message = "燈桿設備 ID 為必填")
    private Long parentDeviceId;

    @NotNull(message = "舊設備 ID 為必填")
    private Long oldDeviceId;

    private String afterDeviceType;
    private Map<String, Object> afterSpec;
    private Long materialSpecId;
    private Long approvedMaterialId;
}
