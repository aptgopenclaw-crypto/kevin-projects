package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.DimmingTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DimmingScheduleResponse {
    private Long id;
    private String scheduleName;
    private DimmingTargetType targetType;
    private Long targetId;
    private Integer brightnessPct;
    private String scheduleCron;
    private LocalDateTime oneTimeAt;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
