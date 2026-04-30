package com.taipei.iot.repair.listener;

import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import com.taipei.iot.repair.service.RepairTicketService;
import com.taipei.iot.repair.entity.RepairTicket;
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
class FaultApprovedListenerTest {

    @InjectMocks private FaultApprovedListener listener;
    @Mock private RepairTicketService repairTicketService;
    @Mock private DeviceService deviceService;
    @Mock private FaultTicketRepository faultTicketRepository;

    @Test
    void onFaultConfirmed_createsRepairAndUpdatesDevice() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("FAULT_REVIEW");
        instance.setTicketId(1L);

        FaultTicket fault = FaultTicket.builder().id(1L).deviceId(10L).build();
        when(faultTicketRepository.findById(1L)).thenReturn(Optional.of(fault));
        when(repairTicketService.createFromFault(1L)).thenReturn(new RepairTicket());

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "CONFIRMED", "審核通過");
        listener.onFaultConfirmed(event);

        verify(repairTicketService).createFromFault(1L);
        verify(deviceService).updateStatus(10L, DeviceStatus.REPORTED);
    }

    @Test
    void onFaultConfirmed_wrongWorkflowType_skips() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("REPAIR_DISPATCH");

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "CONFIRMED", "test");
        listener.onFaultConfirmed(event);

        verifyNoInteractions(repairTicketService);
    }

    @Test
    void onFaultConfirmed_wrongStep_skips() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("FAULT_REVIEW");

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "REJECTED", "駁回");
        listener.onFaultConfirmed(event);

        verifyNoInteractions(repairTicketService);
    }

    @Test
    void onFaultConfirmed_noDevice_skipsDeviceUpdate() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowType("FAULT_REVIEW");
        instance.setTicketId(2L);

        FaultTicket fault = FaultTicket.builder().id(2L).deviceId(null).build();
        when(faultTicketRepository.findById(2L)).thenReturn(Optional.of(fault));
        when(repairTicketService.createFromFault(2L)).thenReturn(new RepairTicket());

        WorkflowTransitionEvent event = new WorkflowTransitionEvent(
                this, instance, "CONFIRMED", "審核通過");
        listener.onFaultConfirmed(event);

        verify(repairTicketService).createFromFault(2L);
        verifyNoInteractions(deviceService);
    }
}
