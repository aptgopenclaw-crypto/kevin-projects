package com.taipei.iot.repair.listener;

import com.taipei.iot.device.enums.DeviceStatus;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairDispatchedListenerTest {

    @InjectMocks private RepairDispatchedListener listener;
    @Mock private DeviceService deviceService;
    @Mock private RepairTicketRepository repairTicketRepository;

    @Test
    void onRepairDispatched_updatesDeviceToUnderRepair() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_DISPATCH");
        instance.setTicketId(1L);

        RepairTicket ticket = RepairTicket.builder()
                .id(1L).ticketNumber("RT-20260425-001").deviceId(10L).build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "DISPATCHED", "派工");
        listener.onRepairDispatched(event);

        verify(deviceService).updateStatus(10L, DeviceStatus.UNDER_REPAIR);
    }

    @Test
    void onRepairDispatched_wrongWorkflowType_skips() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("FAULT_REVIEW");

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "DISPATCHED", "test");
        listener.onRepairDispatched(event);

        verifyNoInteractions(repairTicketRepository);
        verifyNoInteractions(deviceService);
    }

    @Test
    void onRepairDispatched_wrongStep_skips() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_DISPATCH");

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "CLOSED", "結案");
        listener.onRepairDispatched(event);

        verifyNoInteractions(repairTicketRepository);
        verifyNoInteractions(deviceService);
    }

    @Test
    void onRepairDispatched_ticketNotFound_skipsDeviceUpdate() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_DISPATCH");
        instance.setTicketId(99L);

        when(repairTicketRepository.findById(99L)).thenReturn(Optional.empty());

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "DISPATCHED", "派工");
        listener.onRepairDispatched(event);

        verify(repairTicketRepository).findById(99L);
        verifyNoInteractions(deviceService);
    }

    @Test
    void onRepairDispatched_noDevice_skipsDeviceUpdate() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_DISPATCH");
        instance.setTicketId(2L);

        RepairTicket ticket = RepairTicket.builder()
                .id(2L).ticketNumber("RT-20260425-002").deviceId(null).build();
        when(repairTicketRepository.findById(2L)).thenReturn(Optional.of(ticket));

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "DISPATCHED", "派工");
        listener.onRepairDispatched(event);

        verify(repairTicketRepository).findById(2L);
        verifyNoInteractions(deviceService);
    }
}
