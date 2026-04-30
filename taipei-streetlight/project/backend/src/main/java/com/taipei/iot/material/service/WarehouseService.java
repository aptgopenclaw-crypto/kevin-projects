package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.material.dto.WarehouseRequest;
import com.taipei.iot.material.dto.WarehouseResponse;
import com.taipei.iot.material.entity.Warehouse;
import com.taipei.iot.material.enums.WarehouseStatus;
import com.taipei.iot.material.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public Page<WarehouseResponse> list(WarehouseStatus status, String keyword, Pageable pageable) {
        return warehouseRepository.findByFilters(status, keyword, pageable)
                .map(this::toResponse);
    }

    public WarehouseResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<WarehouseResponse> listActive() {
        return warehouseRepository.findByStatus(WarehouseStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        Warehouse warehouse = Warehouse.builder()
                .warehouseCode(request.getWarehouseCode())
                .warehouseName(request.getWarehouseName())
                .location(request.getLocation())
                .status(request.getStatus() != null ? request.getStatus() : WarehouseStatus.ACTIVE)
                .build();
        return toResponse(warehouseRepository.save(warehouse));
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Warehouse warehouse = findOrThrow(id);
        warehouse.setWarehouseName(request.getWarehouseName());
        warehouse.setLocation(request.getLocation());
        if (request.getStatus() != null) {
            warehouse.setStatus(request.getStatus());
        }
        return toResponse(warehouseRepository.save(warehouse));
    }

    @Transactional
    public void delete(Long id) {
        Warehouse warehouse = findOrThrow(id);
        warehouse.setStatus(WarehouseStatus.INACTIVE);
        warehouseRepository.save(warehouse);
    }

    Warehouse findOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));
    }

    private WarehouseResponse toResponse(Warehouse w) {
        return WarehouseResponse.builder()
                .id(w.getId())
                .warehouseCode(w.getWarehouseCode())
                .warehouseName(w.getWarehouseName())
                .location(w.getLocation())
                .status(w.getStatus())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
