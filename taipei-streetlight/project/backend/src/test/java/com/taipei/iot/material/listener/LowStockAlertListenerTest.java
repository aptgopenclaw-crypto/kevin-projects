package com.taipei.iot.material.listener;

import com.taipei.iot.material.event.LowStockAlertEvent;
import com.taipei.iot.notification.dto.NotificationPayload;
import com.taipei.iot.notification.enums.NotificationRefType;
import com.taipei.iot.notification.enums.NotificationType;
import com.taipei.iot.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LowStockAlertListenerTest {

    @InjectMocks private LowStockAlertListener listener;
    @Mock private NotificationService notificationService;

    @Test
    void onLowStock_sendsAlertNotification() {
        LowStockAlertEvent event = new LowStockAlertEvent("T001", "LED燈具-10W", "主倉庫", 3, 10);

        listener.onLowStock(event);

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).send(captor.capture());

        NotificationPayload payload = captor.getValue();
        assertEquals("T001", payload.getTenantId());
        assertEquals(NotificationType.ALERT, payload.getType());
        assertTrue(payload.getTitle().contains("庫存"));
        assertTrue(payload.getContent().contains("LED燈具-10W"));
        assertTrue(payload.getContent().contains("主倉庫"));
        assertTrue(payload.getContent().contains("3"));
        assertTrue(payload.getContent().contains("10"));
    }

    @Test
    void onLowStock_verifyRefTypeIsMaterial() {
        LowStockAlertEvent event = new LowStockAlertEvent("T002", "鈉光燈-250W", "北區倉庫", 0, 5);

        listener.onLowStock(event);

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).send(captor.capture());

        NotificationPayload payload = captor.getValue();
        assertEquals(NotificationRefType.MATERIAL, payload.getRefType());
        assertTrue(payload.getUserIds().isEmpty());
    }

    @Test
    void onLowStock_zeroQuantity_sendsAlert() {
        LowStockAlertEvent event = new LowStockAlertEvent("T001", "LED燈具-20W", "南區倉庫", 0, 15);

        listener.onLowStock(event);

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(notificationService).send(captor.capture());

        NotificationPayload payload = captor.getValue();
        assertTrue(payload.getContent().contains("0"));
        assertTrue(payload.getContent().contains("15"));
        assertTrue(payload.getContent().contains("南區倉庫"));
    }
}
