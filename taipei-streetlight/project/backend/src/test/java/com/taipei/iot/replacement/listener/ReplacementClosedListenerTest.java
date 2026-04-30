package com.taipei.iot.replacement.listener;

import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.repository.ReplacementItemRepository;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplacementClosedListenerTest {

    @InjectMocks private ReplacementClosedListener listener;
    @Mock private ReplacementOrderRepository orderRepo;
    @Mock private ReplacementItemRepository itemRepo;
    @Mock private DeviceEventService deviceEventService;
    @Mock private DeviceService deviceService;

    @Test
    void onReplacementClosed_updatesStatusAndDevices() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPLACEMENT_REVIEW");
        instance.setTicketId(1L);

        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).orderNumber("RO-001")
                .status(ReplacementOrderStatus.PENDING_REVIEW).build();
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));

        ReplacementItem item = ReplacementItem.builder()
                .id(10L).orderId(1L).oldDeviceId(100L).newDeviceId(200L)
                .status(ReplacementItemStatus.COMPLETED).build();
        when(itemRepo.findByOrderId(1L)).thenReturn(List.of(item));

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "CLOSED", "APPROVE");
        listener.onReplacementClosed(event);

        assertEquals(ReplacementOrderStatus.CLOSED, order.getStatus());
        verify(orderRepo).save(order);

        // old device decommissioned
        verify(deviceService).updateStatus(100L, DeviceStatus.DECOMMISSIONED);
        verify(deviceEventService).recordEvent(eq(100L), eq(DeviceEventType.DECOMMISSION),
                contains("RO-001"), isNull(), isNull(), eq(10L));

        // new device activated
        verify(deviceService).updateStatus(200L, DeviceStatus.ACTIVE);
        verify(deviceEventService).recordEvent(eq(200L), eq(DeviceEventType.REPLACE),
                contains("RO-001"), isNull(), isNull(), eq(10L));
    }

    @Test
    void onReplacementClosed_wrongWorkflowType_ignores() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_CLOSE");
        instance.setTicketId(1L);

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "CLOSED", "APPROVE");
        listener.onReplacementClosed(event);

        verifyNoInteractions(orderRepo);
    }

    @Test
    void onReplacementClosed_noItems_noDeviceUpdates() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPLACEMENT_REVIEW");
        instance.setTicketId(2L);

        ReplacementOrder order = ReplacementOrder.builder()
                .id(2L).orderNumber("RO-002")
                .status(ReplacementOrderStatus.PENDING_REVIEW).build();
        when(orderRepo.findById(2L)).thenReturn(Optional.of(order));
        when(itemRepo.findByOrderId(2L)).thenReturn(List.of());

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "CLOSED", "APPROVE");
        listener.onReplacementClosed(event);

        assertEquals(ReplacementOrderStatus.CLOSED, order.getStatus());
        verifyNoInteractions(deviceService);
        verifyNoInteractions(deviceEventService);
    }
}
