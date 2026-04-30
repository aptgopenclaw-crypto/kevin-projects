package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.DimmingTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DimmingScheduleRequest {

    @NotBlank(message = "排程名稱為必填")
    private String scheduleName;

    @NotNull(message = "目標類型為必填")
    private DimmingTargetType targetType;

    private Long targetId;

    @NotNull(message = "亮度為必填")
    @Min(value = 0, message = "亮度最小值為 0")
    @Max(value = 100, message = "亮度最大值為 100")
    private Integer brightnessPct;

    private String scheduleCron;

    private LocalDateTime oneTimeAt;

    private Boolean enabled;
}
