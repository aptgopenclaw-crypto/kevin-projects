package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.DisposalRequest;
import com.taipei.iot.material.dto.DisposalResponse;
import com.taipei.iot.material.service.DisposalService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth/material/disposals")
@RequiredArgsConstructor
public class DisposalController {

    private final DisposalService disposalService;

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PageResponse<DisposalResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DisposalResponse> result = disposalService.list(PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<DisposalResponse> create(@Valid @RequestBody DisposalRequest request) {
        return BaseResponse.success(disposalService.create(request));
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
