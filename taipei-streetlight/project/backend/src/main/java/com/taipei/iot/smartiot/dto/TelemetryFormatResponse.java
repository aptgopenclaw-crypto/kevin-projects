package com.taipei.iot.smartiot.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TelemetryFormatResponse {

    private Long id;
    private String tenantId;
    private String vendorName;
    private String deviceModel;
    private Integer version;
    private List<Map<String, Object>> fieldDefinitions;
    private Map<String, Object> samplePayload;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
