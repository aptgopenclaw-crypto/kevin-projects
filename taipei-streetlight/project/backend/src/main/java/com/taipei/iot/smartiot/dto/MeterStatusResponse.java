package com.taipei.iot.smartiot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MeterStatusResponse {
    private long totalMeters;
    private long onlineMeters;
    private long offlineMeters;
    private long anomalyMeters;
}
