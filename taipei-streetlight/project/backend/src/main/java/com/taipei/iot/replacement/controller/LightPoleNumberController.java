package com.taipei.iot.replacement.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.replacement.dto.PoleNumberRequest;
import com.taipei.iot.replacement.dto.PoleNumberResponse;
import com.taipei.iot.replacement.service.LightPoleNumberService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/replacement/pole-numbers")
@RequiredArgsConstructor
public class LightPoleNumberController {

    private final LightPoleNumberService poleNumberService;

    @GetMapping
    @PreAuthorize("hasAuthority('POLE_NUMBER_MANAGE')")
    public BaseResponse<PageResponse<PoleNumberResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PoleNumberResponse> result = poleNumberService.list(keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('POLE_NUMBER_MANAGE')")
    public BaseResponse<PoleNumberResponse> generate(@Valid @RequestBody PoleNumberRequest request) {
        return BaseResponse.success(poleNumberService.generate(request));
    }

    @GetMapping("/{id}/qr-code")
    @PreAuthorize("hasAuthority('POLE_NUMBER_MANAGE')")
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id) {
        byte[] png = poleNumberService.getQrCodePng(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(png);
    }

    @PostMapping("/qr-codes/batch-pdf")
    @PreAuthorize("hasAuthority('POLE_NUMBER_MANAGE')")
    public ResponseEntity<byte[]> batchExportPdf(@RequestBody List<Long> ids) {
        byte[] pdf = poleNumberService.exportQrCodesPdf(ids);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pole-numbers-qrcode.pdf")
                .body(pdf);
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
