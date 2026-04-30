package com.taipei.iot.repair.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.repair.dto.DispatchRequest;
import com.taipei.iot.repair.dto.DispatchResponse;
import com.taipei.iot.repair.entity.RepairDispatch;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.enums.RepairDispatchStatus;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.repository.RepairDispatchRepository;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairDispatchServiceTest {

    @InjectMocks private RepairDispatchService repairDispatchService;
    @Mock private RepairDispatchRepository repairDispatchRepository;
    @Mock private RepairTicketRepository repairTicketRepository;
    @Mock private WorkflowService workflowService;

    @Test
    void dispatch_success() {
        RepairTicket ticket = RepairTicket.builder()
                .id(1L).status(RepairTicketStatus.ACCEPTED).build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repairDispatchRepository.save(any())).thenAnswer(inv -> {
            RepairDispatch d = inv.getArgument(0);
            d.setId(10L);
            return d;
        });
        when(repairTicketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(20L);
        when(workflowService.findByTicket("REPAIR_TICKET", 1L)).thenReturn(instance);

        DispatchRequest request = DispatchRequest.builder()
                .assignedTo(100L)
                .assignedOrg("光輝維護公司")
                .contractId(5L)
                .dueDate(LocalDate.now().plusDays(7))
                .note("請盡速維修")
                .build();

        DispatchResponse result = repairDispatchService.dispatch(1L, request);

        assertNotNull(result);
        assertEquals(RepairDispatchStatus.DISPATCHED, result.getStatus());
        assertEquals("光輝維護公司", result.getAssignedOrg());
        assertEquals(RepairTicketStatus.DISPATCHED, ticket.getStatus());
        verify(workflowService).transition(eq(20L), eq("DISPATCHED"), anyString(), any(), any(), any(), any());
    }

    @Test
    void dispatch_invalidStatus_throwsException() {
        RepairTicket ticket = RepairTicket.builder()
                .id(1L).status(RepairTicketStatus.IN_PROGRESS).build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        DispatchRequest request = DispatchRequest.builder().contractId(5L).build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> repairDispatchService.dispatch(1L, request));
        assertEquals(ErrorCode.REPAIR_TICKET_INVALID_STATUS, ex.getErrorCode());
    }

    @Test
    void dispatch_ticketNotFound_throwsException() {
        when(repairTicketRepository.findById(99L)).thenReturn(Optional.empty());

        DispatchRequest request = DispatchRequest.builder().contractId(5L).build();

        assertThrows(BusinessException.class,
                () -> repairDispatchService.dispatch(99L, request));
    }

    @Test
    void dispatch_fromTransferred_success() {
        RepairTicket ticket = RepairTicket.builder()
                .id(1L).status(RepairTicketStatus.TRANSFERRED).build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repairDispatchRepository.save(any())).thenAnswer(inv -> {
            RepairDispatch d = inv.getArgument(0);
            d.setId(11L);
            return d;
        });
        when(repairTicketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(21L);
        when(workflowService.findByTicket("REPAIR_TICKET", 1L)).thenReturn(instance);

        DispatchRequest request = DispatchRequest.builder()
                .assignedOrg("新廠商").contractId(6L).build();

        DispatchResponse result = repairDispatchService.dispatch(1L, request);
        assertNotNull(result);
    }

    @Test
    void getByTicketId_returnsList() {
        RepairDispatch d = RepairDispatch.builder()
                .id(1L).repairTicketId(10L).assignedOrg("ABC公司")
                .status(RepairDispatchStatus.DISPATCHED).build();
        when(repairDispatchRepository.findByRepairTicketIdOrderByDispatchedAtDesc(10L))
                .thenReturn(List.of(d));

        List<DispatchResponse> result = repairDispatchService.getByTicketId(10L);

        assertEquals(1, result.size());
        assertEquals("ABC公司", result.get(0).getAssignedOrg());
    }
}
