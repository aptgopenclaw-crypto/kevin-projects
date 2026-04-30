package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OutageAlertResponse {

    private int currentOutageCount;
    private List<OutageZone> outageZones;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OutageZone {
        private String zone;
        private int affectedCount;
        private LocalDateTime since;
    }
}
