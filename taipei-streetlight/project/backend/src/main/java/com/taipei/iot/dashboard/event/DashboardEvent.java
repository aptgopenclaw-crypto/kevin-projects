package com.taipei.iot.dashboard.event;

import com.taipei.iot.dashboard.enums.WidgetType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 儀表板即時推送事件。
 * <p>
 * 由業務服務（例如設備狀態、停電偵測）publish，
 * 由 {@link DashboardEventListener} 接收後透過 STOMP 推送至前端。
 */
@Getter
public class DashboardEvent extends ApplicationEvent {

    private final String tenantId;
    private final WidgetType widgetType;
    private final Object data;

    public DashboardEvent(Object source, String tenantId, WidgetType widgetType, Object data) {
        super(source);
        this.tenantId = tenantId;
        this.widgetType = widgetType;
        this.data = data;
    }
}
