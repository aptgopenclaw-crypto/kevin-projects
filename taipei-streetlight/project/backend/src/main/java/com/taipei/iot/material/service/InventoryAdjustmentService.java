package com.taipei.iot.material.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryAdjustmentService {

    private final InventoryAdjustmentRepository adjustmentRepository;
    private final InventoryRepository inventoryRepository;

    public Page<InventoryAdjustmentResponse> list(AdjustmentType type, Pageable pageable) {
        return adjustmentRepository.findByFilters(type, pageable)
                .map(this::toResponse);
    }

    @Transactional
    @AuditEvent(AuditEventType.ADJUST_INVENTORY)
    public InventoryAdjustmentResponse count(InventoryAdjustmentRequest request) {
        Inventory inventory = findInventoryOrThrow(request.getInventoryId());
        int diff = request.getActualQuantity() - inventory.getQuantityOnHand();

        InventoryAdjustment adj = InventoryAdjustment.builder()
                .inventoryId(request.getInventoryId())
                .adjustmentType(AdjustmentType.COUNT)
                .quantityChange(diff)
                .reason(request.getReason())
                .adjustedBy(SecurityContextUtils.getCurrentUsername())
                .adjustedAt(LocalDateTime.now())
                .build();
        InventoryAdjustment saved = adjustmentRepository.save(adj);

        inventory.setQuantityOnHand(request.getActualQuantity());
        inventoryRepository.save(inventory);

        return toResponse(saved);
    }

    @Transactional
    @AuditEvent(AuditEventType.ADJUST_INVENTORY)
    public void transfer(InventoryAdjustmentRequest request) {
        Inventory from = findInventoryOrThrow(request.getInventoryId());
        if (from.getQuantityOnHand() < request.getQuantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY);
        }

        String username = SecurityContextUtils.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        // 出庫
        from.setQuantityOnHand(from.getQuantityOnHand() - request.getQuantity());
        inventoryRepository.save(from);
        adjustmentRepository.save(InventoryAdjustment.builder()
                .inventoryId(request.getInventoryId())
                .adjustmentType(AdjustmentType.TRANSFER)
                .quantityChange(-request.getQuantity())
                .reason(request.getReason())
                .adjustedBy(username)
                .adjustedAt(now)
                .build());

        // 入庫（UPSERT）
        String tenantId = TenantContext.getCurrentTenantId();
        Inventory to = inventoryRepository
                .findByTenantAndWarehouseAndSpec(tenantId, request.getToWarehouseId(), from.getMaterialSpecId())
                .orElseGet(() -> {
                    Inventory inv = Inventory.builder()
                            .warehouseId(request.getToWarehouseId())
                            .materialSpecId(from.getMaterialSpecId())
                            .quantityOnHand(0)
                            .safetyStock(0)
                            .build();
                    inv.setTenantId(tenantId);
                    return inv;
                });
        to.setQuantityOnHand(to.getQuantityOnHand() + request.getQuantity());
        Inventory savedTo = inventoryRepository.save(to);
        adjustmentRepository.save(InventoryAdjustment.builder()
                .inventoryId(savedTo.getId())
                .adjustmentType(AdjustmentType.TRANSFER)
                .quantityChange(request.getQuantity())
                .reason(request.getReason())
                .adjustedBy(username)
                .adjustedAt(now)
                .build());
    }

    @Transactional
    @AuditEvent(AuditEventType.ADJUST_INVENTORY)
    public InventoryAdjustmentResponse correction(InventoryAdjustmentRequest request) {
        Inventory inventory = findInventoryOrThrow(request.getInventoryId());

        InventoryAdjustment adj = InventoryAdjustment.builder()
                .inventoryId(request.getInventoryId())
                .adjustmentType(AdjustmentType.CORRECTION)
                .quantityChange(request.getQuantity())
                .reason(request.getReason())
                .adjustedBy(SecurityContextUtils.getCurrentUsername())
                .adjustedAt(LocalDateTime.now())
                .build();
        InventoryAdjustment saved = adjustmentRepository.save(adj);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + request.getQuantity());
        inventoryRepository.save(inventory);

        return toResponse(saved);
    }

    private Inventory findInventoryOrThrow(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    private InventoryAdjustmentResponse toResponse(InventoryAdjustment a) {
        return InventoryAdjustmentResponse.builder()
                .id(a.getId())
                .inventoryId(a.getInventoryId())
                .adjustmentType(a.getAdjustmentType())
                .quantityChange(a.getQuantityChange())
                .reason(a.getReason())
                .adjustedBy(a.getAdjustedBy())
                .adjustedAt(a.getAdjustedAt())
                .build();
    }
}
