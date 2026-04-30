package com.taipei.iot.material.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.material.dto.ReceivingRequest;
import com.taipei.iot.material.dto.ReceivingResponse;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.entity.ReceivingRecord;
import com.taipei.iot.material.repository.InventoryRepository;
import com.taipei.iot.material.repository.ReceivingRecordRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceivingService {

    private final ReceivingRecordRepository receivingRecordRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderService purchaseOrderService;

    public Page<ReceivingResponse> list(Long poId, Pageable pageable) {
        return receivingRecordRepository.findByFilters(poId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    @AuditEvent(AuditEventType.RECEIVE_MATERIAL)
    public ReceivingResponse receive(ReceivingRequest request) {
        ReceivingRecord record = ReceivingRecord.builder()
                .poId(request.getPoId())
                .warehouseId(request.getWarehouseId())
                .materialSpecId(request.getMaterialSpecId())
                .quantity(request.getQuantity())
                .receivedDate(LocalDate.now())
                .deliveryNote(request.getDeliveryNote())
                .receivedBy(SecurityContextUtils.getCurrentUsername())
                .build();
        ReceivingRecord saved = receivingRecordRepository.save(record);

        // UPSERT inventory
        String tenantId = TenantContext.getCurrentTenantId();
        Inventory inventory = inventoryRepository
                .findByTenantAndWarehouseAndSpec(tenantId, request.getWarehouseId(), request.getMaterialSpecId())
                .orElseGet(() -> {
                    Inventory inv = Inventory.builder()
                            .warehouseId(request.getWarehouseId())
                            .materialSpecId(request.getMaterialSpecId())
                            .quantityOnHand(0)
                            .safetyStock(0)
                            .build();
                    inv.setTenantId(tenantId);
                    return inv;
                });

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + request.getQuantity());
        inventoryRepository.save(inventory);

        // Update PO status
        if (request.getPoId() != null) {
            purchaseOrderService.updateStatusToReceiving(request.getPoId());
        }

        return toResponse(saved);
    }

    private ReceivingResponse toResponse(ReceivingRecord r) {
        return ReceivingResponse.builder()
                .id(r.getId())
                .poId(r.getPoId())
                .warehouseId(r.getWarehouseId())
                .materialSpecId(r.getMaterialSpecId())
                .quantity(r.getQuantity())
                .receivedDate(r.getReceivedDate())
                .deliveryNote(r.getDeliveryNote())
                .receivedBy(r.getReceivedBy())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
