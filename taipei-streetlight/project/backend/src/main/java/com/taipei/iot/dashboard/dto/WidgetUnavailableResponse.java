package com.taipei.iot.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 尚未啟用模組的 stub 回應 (panel-box / electricity-cost / meter)
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WidgetUnavailableResponse {

    private String widgetType;
    private boolean available;
    private String message;

    public static WidgetUnavailableResponse of(String widgetType) {
        return WidgetUnavailableResponse.builder()
                .widgetType(widgetType)
                .available(false)
                .message("此功能需待智能路燈模組啟用")
                .build();
    }
}
