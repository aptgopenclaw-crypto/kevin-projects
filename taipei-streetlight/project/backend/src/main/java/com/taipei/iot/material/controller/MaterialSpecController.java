package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.MaterialSpecRequest;
import com.taipei.iot.material.dto.MaterialSpecResponse;
import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.material.enums.MaterialStatus;
import com.taipei.iot.material.service.MaterialSpecService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth/material/specs")
@RequiredArgsConstructor
public class MaterialSpecController {

    private final MaterialSpecService materialSpecService;

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PageResponse<MaterialSpecResponse>> list(
            @RequestParam(required = false) MaterialCategory category,
            @RequestParam(required = false) MaterialStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<MaterialSpecResponse> result = materialSpecService.list(category, status, keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<MaterialSpecResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(materialSpecService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<MaterialSpecResponse> create(@Valid @RequestBody MaterialSpecRequest request) {
        return BaseResponse.success(materialSpecService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<MaterialSpecResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody MaterialSpecRequest request) {
        return BaseResponse.success(materialSpecService.update(id, request));
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
