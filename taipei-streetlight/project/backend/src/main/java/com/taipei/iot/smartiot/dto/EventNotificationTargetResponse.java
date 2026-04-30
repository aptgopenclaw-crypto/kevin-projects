package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.NotificationTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventNotificationTargetResponse {
    private Long id;
    private Long ruleId;
    private NotificationTargetType targetType;
    private String targetId;
    private LocalDateTime createdAt;
}
