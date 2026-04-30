package com.taipei.iot.material.service;

import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.material.dto.ReceivingRequest;
import com.taipei.iot.material.dto.ReceivingResponse;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.entity.ReceivingRecord;
import com.taipei.iot.material.repository.InventoryRepository;
import com.taipei.iot.material.repository.ReceivingRecordRepository;
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
class ReceivingServiceTest {

    @InjectMocks private ReceivingService receivingService;
    @Mock private ReceivingRecordRepository receivingRecordRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private PurchaseOrderService purchaseOrderService;

    @Test
    void receive_existingInventory_updatesQuantity() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class);
             MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            scu.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");
            tc.when(TenantContext::getCurrentTenantId).thenReturn("T1");

            Inventory existing = Inventory.builder().id(1L).quantityOnHand(10).build();
            when(inventoryRepository.findByTenantAndWarehouseAndSpec("T1", 1L, 2L))
                    .thenReturn(Optional.of(existing));
            when(receivingRecordRepository.save(any())).thenAnswer(inv -> {
                ReceivingRecord r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReceivingRequest req = ReceivingRequest.builder()
                    .poId(1L).warehouseId(1L).materialSpecId(2L).quantity(20).build();
            ReceivingResponse resp = receivingService.receive(req);

            assertNotNull(resp);
            assertEquals(30, existing.getQuantityOnHand());
            verify(purchaseOrderService).updateStatusToReceiving(1L);
        }
    }

    @Test
    void receive_newInventory_createsRecord() {
        try (MockedStatic<SecurityContextUtils> scu = mockStatic(SecurityContextUtils.class);
             MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            scu.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");
            tc.when(TenantContext::getCurrentTenantId).thenReturn("T1");

            when(inventoryRepository.findByTenantAndWarehouseAndSpec("T1", 1L, 2L))
                    .thenReturn(Optional.empty());
            when(receivingRecordRepository.save(any())).thenAnswer(inv -> {
                ReceivingRecord r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReceivingRequest req = ReceivingRequest.builder()
                    .warehouseId(1L).materialSpecId(2L).quantity(15).build();
            ReceivingResponse resp = receivingService.receive(req);

            assertNotNull(resp);
            verify(inventoryRepository).save(argThat(i -> i.getQuantityOnHand() == 15));
        }
    }
}
