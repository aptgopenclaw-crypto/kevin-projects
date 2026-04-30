package com.taipei.iot.dashboard.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.dashboard.dto.DefaultLayoutRequest;
import com.taipei.iot.dashboard.dto.DefaultLayoutResponse;
import com.taipei.iot.dashboard.dto.LayoutRequest;
import com.taipei.iot.dashboard.dto.LayoutResponse;
import com.taipei.iot.dashboard.service.DashboardLayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/dashboard/layout")
@RequiredArgsConstructor
public class DashboardLayoutController {

    private final DashboardLayoutService layoutService;

    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<LayoutResponse> getLayout() {
        return BaseResponse.success(layoutService.getLayout());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @AuditEvent(AuditEventType.UPDATE_DASHBOARD_LAYOUT)
    public BaseResponse<LayoutResponse> saveLayout(@Valid @RequestBody LayoutRequest request) {
        return BaseResponse.success(layoutService.saveLayout(request));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @AuditEvent(AuditEventType.RESET_DASHBOARD_LAYOUT)
    public BaseResponse<LayoutResponse> resetLayout() {
        return BaseResponse.success(layoutService.resetLayout());
    }

    // ── Default Layout Management (DASHBOARD_MANAGE) ──

    @GetMapping("/default")
    @PreAuthorize("hasAuthority('DASHBOARD_MANAGE')")
    public BaseResponse<DefaultLayoutResponse> getDefaultLayout() {
        return BaseResponse.success(layoutService.getDefaultLayout());
    }

    @PutMapping("/default")
    @PreAuthorize("hasAuthority('DASHBOARD_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_DEFAULT_DASHBOARD_LAYOUT)
    public BaseResponse<DefaultLayoutResponse> saveDefaultLayout(@Valid @RequestBody DefaultLayoutRequest request) {
        return BaseResponse.success(layoutService.saveDefaultLayout(request));
    }
}
