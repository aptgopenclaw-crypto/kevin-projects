package com.taipei.iot.smartiot.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.smartiot.dto.AlertHistoryResponse;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.AlertStatus;
import com.taipei.iot.smartiot.service.AlertService;
import com.taipei.iot.user.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警管理 API (FN-07-015 / FN-07-016)。
 */
@RestController
@RequestMapping("/v1/auth/iot/alerts")
@RequiredArgsConstructor
public class AlertHistoryController {

    private final AlertService alertService;

    /**
     * GET /v1/auth/iot/alerts — 告警列表 (支援 status/severity 篩選)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<PageResponse<AlertHistoryResponse>> list(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AlertHistoryResponse> result = alertService.list(status, severity, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    /**
     * PUT /v1/auth/iot/alerts/{id}/acknowledge — 確認告警 (OPEN → ACK)
     */
    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.ACKNOWLEDGE_ALERT)
    public BaseResponse<AlertHistoryResponse> acknowledge(@PathVariable Long id) {
        return BaseResponse.success(alertService.acknowledge(id));
    }

    /**
     * PUT /v1/auth/iot/alerts/{id}/resolve — 解除告警 (ACK → RESOLVED)
     */
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.RESOLVE_ALERT)
    public BaseResponse<AlertHistoryResponse> resolve(@PathVariable Long id) {
        return BaseResponse.success(alertService.resolve(id));
    }

    /**
     * GET /v1/auth/iot/alerts/export — 匯出告警 (回傳 JSON，前端處理 CSV)
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    @AuditEvent(AuditEventType.EXPORT_ALERTS)
    public BaseResponse<PageResponse<AlertHistoryResponse>> export(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {
        Page<AlertHistoryResponse> result = alertService.list(status, severity, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> p) {
        return PageResponse.<T>builder()
                .content(p.getContent())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize())
                .build();
    }
}
