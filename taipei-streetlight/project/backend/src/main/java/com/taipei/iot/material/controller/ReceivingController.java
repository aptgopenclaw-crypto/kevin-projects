package com.taipei.iot.material.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.material.dto.ReceivingRequest;
import com.taipei.iot.material.dto.ReceivingResponse;
import com.taipei.iot.material.service.ReceivingService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth/material/receiving")
@RequiredArgsConstructor
public class ReceivingController {

    private final ReceivingService receivingService;

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public BaseResponse<PageResponse<ReceivingResponse>> list(
            @RequestParam(required = false) Long poId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ReceivingResponse> result = receivingService.list(poId, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_MANAGE')")
    public BaseResponse<ReceivingResponse> receive(@Valid @RequestBody ReceivingRequest request) {
        return BaseResponse.success(receivingService.receive(request));
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
