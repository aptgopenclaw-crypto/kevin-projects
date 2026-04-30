package com.taipei.iot.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CircuitRequest {

    private Long panelBoxDeviceId;

    @NotBlank(message = "回路編號為必填")
    private String circuitNumber;

    private String circuitName;
    private String taipowerAccount;
    private String usageType;
}
