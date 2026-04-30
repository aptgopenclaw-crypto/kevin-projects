package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.ApprovedMaterialRequest;
import com.taipei.iot.material.dto.ApprovedMaterialResponse;
import com.taipei.iot.material.dto.ImportResult;
import com.taipei.iot.material.enums.ApprovedMaterialStatus;
import com.taipei.iot.material.service.ApprovedMaterialService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/auth/material/approved-materials")
@RequiredArgsConstructor
public class ApprovedMaterialController {

    private final ApprovedMaterialService approvedMaterialService;

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PageResponse<ApprovedMaterialResponse>> list(
            @RequestParam(required = false) ApprovedMaterialStatus status,
            @RequestParam(required = false) Long materialSpecId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ApprovedMaterialResponse> result = approvedMaterialService.list(
                status, materialSpecId, keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<ApprovedMaterialResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(approvedMaterialService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<ApprovedMaterialResponse> create(@Valid @RequestBody ApprovedMaterialRequest request) {
        return BaseResponse.success(approvedMaterialService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<ApprovedMaterialResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody ApprovedMaterialRequest request) {
        return BaseResponse.success(approvedMaterialService.update(id, request));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<ImportResult> batchImport(@RequestParam("file") MultipartFile file) {
        return BaseResponse.success(approvedMaterialService.batchImport(file));
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
