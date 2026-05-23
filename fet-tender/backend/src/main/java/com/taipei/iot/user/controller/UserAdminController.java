package com.taipei.iot.user.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.user.dto.request.AddTenantRoleRequest;
import com.taipei.iot.user.dto.request.CreateUserRequest;
import com.taipei.iot.user.dto.request.UpdateUserRequest;
import com.taipei.iot.common.dto.PageResponse;
import com.taipei.iot.user.dto.response.UserListItemDto;
import com.taipei.iot.user.dto.response.UserTenantMappingDto;
import com.taipei.iot.user.service.UserAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/v1/auth/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_LIST')")
    public BaseResponse<PageResponse<UserListItemDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return BaseResponse.success(userAdminService.listUsers(page, size, keyword));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_LIST')")
    public BaseResponse<UserListItemDto> getUser(
            @PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId) {
        return BaseResponse.success(userAdminService.getUser(userId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @AuditEvent(AuditEventType.CREATE_USER)
    public BaseResponse<UserListItemDto> createUser(Authentication authentication,
                                                     @Valid @RequestBody CreateUserRequest req) {
        String adminUserId = (String) authentication.getPrincipal();
        return BaseResponse.success(userAdminService.createUser(adminUserId, req));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @AuditEvent(AuditEventType.UPDATE_USER)
    public BaseResponse<UserListItemDto> updateUser(Authentication authentication,
                                                     @PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId,
                                                     @Valid @RequestBody UpdateUserRequest req) {
        String adminUserId = (String) authentication.getPrincipal();
        return BaseResponse.success(userAdminService.updateUser(adminUserId, userId, req));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_DISABLE')")
    @AuditEvent(AuditEventType.DISABLE_USER)
    public BaseResponse<Void> disableUser(Authentication authentication,
                                           @PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId) {
        String adminUserId = (String) authentication.getPrincipal();
        userAdminService.disableUser(adminUserId, userId);
        return BaseResponse.success(null);
    }

    @PatchMapping("/{userId}/soft-delete")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @AuditEvent(AuditEventType.SOFT_DELETE_USER)
    public BaseResponse<Void> softDeleteUser(Authentication authentication,
                                              @PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId) {
        String adminUserId = (String) authentication.getPrincipal();
        userAdminService.softDeleteUser(adminUserId, userId);
        return BaseResponse.success(null);
    }

    @GetMapping("/{userId}/tenant-roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BaseResponse<List<UserTenantMappingDto>> getUserTenantMappings(
            @PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId) {
        return BaseResponse.success(userAdminService.getUserTenantMappings(userId));
    }

    @PostMapping("/{userId}/tenant-roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BaseResponse<UserTenantMappingDto> addTenantRole(Authentication authentication,
                                                             @PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId,
                                                             @Valid @RequestBody AddTenantRoleRequest req) {
        String adminUserId = (String) authentication.getPrincipal();
        return BaseResponse.success(userAdminService.addTenantRole(adminUserId, userId, req));
    }

    @DeleteMapping("/{userId}/tenant-roles/{mappingId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public BaseResponse<Void> removeTenantRole(Authentication authentication,
                                                @PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId,
                                                @PathVariable Long mappingId) {
        String adminUserId = (String) authentication.getPrincipal();
        userAdminService.removeTenantRole(adminUserId, userId, mappingId);
        return BaseResponse.success(null);
    }
}
