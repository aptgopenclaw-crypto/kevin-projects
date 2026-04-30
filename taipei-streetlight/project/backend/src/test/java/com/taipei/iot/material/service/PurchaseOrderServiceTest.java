package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.material.dto.PurchaseItemRequest;
import com.taipei.iot.material.dto.PurchaseOrderRequest;
import com.taipei.iot.material.dto.PurchaseOrderResponse;
import com.taipei.iot.material.entity.PurchaseItem;
import com.taipei.iot.material.entity.PurchaseOrder;
import com.taipei.iot.material.enums.PurchaseOrderStatus;
import com.taipei.iot.material.repository.PurchaseItemRepository;
import com.taipei.iot.material.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @InjectMocks private PurchaseOrderService purchaseOrderService;
    @Mock private PurchaseOrderRepository poRepository;
    @Mock private PurchaseItemRepository purchaseItemRepository;

    @Test
    void create_success() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class)) {
            scu.when(SecurityContextUtils::getCurrentUserId).thenReturn("admin");

            when(poRepository.save(any())).thenAnswer(inv -> {
                PurchaseOrder po = inv.getArgument(0);
                po.setId(1L);
                return po;
            });
            when(purchaseItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PurchaseItemRequest item = PurchaseItemRequest.builder()
                    .materialSpecId(1L).quantity(10).unitPrice(BigDecimal.valueOf(100)).build();
            PurchaseOrderRequest req = PurchaseOrderRequest.builder()
                    .supplierId(1L).items(List.of(item)).build();

            PurchaseOrderResponse resp = purchaseOrderService.create(req);

            assertNotNull(resp);
            assertEquals(PurchaseOrderStatus.DRAFT, resp.getStatus());
            assertNotNull(resp.getPoNumber());
            verify(purchaseItemRepository).save(any());
        }
    }

    @Test
    void submit_success() {
        PurchaseOrder po = PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.DRAFT).build();
        when(poRepository.findById(1L)).thenReturn(Optional.of(po));
        when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderResponse resp = purchaseOrderService.submit(1L);

        assertEquals(PurchaseOrderStatus.SUBMITTED, resp.getStatus());
    }

    @Test
    void submit_nonDraft_throws() {
        PurchaseOrder po = PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.SUBMITTED).build();
        when(poRepository.findById(1L)).thenReturn(Optional.of(po));

        assertThrows(BusinessException.class,
                () -> purchaseOrderService.submit(1L));
    }

    @Test
    void update_nonDraft_throws() {
        PurchaseOrder po = PurchaseOrder.builder().id(1L).status(PurchaseOrderStatus.APPROVED).build();
        when(poRepository.findById(1L)).thenReturn(Optional.of(po));

        PurchaseOrderRequest req = PurchaseOrderRequest.builder().supplierId(1L).items(Collections.emptyList()).build();
        assertThrows(BusinessException.class,
                () -> purchaseOrderService.update(1L, req));
    }

    @Test
    void findOrThrow_notFound() {
        when(poRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> purchaseOrderService.findOrThrow(99L));
        assertEquals(ErrorCode.PURCHASE_ORDER_NOT_FOUND, ex.getErrorCode());
    }
}
