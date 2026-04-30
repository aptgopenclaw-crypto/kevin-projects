package com.taipei.iot.material.listener;

import com.taipei.iot.material.event.LowStockAlertEvent;
import com.taipei.iot.notification.dto.NotificationPayload;
import com.taipei.iot.notification.enums.NotificationRefType;
import com.taipei.iot.notification.enums.NotificationType;
import com.taipei.iot.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * E12：庫存低於安全量 → 推播預警通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LowStockAlertListener {

    private final NotificationService notificationService;

    @EventListener
    public void onLowStock(LowStockAlertEvent event) {
        log.warn("E12 事件：庫存預警 — {} ({}) 現有 {} / 安全量 {}",
                event.getSpecName(), event.getWarehouseName(),
                event.getQuantityOnHand(), event.getSafetyStock());

        NotificationPayload payload = NotificationPayload.builder()
                .tenantId(event.getTenantId())
                .userIds(List.of())   // broadcast — InAppChannel handles empty userIds as broadcast
                .type(NotificationType.ALERT)
                .title("庫存安全量預警")
                .content(String.format("材料 [%s] 在 [%s] 庫存量 %d 已低於安全量 %d，請儘速補貨。",
                        event.getSpecName(), event.getWarehouseName(),
                        event.getQuantityOnHand(), event.getSafetyStock()))
                .refType(NotificationRefType.MATERIAL)
                .build();

        notificationService.send(payload);
    }
}
