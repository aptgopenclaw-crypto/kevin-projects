package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.material.dto.WarehouseRequest;
import com.taipei.iot.material.dto.WarehouseResponse;
import com.taipei.iot.material.entity.Warehouse;
import com.taipei.iot.material.enums.WarehouseStatus;
import com.taipei.iot.material.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @InjectMocks private WarehouseService warehouseService;
    @Mock private WarehouseRepository warehouseRepository;

    @Test
    void list_success() {
        Warehouse wh = Warehouse.builder().id(1L).warehouseCode("WH-01").warehouseName("主庫").status(WarehouseStatus.ACTIVE).build();
        when(warehouseRepository.findByFilters(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(wh)));

        Page<WarehouseResponse> result = warehouseService.list(null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("WH-01", result.getContent().get(0).getWarehouseCode());
    }

    @Test
    void create_success() {
        when(warehouseRepository.save(any())).thenAnswer(inv -> {
            Warehouse w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });

        WarehouseRequest req = WarehouseRequest.builder()
                .warehouseCode("WH-01").warehouseName("主庫").build();
        WarehouseResponse resp = warehouseService.create(req);

        assertNotNull(resp);
        assertEquals("WH-01", resp.getWarehouseCode());
        assertEquals(WarehouseStatus.ACTIVE, resp.getStatus());
    }

    @Test
    void update_success() {
        Warehouse existing = Warehouse.builder().id(1L).warehouseCode("WH-01").warehouseName("主庫").status(WarehouseStatus.ACTIVE).build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WarehouseRequest req = WarehouseRequest.builder()
                .warehouseCode("WH-01").warehouseName("更新名稱").build();
        WarehouseResponse resp = warehouseService.update(1L, req);

        assertEquals("更新名稱", resp.getWarehouseName());
    }

    @Test
    void delete_setsInactive() {
        Warehouse existing = Warehouse.builder().id(1L).status(WarehouseStatus.ACTIVE).build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        warehouseService.delete(1L);

        assertEquals(WarehouseStatus.INACTIVE, existing.getStatus());
    }

    @Test
    void findOrThrow_notFound() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> warehouseService.findOrThrow(99L));
        assertEquals(ErrorCode.WAREHOUSE_NOT_FOUND, ex.getErrorCode());
    }
}
