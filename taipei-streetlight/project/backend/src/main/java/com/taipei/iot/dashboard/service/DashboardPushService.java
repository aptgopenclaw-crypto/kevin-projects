package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.event.DashboardPushMessage;
import com.taipei.iot.dashboard.enums.WidgetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 儀表板即時推送服務。
 * <p>
 * 透過 STOMP 將 widget 數據推送到 /topic/tenant/{tenantId}/dashboard。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardPushService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送 widget 數據到指定 tenant 的所有已連線使用者。
     */
    public void pushToTenant(String tenantId, WidgetType widgetType, Object data) {
        DashboardPushMessage message = DashboardPushMessage.builder()
                .widget(widgetType.getKey())
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        String destination = "/topic/tenant/" + tenantId + "/dashboard";
        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Dashboard push sent to {} — widget={}", destination, widgetType.getKey());
        } catch (Exception e) {
            log.warn("Dashboard push failed for tenant={}, widget={}: {}",
                    tenantId, widgetType.getKey(), e.getMessage());
        }
    }
}
