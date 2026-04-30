package com.taipei.iot.dashboard.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WidgetType {

    MAINTENANCE_STATS("maintenance-stats", "養護統計", false),
    OUTAGE_ALERT("outage-alert", "停電告警", true),
    FAULT_HEATMAP("fault-heatmap", "故障熱力圖", false),
    KPI_SUMMARY("kpi-summary", "KPI 績效", false),
    LAMP_COUNT("lamp-count", "路燈數量", false),
    LAMP_STATUS("lamp-status", "路燈在線/離線", true),
    PANEL_BOX("panel-box", "配電箱", false),
    ATTACHMENTS("attachments", "附件統計", false),
    ELECTRICITY_COST("electricity-cost", "電費", false),
    METER("meter", "智慧電表", false),
    GIS_OVERVIEW("gis-overview", "GIS 總覽", true),
    ;

    private final String key;
    private final String label;
    private final boolean realtime;  // 是否支援 WebSocket 即時推送
}
