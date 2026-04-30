package com.taipei.iot.repair.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import com.taipei.iot.repair.dto.CompletionReportRequest;
import com.taipei.iot.repair.dto.PublicRepairRequest;
import com.taipei.iot.repair.dto.PublicRepairStatusResponse;
import com.taipei.iot.repair.dto.RepairTicketQueryParams;
import com.taipei.iot.repair.dto.RepairTicketRequest;
import com.taipei.iot.repair.dto.RepairTicketResponse;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.replacement.entity.LightPoleNumber;
import com.taipei.iot.replacement.repository.LightPoleNumberRepository;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairTicketServiceTest {

    @InjectMocks private RepairTicketService repairTicketService;
    @Mock private RepairTicketRepository repairTicketRepository;
    @Mock private FaultTicketRepository faultTicketRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private WorkflowService workflowService;
    @Mock private DataScopeHelper dataScopeHelper;
    @Mock private LightPoleNumberRepository lightPoleNumberRepository;

    // ── createFromFault (路徑 A) ──

    @Test
    void createFromFault_success() {
        FaultTicket fault = FaultTicket.builder()
                .id(1L).deviceId(10L).circuitId(20L)
                .reportedBy("user-001").description("路燈不亮").build();
        when(faultTicketRepository.findById(1L)).thenReturn(Optional.of(fault));
        when(repairTicketRepository.save(any())).thenAnswer(inv -> {
            RepairTicket t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(workflowService.createInstance(anyString(), anyString(), anyLong(), any()))
                .thenReturn(1L);

        RepairTicket result = repairTicketService.createFromFault(1L);

        assertNotNull(result);
        assertEquals(RepairTicketSource.FAULT_TICKET, result.getSource());
        assertEquals(RepairTicketStatus.PENDING, result.getStatus());
        assertEquals(10L, result.getDeviceId());
        assertEquals(1L, result.getFaultTicketId());
        verify(workflowService).createInstance("REPAIR_DISPATCH", "REPAIR_TICKET", 100L, null);
    }

    @Test
    void createFromFault_faultNotFound_throwsException() {
        when(faultTicketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> repairTicketService.createFromFault(99L));
    }

    // ── createDirect (路徑 B) ──

    @Test
    void createDirect_success() {
        RepairTicketRequest request = RepairTicketRequest.builder()
                .source(RepairTicketSource.EXTERNAL_1999)
                .reporterName("王先生")
                .reporterPhone("0912345678")
                .reportDescription("路燈不亮")
                .priority(RepairTicketPriority.HIGH)
                .build();

        when(repairTicketRepository.save(any())).thenAnswer(inv -> {
            RepairTicket t = inv.getArgument(0);
            t.setId(200L);
            return t;
        });
        when(workflowService.createInstance(anyString(), anyString(), anyLong(), any()))
                .thenReturn(2L);

        RepairTicketResponse result = repairTicketService.createDirect(request);

        assertNotNull(result);
        assertEquals(RepairTicketPriority.HIGH, result.getPriority());
        verify(workflowService).createInstance("REPAIR_DISPATCH", "REPAIR_TICKET", 200L, null);
    }

    @Test
    void createDirect_withDeviceId_validatesDevice() {
        RepairTicketRequest request = RepairTicketRequest.builder()
                .source(RepairTicketSource.PHONE)
                .deviceId(999L)
                .build();

        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> repairTicketService.createDirect(request));
    }

    // ── accept ──

    @Test
    void accept_success() {
        RepairTicket ticket = RepairTicket.builder()
                .id(1L).status(RepairTicketStatus.PENDING).build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repairTicketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(10L);
        when(workflowService.findByTicket("REPAIR_TICKET", 1L)).thenReturn(instance);

        RepairTicketResponse result = repairTicketService.accept(1L);

        assertEquals(RepairTicketStatus.ACCEPTED, result.getStatus());
        verify(workflowService).transition(eq(10L), eq("ACCEPTED"), anyString(), any(), any(), any(), any());
    }

    @Test
    void accept_invalidStatus_throwsException() {
        RepairTicket ticket = RepairTicket.builder()
                .id(1L).status(RepairTicketStatus.DISPATCHED).build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> repairTicketService.accept(1L));
        assertEquals(ErrorCode.REPAIR_TICKET_INVALID_STATUS, ex.getErrorCode());
    }

    // ── reportCompletion ──

    @Test
    void reportCompletion_success() {
        RepairTicket ticket = RepairTicket.builder()
                .id(1L).status(RepairTicketStatus.IN_PROGRESS).build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repairTicketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(10L);
        when(workflowService.findByTicket("REPAIR_TICKET", 1L)).thenReturn(instance);

        CompletionReportRequest request = CompletionReportRequest.builder()
                .repairDescription("更換燈泡")
                .faultCause("燈泡老化")
                .build();

        repairTicketService.reportCompletion(1L, request);

        assertEquals(RepairTicketStatus.COMPLETION_REPORTED, ticket.getStatus());
        assertNotNull(ticket.getCompletedAt());
        assertEquals("更換燈泡", ticket.getRepairDescription());
    }

    @Test
    void reportCompletion_wrongStatus_throwsException() {
        RepairTicket ticket = RepairTicket.builder()
                .id(1L).status(RepairTicketStatus.PENDING).build();
        when(repairTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        CompletionReportRequest request = CompletionReportRequest.builder()
                .repairDescription("test").build();

        assertThrows(BusinessException.class,
                () -> repairTicketService.reportCompletion(1L, request));
    }

    // ── list ──

    @Test
    void list_returnsPage() {
        when(dataScopeHelper.getVisibleDeptIds()).thenReturn(Collections.emptyList());
        RepairTicket ticket = RepairTicket.builder()
                .id(1L).ticketNumber("RT-20260422-001")
                .source(RepairTicketSource.FAULT_TICKET)
                .status(RepairTicketStatus.PENDING)
                .priority(RepairTicketPriority.NORMAL).build();
        when(repairTicketRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        Page<RepairTicketResponse> result = repairTicketService.list(
                new RepairTicketQueryParams(), PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    // ── getById ──

    @Test
    void getById_notFound_throwsException() {
        when(repairTicketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> repairTicketService.getById(99L));
    }

    // ── createPublicTicket ──

    @Test
    void createPublicTicket_withoutPoleNumber_usesDefaultTenant() {
        PublicRepairRequest request = new PublicRepairRequest();
        request.setReporterName("張三");
        request.setReporterPhone("0912345678");
        request.setReportAddress("台北市中山區");
        request.setReportDescription("路燈不亮");

        when(repairTicketRepository.save(any())).thenAnswer(inv -> {
            RepairTicket t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(workflowService.createInstance(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(1L);

        RepairTicket result = repairTicketService.createPublicTicket(request);

        assertNotNull(result);
        assertEquals(RepairTicketSource.CITIZEN_WEB, result.getSource());
        assertEquals(RepairTicketStatus.PENDING, result.getStatus());
        assertEquals("CITIZEN", result.getCreatedBy());
        assertNull(result.getDeviceId());
        verify(workflowService).createInstance("REPAIR_DISPATCH", "REPAIR_TICKET", 1L, "CITIZEN");
    }

    @Test
    void createPublicTicket_withPoleNumber_resolvesDeviceAndTenant() {
        PublicRepairRequest request = new PublicRepairRequest();
        request.setReporterName("李四");
        request.setReporterPhone("0987654321");
        request.setReportAddress("信義區");
        request.setReportDescription("燈閃爍");
        request.setPoleNumber("P-001");

        LightPoleNumber pole = LightPoleNumber.builder()
                .tenantId("TENANT_B").deviceId(99L).poleNumber("P-001").build();
        when(lightPoleNumberRepository.findByPoleNumber("P-001")).thenReturn(Optional.of(pole));
        when(repairTicketRepository.save(any())).thenAnswer(inv -> {
            RepairTicket t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });
        when(workflowService.createInstance(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(1L);

        RepairTicket result = repairTicketService.createPublicTicket(request);

        assertEquals(99L, result.getDeviceId());
        verify(lightPoleNumberRepository).findByPoleNumber("P-001");
    }

    // ── getPublicStatus ──

    @Test
    void getPublicStatus_found_returnsResponse() {
        RepairTicket ticket = RepairTicket.builder()
                .ticketNumber("RT-20260424-001")
                .reporterPhone("0912345678")
                .status(RepairTicketStatus.PENDING)
                .build();
        when(repairTicketRepository.findByTicketNumberAndReporterPhone("RT-20260424-001", "0912345678"))
                .thenReturn(Optional.of(ticket));

        PublicRepairStatusResponse resp = repairTicketService.getPublicStatus("RT-20260424-001", "0912345678");

        assertEquals("RT-20260424-001", resp.getTicketNumber());
        assertEquals(RepairTicketStatus.PENDING, resp.getStatus());
        assertNotNull(resp.getStatusLabel());
    }

    @Test
    void getPublicStatus_notFound_throwsException() {
        when(repairTicketRepository.findByTicketNumberAndReporterPhone("RT-FAKE", "0900000000"))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> repairTicketService.getPublicStatus("RT-FAKE", "0900000000"));
    }
}
