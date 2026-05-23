package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.TenderDashboardResponse;
import com.taipei.iot.tender.service.TenderDashboardService;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tender/dashboard")
@RequiredArgsConstructor
public class TenderDashboardController {

    private final TenderDashboardService service;

    @GetMapping
    @PreAuthorize("hasAuthority('tender:announcement:view')")
    public BaseResponse<TenderDashboardResponse> getDashboard() {
        String tenantId = TenantContext.getCurrentTenantId();
        return BaseResponse.success(service.getDashboard(tenantId));
    }
}
