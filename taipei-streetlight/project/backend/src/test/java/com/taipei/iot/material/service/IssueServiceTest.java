package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.material.dto.IssueRecordRequest;
import com.taipei.iot.material.dto.IssueRequestRequest;
import com.taipei.iot.material.dto.IssueRequestResponse;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.entity.IssueRequest;
import com.taipei.iot.material.enums.IssueRequestStatus;
import com.taipei.iot.material.repository.InventoryRepository;
import com.taipei.iot.material.repository.IssueRecordRepository;
import com.taipei.iot.material.repository.IssueRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @InjectMocks private IssueService issueService;
    @Mock private IssueRequestRepository issueRequestRepository;
    @Mock private IssueRecordRepository issueRecordRepository;
    @Mock private InventoryRepository inventoryRepository;

    @Test
    void createManual_success() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class)) {
            scu.when(SecurityContextUtils::getCurrentUserId).thenReturn("admin");

            when(issueRequestRepository.save(any())).thenAnswer(inv -> {
                IssueRequest r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            IssueRequestRequest req = IssueRequestRequest.builder().repairTicketId(10L).build();
            IssueRequestResponse resp = issueService.createManual(req);

            assertNotNull(resp);
            assertEquals(IssueRequestStatus.PENDING, resp.getStatus());
            assertEquals(10L, resp.getRepairTicketId());
        }
    }

    @Test
    void approve_success() {
        IssueRequest request = IssueRequest.builder().id(1L).status(IssueRequestStatus.PENDING).build();
        when(issueRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(issueRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IssueRequestResponse resp = issueService.approve(1L);
        assertEquals(IssueRequestStatus.APPROVED, resp.getStatus());
    }

    @Test
    void approve_wrongStatus_throws() {
        IssueRequest request = IssueRequest.builder().id(1L).status(IssueRequestStatus.ISSUED).build();
        when(issueRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(BusinessException.class, () -> issueService.approve(1L));
    }

    @Test
    void issue_success() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class)) {
            scu.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");

            IssueRequest request = IssueRequest.builder().id(1L).status(IssueRequestStatus.APPROVED).build();
            when(issueRequestRepository.findById(1L)).thenReturn(Optional.of(request));

            Inventory inv = Inventory.builder().id(10L).quantityOnHand(50).build();
            when(inventoryRepository.findById(10L)).thenReturn(Optional.of(inv));
            when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(issueRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(issueRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            IssueRecordRequest item = IssueRecordRequest.builder()
                    .inventoryId(10L).materialSpecId(1L).quantity(5).build();
            issueService.issue(1L, List.of(item));

            assertEquals(45, inv.getQuantityOnHand());
            assertEquals(IssueRequestStatus.ISSUED, request.getStatus());
        }
    }

    @Test
    void issue_insufficientInventory_throws() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class)) {
            scu.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");

            IssueRequest request = IssueRequest.builder().id(1L).status(IssueRequestStatus.APPROVED).build();
            when(issueRequestRepository.findById(1L)).thenReturn(Optional.of(request));

            Inventory inv = Inventory.builder().id(10L).quantityOnHand(2).build();
            when(inventoryRepository.findById(10L)).thenReturn(Optional.of(inv));

            IssueRecordRequest item = IssueRecordRequest.builder()
                    .inventoryId(10L).materialSpecId(1L).quantity(5).build();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> issueService.issue(1L, List.of(item)));
            assertEquals(ErrorCode.INSUFFICIENT_INVENTORY, ex.getErrorCode());
        }
    }

    @Test
    void reject_success() {
        IssueRequest request = IssueRequest.builder().id(1L).status(IssueRequestStatus.PENDING).build();
        when(issueRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(issueRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IssueRequestResponse resp = issueService.reject(1L);
        assertEquals(IssueRequestStatus.REJECTED, resp.getStatus());
    }
}
