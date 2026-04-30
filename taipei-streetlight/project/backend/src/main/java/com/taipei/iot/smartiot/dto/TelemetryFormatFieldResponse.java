package com.taipei.iot.smartiot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TelemetryFormatFieldResponse {

    private String name;
    private String type;
    private String unit;
    private boolean virtual;
}
