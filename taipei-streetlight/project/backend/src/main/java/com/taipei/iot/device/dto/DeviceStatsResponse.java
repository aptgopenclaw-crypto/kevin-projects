package com.taipei.iot.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeviceStatsResponse {

    private long totalDevices;
    private Map<String, Long> byType;
    private Map<String, Long> byStatus;
    private double onlineRate;
    private long openFaults;
}
