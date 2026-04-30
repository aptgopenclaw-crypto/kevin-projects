package com.taipei.iot.device.dto;

import com.taipei.iot.device.enums.ConnectivityType;
import com.taipei.iot.device.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeviceRequest {

    @NotNull(message = "設備類型為必填")
    private DeviceType deviceType;

    @NotBlank(message = "設備代碼為必填")
    private String deviceCode;

    private String deviceName;

    private BigDecimal twd97X;
    private BigDecimal twd97Y;
    private BigDecimal lng;
    private BigDecimal lat;
    private BigDecimal elevation;
    private BigDecimal twd67X;
    private BigDecimal twd67Y;
    private String taipowerCoord;

    private Long deptId;
    private Long contractId;
    private String propertyOwner;

    private LocalDate installedAt;

    private Long parentDeviceId;
    private String mountPosition;
    private ConnectivityType connectivityType;
    private Map<String, Object> networkConfig;

    private Long circuitId;
    private Map<String, Object> attributes;
}
