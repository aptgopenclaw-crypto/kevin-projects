package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventNotificationChannelResponse {
    private Long id;
    private Long ruleId;
    private NotificationChannel channel;
    private Map<String, Object> config;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
