package com.taipei.iot.device.dto;

import com.taipei.iot.device.enums.ConnectivityType;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeviceResponse {

    private Long id;
    private DeviceType deviceType;
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
    private String deptName;
    private Long contractId;
    private String propertyOwner;

    private DeviceStatus status;
    private LocalDate installedAt;
    private LocalDate decommissionedAt;

    private Long parentDeviceId;
    private String parentDeviceCode;
    private String mountPosition;
    private ConnectivityType connectivityType;
    private Map<String, Object> networkConfig;
    private LocalDateTime lastHeartbeatAt;

    private Long circuitId;
    private String circuitNumber;

    private Map<String, Object> attributes;

    private long childrenCount;

    /** 組合元件（燈具、控制器等），僅在取得單一設備明細時填入 */
    private List<DeviceResponse> children;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
