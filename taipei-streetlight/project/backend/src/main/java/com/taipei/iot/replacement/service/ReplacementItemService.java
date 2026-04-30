package com.taipei.iot.replacement.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.device.dto.DeviceResponse;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.material.entity.ApprovedMaterial;
import com.taipei.iot.material.enums.ApprovedMaterialStatus;
import com.taipei.iot.material.repository.ApprovedMaterialRepository;
import com.taipei.iot.replacement.dto.ReplacementItemRequest;
import com.taipei.iot.replacement.dto.ReplacementItemResponse;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.repository.ReplacementItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplacementItemService {

    private final ReplacementItemRepository itemRepo;
    private final ReplacementOrderService orderService;
    private final DeviceService deviceService;
    private final ApprovedMaterialRepository approvedMaterialRepo;

    public List<ReplacementItemResponse> getItems(Long orderId) {
        return itemRepo.findByOrderId(orderId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ReplacementItemResponse addItem(Long orderId, ReplacementItemRequest request) {
        var order = orderService.findOrThrow(orderId);
        if (order.getStatus() != ReplacementOrderStatus.DRAFT
                && order.getStatus() != ReplacementOrderStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.REPLACEMENT_INVALID_STATUS);
        }

        DeviceResponse parentDevice = deviceService.getById(request.getParentDeviceId());
        DeviceResponse oldDevice = deviceService.getById(request.getOldDeviceId());

        // 驗證舊設備屬於指定燈桿
        if (oldDevice.getParentDeviceId() != null
                && !request.getParentDeviceId().equals(oldDevice.getParentDeviceId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "舊設備不屬於指定燈桿");
        }

        // 材料管控：approved_material_id 必須存在且狀態為 ACTIVE
        if (request.getApprovedMaterialId() != null) {
            ApprovedMaterial am = approvedMaterialRepo.findById(request.getApprovedMaterialId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.APPROVED_MATERIAL_NOT_FOUND));
            if (am.getStatus() != ApprovedMaterialStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.MATERIAL_NOT_AVAILABLE);
            }
        }

        ReplacementItem item = ReplacementItem.builder()
                .orderId(orderId)
                .parentDeviceId(request.getParentDeviceId())
                .oldDeviceId(request.getOldDeviceId())
                .beforeDeviceType(oldDevice.getDeviceType() != null ? oldDevice.getDeviceType().name() : null)
                .beforeSpec(oldDevice.getAttributes())
                .afterDeviceType(request.getAfterDeviceType())
                .afterSpec(request.getAfterSpec())
                .materialSpecId(request.getMaterialSpecId())
                .approvedMaterialId(request.getApprovedMaterialId())
                .status(ReplacementItemStatus.PENDING)
                .build();

        return toResponse(itemRepo.save(item));
    }

    @Transactional
    public ReplacementItemResponse updateItem(Long orderId, Long itemId, ReplacementItemRequest request) {
        var order = orderService.findOrThrow(orderId);
        if (order.getStatus() != ReplacementOrderStatus.DRAFT
                && order.getStatus() != ReplacementOrderStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.REPLACEMENT_INVALID_STATUS);
        }

        ReplacementItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPLACEMENT_ORDER_NOT_FOUND));

        item.setParentDeviceId(request.getParentDeviceId());
        item.setOldDeviceId(request.getOldDeviceId());
        item.setAfterDeviceType(request.getAfterDeviceType());
        item.setAfterSpec(request.getAfterSpec());
        item.setMaterialSpecId(request.getMaterialSpecId());
        item.setApprovedMaterialId(request.getApprovedMaterialId());

        return toResponse(itemRepo.save(item));
    }

    @Transactional
    public void deleteItem(Long orderId, Long itemId) {
        var order = orderService.findOrThrow(orderId);
        if (order.getStatus() != ReplacementOrderStatus.DRAFT) {
            throw new BusinessException(ErrorCode.REPLACEMENT_INVALID_STATUS);
        }
        itemRepo.deleteById(itemId);
    }

    private ReplacementItemResponse toResponse(ReplacementItem item) {
        ReplacementItemResponse.ReplacementItemResponseBuilder builder = ReplacementItemResponse.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .parentDeviceId(item.getParentDeviceId())
                .oldDeviceId(item.getOldDeviceId())
                .newDeviceId(item.getNewDeviceId())
                .beforeDeviceType(item.getBeforeDeviceType())
                .beforeSpec(item.getBeforeSpec())
                .afterDeviceType(item.getAfterDeviceType())
                .afterSpec(item.getAfterSpec())
                .materialSpecId(item.getMaterialSpecId())
                .approvedMaterialId(item.getApprovedMaterialId())
                .status(item.getStatus())
                .completedAt(item.getCompletedAt())
                .completedBy(item.getCompletedBy())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt());

        // 填入 deviceCode 欄位
        try {
            builder.parentDeviceCode(deviceService.getById(item.getParentDeviceId()).getDeviceCode());
        } catch (Exception ignored) { /* device may not exist */ }
        try {
            builder.oldDeviceCode(deviceService.getById(item.getOldDeviceId()).getDeviceCode());
        } catch (Exception ignored) { /* device may not exist */ }
        if (item.getNewDeviceId() != null) {
            try {
                builder.newDeviceCode(deviceService.getById(item.getNewDeviceId()).getDeviceCode());
            } catch (Exception ignored) { /* device may not exist */ }
        }

        return builder.build();
    }
}
