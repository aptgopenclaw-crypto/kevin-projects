package com.taipei.iot.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CircuitResponse {

    private Long id;
    private Long panelBoxDeviceId;
    private String circuitNumber;
    private String circuitName;
    private String taipowerAccount;
    private String usageType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
