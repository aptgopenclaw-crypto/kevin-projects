package com.taipei.iot.replacement.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.dto.ComponentReplaceRequest;
import com.taipei.iot.device.dto.DeviceResponse;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.replacement.dto.ReplacementOrderQueryParams;
import com.taipei.iot.replacement.dto.ReplacementOrderRequest;
import com.taipei.iot.replacement.dto.ReplacementOrderResponse;
import com.taipei.iot.replacement.dto.SelfCheckItemRequest;
import com.taipei.iot.replacement.dto.SelfCheckRequest;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderType;
import com.taipei.iot.replacement.repository.ReplacementItemRepository;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
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
class ReplacementOrderServiceTest {

    @InjectMocks private ReplacementOrderService service;
    @Mock private ReplacementOrderRepository repo;
    @Mock private ReplacementItemRepository itemRepo;
    @Mock private RepairTicketRepository repairTicketRepo;
    @Mock private DeviceService deviceService;
    @Mock private DeviceEventService deviceEventService;
    @Mock private WorkflowService workflowService;
    @Mock private DataScopeHelper dataScopeHelper;

    // ── createFromRepair ──

    @Test
    void createFromRepair_success() {
        try (MockedStatic<SecurityContextUtils> utils = mockStatic(SecurityContextUtils.class)) {
            utils.when(SecurityContextUtils::getCurrentUserId).thenReturn("user-001");

            RepairTicket repair = RepairTicket.builder()
                    .id(1L).contractId(10L).deptId(5L).build();
            when(repairTicketRepo.findById(1L)).thenReturn(Optional.of(repair));
            when(repo.save(any())).thenAnswer(inv -> {
                ReplacementOrder o = inv.getArgument(0);
                o.setId(100L);
                return o;
            });
            when(workflowService.createInstance(anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(1L);

            ReplacementOrderRequest request = ReplacementOrderRequest.builder()
                    .orderType(ReplacementOrderType.REPLACEMENT)
                    .dispatchReason("燈具老化")
                    .build();

            ReplacementOrderResponse result = service.createFromRepair(1L, request);

            assertNotNull(result);
            assertEquals(ReplacementOrderStatus.DRAFT, result.getStatus());
            assertEquals(1L, result.getRepairTicketId());
            verify(workflowService).createInstance("REPLACEMENT_REVIEW", "REPLACEMENT_ORDER", 100L, "user-001");
        }
    }

    @Test
    void createFromRepair_repairNotFound_throwsException() {
        when(repairTicketRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> service.createFromRepair(99L, ReplacementOrderRequest.builder().build()));
    }

    // ── createDirect ──

    @Test
    void createDirect_success() {
        try (MockedStatic<SecurityContextUtils> utils = mockStatic(SecurityContextUtils.class)) {
            utils.when(SecurityContextUtils::getCurrentUserId).thenReturn("user-001");

            when(repo.save(any())).thenAnswer(inv -> {
                ReplacementOrder o = inv.getArgument(0);
                o.setId(200L);
                return o;
            });
            when(workflowService.createInstance(anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(2L);

            ReplacementOrderRequest request = ReplacementOrderRequest.builder()
                    .orderType(ReplacementOrderType.NEW_INSTALL)
                    .location("信義區")
                    .build();

            ReplacementOrderResponse result = service.createDirect(request);

            assertNotNull(result);
            assertEquals(ReplacementOrderStatus.DRAFT, result.getStatus());
            assertEquals(ReplacementOrderType.NEW_INSTALL, result.getOrderType());
        }
    }

    // ── dispatch ──

    @Test
    void dispatch_success() {
        try (MockedStatic<SecurityContextUtils> utils = mockStatic(SecurityContextUtils.class)) {
            utils.when(SecurityContextUtils::getCurrentUserId).thenReturn("user-001");
            utils.when(SecurityContextUtils::getCurrentUsername).thenReturn("Admin");

            ReplacementOrder order = ReplacementOrder.builder()
                    .id(1L).status(ReplacementOrderStatus.DRAFT).build();
            when(repo.findById(1L)).thenReturn(Optional.of(order));
            when(repo.save(any())).thenReturn(order);

            WorkflowInstance wfInstance = new WorkflowInstance();
            wfInstance.setId(10L);
            when(workflowService.findByTicket("REPLACEMENT_ORDER", 1L)).thenReturn(wfInstance);

            ReplacementOrderRequest request = ReplacementOrderRequest.builder()
                    .assignedContractor("承商A")
                    .build();
            service.dispatch(1L, request);

            assertEquals(ReplacementOrderStatus.DISPATCHED, order.getStatus());
            verify(workflowService).transition(eq(10L), eq("DISPATCHED"), eq("DISPATCH"),
                    eq("user-001"), eq("Admin"), eq("派工至承商"), isNull());
        }
    }

    @Test
    void dispatch_invalidStatus_throwsException() {
        ReplacementOrder order = ReplacementOrder.builder()
                .id(1L).status(ReplacementOrderStatus.IN_PROGRESS).build();
        when(repo.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class,
                () -> service.dispatch(1L, ReplacementOrderRequest.builder().build()));
    }

    // ── startWork ──

    @Test
    void startWork_success() {
        try (MockedStatic<SecurityContextUtils> utils = mockStatic(SecurityContextUtils.class)) {
            utils.when(SecurityContextUtils::getCurrentUserId).thenReturn("user-001");
            utils.when(SecurityContextUtils::getCurrentUsername).thenReturn("Admin");

            ReplacementOrder order = ReplacementOrder.builder()
                    .id(1L).status(ReplacementOrderStatus.DISPATCHED).build();
            when(repo.findById(1L)).thenReturn(Optional.of(order));
            when(repo.save(any())).thenReturn(order);

            WorkflowInstance wfInstance = new WorkflowInstance();
            wfInstance.setId(10L);
            when(workflowService.findByTicket("REPLACEMENT_ORDER", 1L)).thenReturn(wfInstance);

            service.startWork(1L);

            assertEquals(ReplacementOrderStatus.IN_PROGRESS, order.getStatus());
        }
    }

    // ── selfCheck ──

    @Test
    void selfCheck_success() {
        try (MockedStatic<SecurityContextUtils> utils = mockStatic(SecurityContextUtils.class)) {
            utils.when(SecurityContextUtils::getCurrentUserId).thenReturn("user-001");
            utils.when(SecurityContextUtils::getCurrentUsername).thenReturn("Contractor");

            ReplacementOrder order = ReplacementOrder.builder()
                    .id(1L).orderNumber("RO-20240101-001")
                    .status(ReplacementOrderStatus.IN_PROGRESS).build();
            when(repo.findById(1L)).thenReturn(Optional.of(order));
            when(repo.save(any())).thenReturn(order);

            ReplacementItem item = ReplacementItem.builder()
                    .id(10L).parentDeviceId(100L).oldDeviceId(200L)
                    .beforeDeviceType("LUMINAIRE").build();
            when(itemRepo.findById(10L)).thenReturn(Optional.of(item));
            when(itemRepo.save(any())).thenReturn(item);

            DeviceResponse newDeviceResp = DeviceResponse.builder()
                    .id(300L).build();
            when(deviceService.replaceComponent(eq(100L), any(ComponentReplaceRequest.class), eq(deviceEventService)))
                    .thenReturn(newDeviceResp);

            WorkflowInstance wfInstance = new WorkflowInstance();
            wfInstance.setId(10L);
            when(workflowService.findByTicket("REPLACEMENT_ORDER", 1L)).thenReturn(wfInstance);

            SelfCheckItemRequest itemCheck = SelfCheckItemRequest.builder()
                    .itemId(10L).deviceCode("LED-NEW-001").notes("更換完成").build();
            SelfCheckRequest request = SelfCheckRequest.builder()
                    .items(List.of(itemCheck)).build();

            service.selfCheck(1L, request);

            assertEquals(ReplacementOrderStatus.SELF_CHECKED, order.getStatus());
            assertEquals(300L, item.getNewDeviceId());
            assertEquals(ReplacementItemStatus.COMPLETED, item.getStatus());
        }
    }

    // ── approve ──

    @Test
    void approve_success() {
        try (MockedStatic<SecurityContextUtils> utils = mockStatic(SecurityContextUtils.class)) {
            utils.when(SecurityContextUtils::getCurrentUserId).thenReturn("user-001");
            utils.when(SecurityContextUtils::getCurrentUsername).thenReturn("Admin");

            ReplacementOrder order = ReplacementOrder.builder()
                    .id(1L).status(ReplacementOrderStatus.PENDING_REVIEW).build();
            when(repo.findById(1L)).thenReturn(Optional.of(order));

            WorkflowInstance wfInstance = new WorkflowInstance();
            wfInstance.setId(10L);
            when(workflowService.findByTicket("REPLACEMENT_ORDER", 1L)).thenReturn(wfInstance);

            service.approve(1L, "審核通過");

            verify(workflowService).transition(eq(10L), eq("CLOSED"), eq("APPROVE"),
                    anyString(), anyString(), eq("審核通過"), isNull());
        }
    }

    // ── returnOrder ──

    @Test
    void returnOrder_success() {
        try (MockedStatic<SecurityContextUtils> utils = mockStatic(SecurityContextUtils.class)) {
            utils.when(SecurityContextUtils::getCurrentUserId).thenReturn("user-001");
            utils.when(SecurityContextUtils::getCurrentUsername).thenReturn("Admin");

            ReplacementOrder order = ReplacementOrder.builder()
                    .id(1L).status(ReplacementOrderStatus.PENDING_REVIEW).build();
            when(repo.findById(1L)).thenReturn(Optional.of(order));
            when(repo.save(any())).thenReturn(order);

            WorkflowInstance wfInstance = new WorkflowInstance();
            wfInstance.setId(10L);
            when(workflowService.findByTicket("REPLACEMENT_ORDER", 1L)).thenReturn(wfInstance);

            service.returnOrder(1L, "需補件");

            assertEquals(ReplacementOrderStatus.RETURNED, order.getStatus());
        }
    }

    // ── list ──

    @Test
    void list_success() {
        when(dataScopeHelper.getVisibleDeptIds()).thenReturn(List.of(1L, 2L));
        Page<ReplacementOrder> page = new PageImpl<>(List.of(
                ReplacementOrder.builder().id(1L).orderType(ReplacementOrderType.REPLACEMENT)
                        .status(ReplacementOrderStatus.DRAFT).build()
        ));
        when(repo.findByFilters(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        ReplacementOrderQueryParams params = ReplacementOrderQueryParams.builder().build();
        Page<ReplacementOrderResponse> result = service.list(params, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    // ── getById: not found ──

    @Test
    void getById_notFound_throwsException() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getById(999L));
        assertEquals(ErrorCode.REPLACEMENT_ORDER_NOT_FOUND, ex.getErrorCode());
    }
}
