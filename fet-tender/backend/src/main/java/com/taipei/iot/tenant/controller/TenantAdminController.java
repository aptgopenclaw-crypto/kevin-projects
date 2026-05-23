package com.taipei.iot.tenant.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tenant.dto.CreateTenantRequest;
import com.taipei.iot.tenant.dto.TenantDto;
import com.taipei.iot.tenant.dto.UpdateTenantRequest;
import com.taipei.iot.tenant.service.TenantAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    @GetMapping
    public BaseResponse<List<TenantDto>> listTenants() {
        return BaseResponse.success(tenantAdminService.listTenants());
    }

    @PostMapping
    @AuditEvent(AuditEventType.CREATE_TENANT)
    public BaseResponse<TenantDto> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return BaseResponse.success(tenantAdminService.createTenant(request));
    }

    @PutMapping("/{tenantId}")
    @AuditEvent(AuditEventType.UPDATE_TENANT)
    public BaseResponse<TenantDto> updateTenant(@PathVariable String tenantId,
                                                 @Valid @RequestBody UpdateTenantRequest request) {
        return BaseResponse.success(tenantAdminService.updateTenant(tenantId, request));
    }

    @PatchMapping("/{tenantId}/enabled")
    @AuditEvent(AuditEventType.TOGGLE_TENANT_ENABLED)
    public BaseResponse<Void> toggleEnabled(@PathVariable String tenantId,
                                             @RequestParam boolean enabled) {
        tenantAdminService.toggleEnabled(tenantId, enabled);
        return BaseResponse.success(null);
    }
}
