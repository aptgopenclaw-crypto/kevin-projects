package com.taipei.iot.smartiot.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.smartiot.dto.TelemetryFormatFieldResponse;
import com.taipei.iot.smartiot.dto.TelemetryFormatRequest;
import com.taipei.iot.smartiot.dto.TelemetryFormatResponse;
import com.taipei.iot.smartiot.service.TelemetryFormatService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/iot/telemetry-formats")
@RequiredArgsConstructor
public class TelemetryFormatController {

    private final TelemetryFormatService formatService;

    /**
     * POST /v1/auth/iot/telemetry-formats — 建立 Format 定義 (FN-07-044)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_TELEMETRY_FORMAT)
    public BaseResponse<TelemetryFormatResponse> create(@Valid @RequestBody TelemetryFormatRequest request) {
        return BaseResponse.success(formatService.create(request));
    }

    /**
     * GET /v1/auth/iot/telemetry-formats — Format 清單 (FN-07-045)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<PageResponse<TelemetryFormatResponse>> list(
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TelemetryFormatResponse> result = formatService.list(vendorName, keyword, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    /**
     * PUT /v1/auth/iot/telemetry-formats/{id} — 更新 Format (FN-07-046)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_TELEMETRY_FORMAT)
    public BaseResponse<TelemetryFormatResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody TelemetryFormatRequest request) {
        return BaseResponse.success(formatService.update(id, request));
    }

    /**
     * GET /v1/auth/iot/telemetry-formats/{id}/fields — 欄位清單 (FN-07-047)
     */
    @GetMapping("/{id}/fields")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<List<TelemetryFormatFieldResponse>> getFields(@PathVariable Long id) {
        return BaseResponse.success(formatService.getFields(id));
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
