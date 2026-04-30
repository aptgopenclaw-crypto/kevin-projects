package com.taipei.iot.replacement.listener;

import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.enums.ReplacementItemStatus;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplacementSelfCheckedListenerTest {

    @InjectMocks private ReplacementSelfCheckedListener listener;
    @Mock private ReplacementOrderRepository orderRepo;
    @Mock private ReplacementItemRepository itemRepo;
    @Mock private DeviceEventService deviceEventService;

    @Test
    void onSelfChecked_recordsInspectEvent() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPLACEMENT_REVIEW");
        instance.setTicketId(1L);

        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).orderNumber("RO-001").build();
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));

        ReplacementItem item = ReplacementItem.builder()
                .id(10L).orderId(1L).oldDeviceId(100L).newDeviceId(200L)
                .status(ReplacementItemStatus.COMPLETED).build();
        when(itemRepo.findByOrderId(1L)).thenReturn(List.of(item));

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "SELF_CHECKED", "APPROVE");
        listener.onSelfChecked(event);

        verify(deviceEventService).recordEvent(eq(200L), eq(DeviceEventType.INSPECT),
                contains("RO-001"), isNull(), isNull(), eq(10L));
    }

    @Test
    void onSelfChecked_wrongStep_ignores() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPLACEMENT_REVIEW");
        instance.setTicketId(1L);

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "CLOSED", "APPROVE");
        listener.onSelfChecked(event);

        verifyNoInteractions(orderRepo);
    }

    @Test
    void onSelfChecked_wrongType_ignores() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_CLOSE");
        instance.setTicketId(1L);

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(this, instance, "SELF_CHECKED", "APPROVE");
        listener.onSelfChecked(event);

        verifyNoInteractions(orderRepo);
    }
}
