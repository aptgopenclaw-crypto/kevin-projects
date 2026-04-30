package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.QualityFlag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class TelemetryResponse {

    private Long id;
    private LocalDateTime time;
    private Long deviceId;
    private Long formatId;
    private Map<String, Object> payload;
    private QualityFlag qualityFlag;
}
