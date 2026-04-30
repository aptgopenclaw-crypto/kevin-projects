package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.NotificationTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventNotificationTargetRequest {
    @NotNull
    private NotificationTargetType targetType;
    @NotNull
    private String targetId;
}
