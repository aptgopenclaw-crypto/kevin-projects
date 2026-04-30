package com.taipei.iot.fault.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.fault.dto.FaultTicketRequest;
import com.taipei.iot.fault.dto.FaultTicketResponse;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.enums.FaultTicketSource;
import com.taipei.iot.fault.enums.FaultTicketStatus;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import com.taipei.iot.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaultTicketServiceTest {

    @InjectMocks private FaultTicketService faultTicketService;
    @Mock private FaultTicketRepository faultTicketRepository;
    @Mock private FaultCorrelationService faultCorrelationService;

    private FaultTicket ticket;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("TENANT_A");
        var auth = new UsernamePasswordAuthenticationToken("user-001", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ticket = FaultTicket.builder().id(1L)
                .deviceId(10L).circuitId(5L)
                .source(FaultTicketSource.CITIZEN_REPORT)
                .status(FaultTicketStatus.OPEN)
                .priority("NORMAL").description("燈不亮").build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_returnsPage() {
        when(faultTicketRepository.findByFilters(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        Page<FaultTicketResponse> result = faultTicketService.list(null, null, PageRequest.of(0, 20));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getById_found() {
        when(faultTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        FaultTicketResponse res = faultTicketService.getById(1L);
        assertEquals(FaultTicketSource.CITIZEN_REPORT, res.getSource());
    }

    @Test
    void getById_notFound_throws() {
        when(faultTicketRepository.findById(99L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> faultTicketService.getById(99L));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void create_setsStatusOpenAndDefaultPriority() {
        FaultTicketRequest req = new FaultTicketRequest();
        req.setDeviceId(10L);
        req.setCircuitId(5L);
        req.setSource(FaultTicketSource.PATROL);
        req.setDescription("New fault");

        when(faultTicketRepository.save(any(FaultTicket.class))).thenAnswer(inv -> {
            FaultTicket t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });

        FaultTicketResponse res = faultTicketService.create(req);

        assertEquals(FaultTicketStatus.OPEN, res.getStatus());
        assertEquals("NORMAL", res.getPriority());
        verify(faultCorrelationService).detectOnNewTicket(any());
    }

    @Test
    void create_triggersCorrelationDetection() {
        FaultTicketRequest req = new FaultTicketRequest();
        req.setDeviceId(10L);
        req.setSource(FaultTicketSource.AUTO_ALERT);

        when(faultTicketRepository.save(any())).thenAnswer(inv -> {
            FaultTicket t = inv.getArgument(0);
            t.setId(3L);
            return t;
        });

        faultTicketService.create(req);

        verify(faultCorrelationService).detectOnNewTicket(any());
    }

    @Test
    void resolve_setsResolvedStatusAndTimestamp() {
        when(faultTicketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(faultTicketRepository.save(any())).thenReturn(ticket);

        faultTicketService.resolve(1L, "已修復");

        assertEquals(FaultTicketStatus.RESOLVED, ticket.getStatus());
        assertNotNull(ticket.getResolvedAt());
        assertEquals("user-001", ticket.getResolvedBy());
        assertEquals("已修復", ticket.getResolutionNote());
    }

    @Test
    void resolve_notFound_throws() {
        when(faultTicketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> faultTicketService.resolve(99L, null));
    }
}
