package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.NotificationChannel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventNotificationChannelRequest {
    @NotNull
    private NotificationChannel channel;
    private Map<String, Object> config;
    private Boolean enabled;
}
