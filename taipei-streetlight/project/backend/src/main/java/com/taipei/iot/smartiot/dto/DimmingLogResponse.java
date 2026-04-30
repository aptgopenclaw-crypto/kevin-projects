package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.DimmingCommandType;
import com.taipei.iot.smartiot.enums.DimmingResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DimmingLogResponse {
    private Long id;
    private Long deviceId;
    private DimmingCommandType commandType;
    private Integer brightnessPct;
    private DimmingResult result;
    private LocalDateTime sentAt;
    private LocalDateTime ackAt;
    private Long scheduleId;
}
