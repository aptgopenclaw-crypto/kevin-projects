package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AlertHistoryResponse {
    private Long id;
    private String tenantId;
    private Long ruleId;
    private Long deviceId;
    private AlertSeverity severity;
    private AlertStatus status;
    private String message;
    private Map<String, Object> triggeredValues;
    private LocalDateTime triggeredAt;
    private String ackBy;
    private LocalDateTime ackAt;
    private LocalDateTime resolvedAt;
    private Integer mttrMinutes;
    private Long faultTicketId;
    private Boolean notificationSent;
}
