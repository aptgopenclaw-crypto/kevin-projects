package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.PurchaseOrderRequest;
import com.taipei.iot.material.dto.PurchaseOrderResponse;
import com.taipei.iot.material.enums.PurchaseOrderStatus;
import com.taipei.iot.material.service.PurchaseOrderService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth/material/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PageResponse<PurchaseOrderResponse>> list(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PurchaseOrderResponse> result = purchaseOrderService.list(status, keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PurchaseOrderResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(purchaseOrderService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest request) {
        return BaseResponse.success(purchaseOrderService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<PurchaseOrderResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody PurchaseOrderRequest request) {
        return BaseResponse.success(purchaseOrderService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<PurchaseOrderResponse> submit(@PathVariable Long id) {
        return BaseResponse.success(purchaseOrderService.submit(id));
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
