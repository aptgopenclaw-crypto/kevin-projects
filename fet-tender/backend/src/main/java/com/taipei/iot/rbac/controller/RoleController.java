package com.taipei.iot.rbac.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.rbac.dto.request.AssignRolePermissionsRequest;
import com.taipei.iot.rbac.dto.request.CreateRoleRequest;
import com.taipei.iot.rbac.dto.request.UpdateRoleRequest;
import com.taipei.iot.rbac.dto.response.RoleDto;
import com.taipei.iot.rbac.dto.response.RolePermissionListDto;
import com.taipei.iot.rbac.service.RoleService;
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
@RequestMapping("/v1/auth/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public BaseResponse<List<RoleDto>> listRoles() {
        return BaseResponse.success(roleService.listRoles());
    }

    @GetMapping("/assignable")
    public BaseResponse<List<RoleDto>> listAssignableRoles() {
        return BaseResponse.success(roleService.listAssignableRoles());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    @AuditEvent(AuditEventType.CREATE_ROLE)
    public BaseResponse<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return BaseResponse.success(roleService.createRole(request));
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    @AuditEvent(AuditEventType.UPDATE_ROLE)
    public BaseResponse<RoleDto> updateRole(@PathVariable String roleId,
                                            @Valid @RequestBody UpdateRoleRequest request) {
        return BaseResponse.success(roleService.updateRole(roleId, request));
    }

    @PatchMapping("/{roleId}/enabled")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    @AuditEvent(AuditEventType.TOGGLE_ROLE_ENABLED)
    public BaseResponse<Void> toggleEnabled(@PathVariable String roleId,
                                            @RequestParam boolean enabled) {
        roleService.toggleEnabled(roleId, enabled);
        return BaseResponse.success(null);
    }

    @GetMapping("/{roleId}/permissions")
    public BaseResponse<RolePermissionListDto> getRolePermissions(@PathVariable String roleId) {
        return BaseResponse.success(roleService.getRolePermissions(roleId));
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN_PERM')")
    @AuditEvent(AuditEventType.ASSIGN_ROLE_PERMISSIONS)
    public BaseResponse<RolePermissionListDto> assignPermissions(
            @PathVariable String roleId,
            @Valid @RequestBody AssignRolePermissionsRequest request) {
        return BaseResponse.success(roleService.assignPermissions(roleId, request));
    }
}
