package com.taipei.iot.material.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.material.dto.InventoryResponse;
import com.taipei.iot.material.dto.InventorySummaryResponse;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.material.event.LowStockAlertEvent;
import com.taipei.iot.material.repository.InventoryRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Page<InventoryResponse> list(Long warehouseId, MaterialCategory category,
                                         String keyword, boolean belowSafetyStock,
                                         Pageable pageable) {
        return inventoryRepository.findByFilters(warehouseId, category, keyword, belowSafetyStock, pageable)
                .map(this::toResponse);
    }

    public List<InventoryResponse> findBelowSafetyStock() {
        return inventoryRepository.findBelowSafetyStock()
                .stream().map(this::toResponse).toList();
    }

    public List<InventorySummaryResponse> summarize() {
        return inventoryRepository.summarizeByCategory().stream()
                .map(row -> InventorySummaryResponse.builder()
                        .category((MaterialCategory) row[0])
                        .itemCount((Long) row[1])
                        .totalQuantity((Long) row[2])
                        .build())
                .toList();
    }

    Inventory findOrThrow(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void checkSafetyStock() {
        TenantContext.setSystemContext();
        try {
            List<Inventory> alerts = inventoryRepository.findBelowSafetyStockAllTenants();
            for (Inventory inv : alerts) {
                eventPublisher.publishEvent(new LowStockAlertEvent(
                        inv.getTenantId(),
                        inv.getMaterialSpec().getSpecName(),
                        inv.getWarehouse().getWarehouseName(),
                        inv.getQuantityOnHand(),
                        inv.getSafetyStock()
                ));
            }
        } finally {
            TenantContext.clear();
        }
    }

    InventoryResponse toResponse(Inventory inv) {
        return InventoryResponse.builder()
                .id(inv.getId())
                .warehouseId(inv.getWarehouseId())
                .warehouseName(inv.getWarehouse() != null ? inv.getWarehouse().getWarehouseName() : null)
                .materialSpecId(inv.getMaterialSpecId())
                .specCode(inv.getMaterialSpec() != null ? inv.getMaterialSpec().getSpecCode() : null)
                .specName(inv.getMaterialSpec() != null ? inv.getMaterialSpec().getSpecName() : null)
                .category(inv.getMaterialSpec() != null ? inv.getMaterialSpec().getCategory() : null)
                .quantityOnHand(inv.getQuantityOnHand())
                .safetyStock(inv.getSafetyStock())
                .belowSafetyStock(inv.getSafetyStock() > 0 && inv.getQuantityOnHand() < inv.getSafetyStock())
                .build();
    }
}
