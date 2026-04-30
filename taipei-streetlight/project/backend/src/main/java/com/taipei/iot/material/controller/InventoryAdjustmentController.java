package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.InventoryAdjustmentRequest;
import com.taipei.iot.material.dto.InventoryAdjustmentResponse;
import com.taipei.iot.material.enums.AdjustmentType;
import com.taipei.iot.material.service.InventoryAdjustmentService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth/material/adjustments")
@RequiredArgsConstructor
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService adjustmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public BaseResponse<PageResponse<InventoryAdjustmentResponse>> list(
            @RequestParam(required = false) AdjustmentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InventoryAdjustmentResponse> result = adjustmentService.list(type, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @PostMapping("/count")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public BaseResponse<InventoryAdjustmentResponse> count(@Valid @RequestBody InventoryAdjustmentRequest request) {
        return BaseResponse.success(adjustmentService.count(request));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public BaseResponse<Void> transfer(@Valid @RequestBody InventoryAdjustmentRequest request) {
        adjustmentService.transfer(request);
        return BaseResponse.success(null);
    }

    @PostMapping("/correction")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    public BaseResponse<InventoryAdjustmentResponse> correction(@Valid @RequestBody InventoryAdjustmentRequest request) {
        return BaseResponse.success(adjustmentService.correction(request));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
