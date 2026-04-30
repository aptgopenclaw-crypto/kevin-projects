package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.SupplierRequest;
import com.taipei.iot.material.dto.SupplierResponse;
import com.taipei.iot.material.enums.SupplierStatus;
import com.taipei.iot.material.service.SupplierService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/material/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PageResponse<SupplierResponse>> list(
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SupplierResponse> result = supplierService.list(status, keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<List<SupplierResponse>> listActive() {
        return BaseResponse.success(supplierService.listActive());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return BaseResponse.success(supplierService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<SupplierResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody SupplierRequest request) {
        return BaseResponse.success(supplierService.update(id, request));
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
