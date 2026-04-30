package com.taipei.iot.fault.dto;

import com.taipei.iot.fault.enums.FaultTicketSource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FaultTicketRequest {

    private Long deviceId;
    private Long circuitId;

    @NotNull(message = "來源為必填")
    private FaultTicketSource source;

    private String priority;
    private String description;
}
