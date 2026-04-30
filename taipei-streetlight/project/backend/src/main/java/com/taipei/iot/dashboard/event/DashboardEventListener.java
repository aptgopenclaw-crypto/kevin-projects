package com.taipei.iot.dashboard.event;

import com.taipei.iot.dashboard.service.DashboardPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 監聽 {@link DashboardEvent} 並透過 WebSocket 推送至前端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardEventListener {

    private final DashboardPushService pushService;

    @Async
    @EventListener
    public void onDashboardEvent(DashboardEvent event) {
        log.debug("DashboardEvent received — tenant={}, widget={}",
                event.getTenantId(), event.getWidgetType().getKey());
        pushService.pushToTenant(event.getTenantId(), event.getWidgetType(), event.getData());
    }
}
