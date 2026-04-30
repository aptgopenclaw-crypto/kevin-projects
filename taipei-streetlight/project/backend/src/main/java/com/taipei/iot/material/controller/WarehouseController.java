package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.WarehouseRequest;
import com.taipei.iot.material.dto.WarehouseResponse;
import com.taipei.iot.material.enums.WarehouseStatus;
import com.taipei.iot.material.service.WarehouseService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/material/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PageResponse<WarehouseResponse>> list(
            @RequestParam(required = false) WarehouseStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<WarehouseResponse> result = warehouseService.list(status, keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<List<WarehouseResponse>> listActive() {
        return BaseResponse.success(warehouseService.listActive());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
        return BaseResponse.success(warehouseService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<WarehouseResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody WarehouseRequest request) {
        return BaseResponse.success(warehouseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return BaseResponse.success(null);
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
