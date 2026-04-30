package com.taipei.iot.material.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.material.dto.*;
import com.taipei.iot.material.entity.PurchaseItem;
import com.taipei.iot.material.entity.PurchaseOrder;
import com.taipei.iot.material.enums.PurchaseOrderStatus;
import com.taipei.iot.material.repository.PurchaseItemRepository;
import com.taipei.iot.material.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseItemRepository purchaseItemRepository;

    private final AtomicLong dailySequence = new AtomicLong(1);
    private volatile String lastDate = "";

    public Page<PurchaseOrderResponse> list(PurchaseOrderStatus status, String keyword, Pageable pageable) {
        return poRepository.findByFilters(status, keyword, pageable)
                .map(this::toResponse);
    }

    public PurchaseOrderResponse getById(Long id) {
        PurchaseOrder po = findOrThrow(id);
        PurchaseOrderResponse resp = toResponse(po);
        resp.setItems(purchaseItemRepository.findByPoId(id).stream()
                .map(this::toItemResponse).toList());
        return resp;
    }

    @Transactional
    @AuditEvent(AuditEventType.CREATE_PURCHASE_ORDER)
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePoNumber())
                .supplierId(request.getSupplierId())
                .contractId(request.getContractId())
                .orderDate(LocalDate.now())
                .status(PurchaseOrderStatus.DRAFT)
                .notes(request.getNotes())
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();

        PurchaseOrder saved = poRepository.save(po);

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : request.getItems()) {
            PurchaseItem item = PurchaseItem.builder()
                    .poId(saved.getId())
                    .materialSpecId(itemReq.getMaterialSpecId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .notes(itemReq.getNotes())
                    .build();
            purchaseItemRepository.save(item);
            if (itemReq.getUnitPrice() != null) {
                total = total.add(itemReq.getUnitPrice()
                        .multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            }
        }

        saved.setTotalAmount(total);
        return toResponse(poRepository.save(saved));
    }

    @Transactional
    public PurchaseOrderResponse update(Long id, PurchaseOrderRequest request) {
        PurchaseOrder po = findOrThrow(id);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "僅草稿狀態可編輯");
        }
        po.setSupplierId(request.getSupplierId());
        po.setContractId(request.getContractId());
        po.setNotes(request.getNotes());

        // 刪除舊明細，重建
        purchaseItemRepository.deleteAll(purchaseItemRepository.findByPoId(id));

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : request.getItems()) {
            PurchaseItem item = PurchaseItem.builder()
                    .poId(id)
                    .materialSpecId(itemReq.getMaterialSpecId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .notes(itemReq.getNotes())
                    .build();
            purchaseItemRepository.save(item);
            if (itemReq.getUnitPrice() != null) {
                total = total.add(itemReq.getUnitPrice()
                        .multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            }
        }
        po.setTotalAmount(total);
        return toResponse(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderResponse submit(Long id) {
        PurchaseOrder po = findOrThrow(id);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "僅草稿狀態可送審");
        }
        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        return toResponse(poRepository.save(po));
    }

    void updateStatusToReceiving(Long poId) {
        PurchaseOrder po = findOrThrow(poId);
        if (po.getStatus() == PurchaseOrderStatus.APPROVED || po.getStatus() == PurchaseOrderStatus.SUBMITTED) {
            po.setStatus(PurchaseOrderStatus.RECEIVING);
            poRepository.save(po);
        }
    }

    PurchaseOrder findOrThrow(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PURCHASE_ORDER_NOT_FOUND));
    }

    private synchronized String generatePoNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(1);
        }
        return String.format("PO-%s-%03d", today, dailySequence.getAndIncrement());
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplierId())
                .supplierName(po.getSupplier() != null ? po.getSupplier().getSupplierName() : null)
                .contractId(po.getContractId())
                .orderDate(po.getOrderDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .notes(po.getNotes())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private PurchaseItemResponse toItemResponse(PurchaseItem item) {
        return PurchaseItemResponse.builder()
                .id(item.getId())
                .materialSpecId(item.getMaterialSpecId())
                .specCode(item.getMaterialSpec() != null ? item.getMaterialSpec().getSpecCode() : null)
                .specName(item.getMaterialSpec() != null ? item.getMaterialSpec().getSpecName() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .notes(item.getNotes())
                .build();
    }
}
