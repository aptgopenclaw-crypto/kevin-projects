package com.taipei.iot.repair.listener;

import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairClosedListenerTest {

    @InjectMocks private RepairClosedListener listener;
    @Mock private DeviceEventService deviceEventService;
    @Mock private DeviceService deviceService;
    @Mock private RepairTicketRepository repairTicketRepository;

    @Test
    void onRepairClosed_updatesDeviceAndRecordsEvent() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_CLOSE");
        instance.setTicketId(1L);

        RepairTicket ticket = RepairTicket.builder()
                .id(1L).ticketNumber("RT-20260422-001")
                .deviceId(10L).repairDescription("更換燈泡").build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "CLOSED", "結案");
        listener.onRepairClosed(event);

        verify(deviceService).updateStatus(10L, DeviceStatus.ACTIVE);
        verify(deviceEventService).recordEvent(eq(10L), eq(DeviceEventType.REPAIR),
                contains("RT-20260422-001"), isNull(), eq(1L), isNull());
    }

    @Test
    void onRepairClosed_wrongWorkflowType_skips() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_DISPATCH");

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "CLOSED", "test");
        listener.onRepairClosed(event);

        verifyNoInteractions(deviceService, deviceEventService);
    }

    @Test
    void onRepairClosed_noDevice_skipsDeviceOps() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_CLOSE");
        instance.setTicketId(2L);

        RepairTicket ticket = RepairTicket.builder()
                .id(2L).ticketNumber("RT-20260422-002")
                .deviceId(null).build();
        when(repairTicketRepository.findById(2L)).thenReturn(Optional.of(ticket));

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "CLOSED", "結案");
        listener.onRepairClosed(event);

        verifyNoInteractions(deviceService, deviceEventService);
    }
}
