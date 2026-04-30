package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.InventoryResponse;
import com.taipei.iot.material.dto.InventorySummaryResponse;
import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.material.service.InventoryService;
import com.taipei.iot.user.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/material/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public BaseResponse<PageResponse<InventoryResponse>> list(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) MaterialCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean belowSafetyStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InventoryResponse> result = inventoryService.list(
                warehouseId, category, keyword, belowSafetyStock, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public BaseResponse<List<InventorySummaryResponse>> summary() {
        return BaseResponse.success(inventoryService.summarize());
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public BaseResponse<List<InventoryResponse>> alerts() {
        return BaseResponse.success(inventoryService.findBelowSafetyStock());
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
