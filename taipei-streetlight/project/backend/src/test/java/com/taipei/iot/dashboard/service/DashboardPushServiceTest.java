package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.enums.WidgetType;
import com.taipei.iot.dashboard.event.DashboardPushMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardPushServiceTest {

    @InjectMocks private DashboardPushService service;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @Test
    void pushToTenant_sendsCorrectMessage() {
        Map<String, Object> data = Map.of("online", 15000L, "offline", 1000L);

        service.pushToTenant("T1", WidgetType.LAMP_STATUS, data);

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DashboardPushMessage> msgCaptor = ArgumentCaptor.forClass(DashboardPushMessage.class);
        verify(messagingTemplate).convertAndSend(destCaptor.capture(), msgCaptor.capture());

        assertEquals("/topic/tenant/T1/dashboard", destCaptor.getValue());

        DashboardPushMessage msg = msgCaptor.getValue();
        assertEquals("lamp-status", msg.getWidget());
        assertSame(data, msg.getData());
        assertTrue(msg.getTimestamp() > 0);
    }

    @Test
    void pushToTenant_outageAlert_correctWidget() {
        Map<String, Object> data = Map.of("currentOutageCount", 3);

        service.pushToTenant("T2", WidgetType.OUTAGE_ALERT, data);

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DashboardPushMessage> msgCaptor = ArgumentCaptor.forClass(DashboardPushMessage.class);
        verify(messagingTemplate).convertAndSend(destCaptor.capture(), msgCaptor.capture());

        assertEquals("/topic/tenant/T2/dashboard", destCaptor.getValue());
        assertEquals("outage-alert", msgCaptor.getValue().getWidget());
    }

    @Test
    void pushToTenant_templateThrows_doesNotPropagate() {
        doThrow(new RuntimeException("WS error"))
                .when(messagingTemplate).convertAndSend(anyString(), any(DashboardPushMessage.class));

        // Should not throw
        assertDoesNotThrow(() ->
                service.pushToTenant("T1", WidgetType.GIS_OVERVIEW, Map.of()));
    }
}
