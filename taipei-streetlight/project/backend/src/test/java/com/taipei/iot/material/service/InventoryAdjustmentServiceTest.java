package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.material.dto.InventoryAdjustmentRequest;
import com.taipei.iot.material.dto.InventoryAdjustmentResponse;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.entity.InventoryAdjustment;
import com.taipei.iot.material.enums.AdjustmentType;
import com.taipei.iot.material.repository.InventoryAdjustmentRepository;
import com.taipei.iot.material.repository.InventoryRepository;
import com.taipei.iot.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentServiceTest {

    @InjectMocks private InventoryAdjustmentService adjustmentService;
    @Mock private InventoryAdjustmentRepository adjustmentRepository;
    @Mock private InventoryRepository inventoryRepository;

    @Test
    void count_success() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class)) {
            scu.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");

            Inventory inv = Inventory.builder().id(1L).quantityOnHand(20).build();
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inv));
            when(adjustmentRepository.save(any())).thenAnswer(i -> {
                InventoryAdjustment a = i.getArgument(0);
                a.setId(1L);
                return a;
            });
            when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InventoryAdjustmentRequest req = InventoryAdjustmentRequest.builder()
                    .inventoryId(1L).actualQuantity(25).reason("盤點").build();
            InventoryAdjustmentResponse resp = adjustmentService.count(req);

            assertEquals(5, resp.getQuantityChange());
            assertEquals(AdjustmentType.COUNT, resp.getAdjustmentType());
            assertEquals(25, inv.getQuantityOnHand());
        }
    }

    @Test
    void transfer_insufficientInventory_throws() {
        Inventory from = Inventory.builder().id(1L).quantityOnHand(3).build();
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(from));

        InventoryAdjustmentRequest req = InventoryAdjustmentRequest.builder()
                .inventoryId(1L).toWarehouseId(2L).quantity(10).build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adjustmentService.transfer(req));
        assertEquals(ErrorCode.INSUFFICIENT_INVENTORY, ex.getErrorCode());
    }

    @Test
    void transfer_success() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class);
             MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            scu.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");
            tc.when(TenantContext::getCurrentTenantId).thenReturn("T1");

            Inventory from = Inventory.builder().id(1L).materialSpecId(10L).quantityOnHand(50).build();
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(from));

            Inventory to = Inventory.builder().id(2L).warehouseId(2L).materialSpecId(10L).quantityOnHand(10).build();
            when(inventoryRepository.findByTenantAndWarehouseAndSpec("T1", 2L, 10L))
                    .thenReturn(Optional.of(to));
            when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(adjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InventoryAdjustmentRequest req = InventoryAdjustmentRequest.builder()
                    .inventoryId(1L).toWarehouseId(2L).quantity(15).reason("轉庫").build();
            adjustmentService.transfer(req);

            assertEquals(35, from.getQuantityOnHand());
            assertEquals(25, to.getQuantityOnHand());
            verify(adjustmentRepository, times(2)).save(any());
        }
    }

    @Test
    void correction_success() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class)) {
            scu.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");

            Inventory inv = Inventory.builder().id(1L).quantityOnHand(20).build();
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inv));
            when(adjustmentRepository.save(any())).thenAnswer(i -> {
                InventoryAdjustment a = i.getArgument(0);
                a.setId(1L);
                return a;
            });
            when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            InventoryAdjustmentRequest req = InventoryAdjustmentRequest.builder()
                    .inventoryId(1L).quantity(-3).reason("修正").build();
            InventoryAdjustmentResponse resp = adjustmentService.correction(req);

            assertEquals(-3, resp.getQuantityChange());
            assertEquals(17, inv.getQuantityOnHand());
        }
    }
}
