package com.taipei.iot.dashboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LayoutRequest {

    @NotNull(message = "版面配置 JSON 不可為空")
    private String layoutJson;
}
