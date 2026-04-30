package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LampCountResponse {

    private long total;
    private Map<String, Long> byContractor;
    private Map<String, Long> byType;
    private Map<String, Long> byLightSource;
    private Map<String, Long> byFacilityType;
}
