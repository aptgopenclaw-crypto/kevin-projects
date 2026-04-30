package com.taipei.iot.smartiot.dto;

import com.taipei.iot.device.enums.ConnectivityType;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IoTDeviceResponse {

    private Long id;
    private String tenantId;
    private String deviceCode;
    private String deviceName;
    private DeviceType deviceType;
    private DeviceStatus status;
    private ConnectivityType connectivityType;

    // IoT 專屬
    private String deviceToken;
    private String authType;
    private String firmwareVersion;
    private LocalDateTime lastTelemetryAt;
    private LocalDateTime lastHeartbeatAt;
    private Long formatId;

    // 地理
    private Double lng;
    private Double lat;

    // 組織
    private Long deptId;
}
