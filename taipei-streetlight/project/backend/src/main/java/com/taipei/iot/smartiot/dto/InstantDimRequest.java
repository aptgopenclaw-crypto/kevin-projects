package com.taipei.iot.smartiot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InstantDimRequest {

    @NotNull(message = "設備 ID 為必填")
    private Long deviceId;

    @NotNull(message = "亮度為必填")
    @Min(value = 0, message = "亮度最小值為 0")
    @Max(value = 100, message = "亮度最大值為 100")
    private Integer brightness;
}
