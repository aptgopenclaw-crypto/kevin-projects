package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.material.dto.InventoryResponse;
import com.taipei.iot.material.dto.InventorySummaryResponse;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.entity.MaterialSpec;
import com.taipei.iot.material.entity.Warehouse;
import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.material.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks private InventoryService inventoryService;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private Inventory buildInventory(int onHand, int safety) {
        Warehouse wh = Warehouse.builder().id(1L).warehouseName("主庫").build();
        MaterialSpec spec = MaterialSpec.builder().id(1L).specCode("M-001").specName("LED燈具").category(MaterialCategory.LUMINAIRE).build();

        Inventory inv = Inventory.builder()
                .id(1L).warehouseId(1L).materialSpecId(1L)
                .quantityOnHand(onHand).safetyStock(safety).build();
        inv.setWarehouse(wh);
        inv.setMaterialSpec(spec);
        return inv;
    }

    @Test
    void list_success() {
        Inventory inv = buildInventory(25, 10);
        when(inventoryRepository.findByFilters(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(inv)));

        Page<InventoryResponse> result = inventoryService.list(null, null, null, false, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).isBelowSafetyStock());
    }

    @Test
    void findBelowSafetyStock_returnsAlerts() {
        Inventory inv = buildInventory(3, 10);
        when(inventoryRepository.findBelowSafetyStock()).thenReturn(List.of(inv));

        List<InventoryResponse> alerts = inventoryService.findBelowSafetyStock();

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).isBelowSafetyStock());
    }

    @Test
    @SuppressWarnings("unchecked")
    void summarize_success() {
        Object[] row = new Object[]{ MaterialCategory.LUMINAIRE, 5L, 100L };
        List<Object[]> rows = java.util.Collections.singletonList(row);
        when(inventoryRepository.summarizeByCategory()).thenReturn(rows);

        List<InventorySummaryResponse> result = inventoryService.summarize();

        assertEquals(1, result.size());
        assertEquals(MaterialCategory.LUMINAIRE, result.get(0).getCategory());
        assertEquals(5, result.get(0).getItemCount());
    }

    @Test
    void findOrThrow_notFound() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> inventoryService.findOrThrow(99L));
    }
}
