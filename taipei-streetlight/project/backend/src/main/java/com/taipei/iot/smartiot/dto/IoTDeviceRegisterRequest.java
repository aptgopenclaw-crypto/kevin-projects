package com.taipei.iot.smartiot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * IoT 設備註冊請求 — 將既有 device 啟用 IoT 功能。
 */
@Getter
@Setter
public class IoTDeviceRegisterRequest {

    @NotNull(message = "設備 ID 為必填")
    private Long deviceId;

    @Size(max = 20, message = "認證方式長度不得超過 20")
    private String authType;   // TOKEN / CERT / PSK — 預設 TOKEN

    @Size(max = 50, message = "韌體版本長度不得超過 50")
    private String firmwareVersion;

    private Long formatId;
}
