package com.taipei.iot.device.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComponentReplaceRequest {

    /** 要被置換的舊元件 ID */
    @NotNull(message = "舊元件 ID 為必填")
    private Long oldDeviceId;

    /** 新元件資料 */
    @NotNull(message = "新元件資料為必填")
    @Valid
    private DeviceRequest newDevice;

    /** 置換原因說明 */
    private String reason;
}
