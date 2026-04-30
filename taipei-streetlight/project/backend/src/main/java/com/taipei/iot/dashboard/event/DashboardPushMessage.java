package com.taipei.iot.dashboard.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket 推送至前端的 payload 包裝。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPushMessage {

    /** Widget 類型 key (e.g. "outage-alert") */
    private String widget;

    /** 推送數據 (具體結構依 widget 而異) */
    private Object data;

    /** 推送時間戳 (epoch ms) */
    private long timestamp;
}
