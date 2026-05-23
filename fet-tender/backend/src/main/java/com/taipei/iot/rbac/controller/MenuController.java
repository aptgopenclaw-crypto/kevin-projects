package com.taipei.iot.rbac.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.rbac.dto.request.CreateMenuRequest;
import com.taipei.iot.rbac.dto.request.UpdateMenuRequest;
import com.taipei.iot.rbac.dto.response.MenuDto;
import com.taipei.iot.rbac.dto.response.UserMenuDto;
import com.taipei.iot.rbac.service.MenuService;
import com.taipei.iot.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/auth/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public BaseResponse<List<MenuDto>> getMenuTree() {
        return BaseResponse.success(menuService.getMenuTree());
    }

    @GetMapping("/my")
    public BaseResponse<List<UserMenuDto>> getMyMenus(Authentication authentication) {
        // Extract role IDs from authorities (e.g., ROLE_ADMIN -> ROLE_ADMIN)
        List<String> roleIds = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String tenantId = TenantContext.getCurrentTenantId();
        return BaseResponse.success(menuService.getMyMenus(roleIds, tenantId));
    }

    @PostMapping
    @AuditEvent(AuditEventType.CREATE_MENU)
    public BaseResponse<MenuDto> createMenu(@Valid @RequestBody CreateMenuRequest request) {
        return BaseResponse.success(menuService.createMenu(request));
    }

    @PutMapping
    @AuditEvent(AuditEventType.UPDATE_MENU)
    public BaseResponse<MenuDto> updateMenu(@Valid @RequestBody UpdateMenuRequest request) {
        return BaseResponse.success(menuService.updateMenu(request));
    }

    @DeleteMapping("/{menuId}")
    @AuditEvent(AuditEventType.DELETE_MENU)
    public BaseResponse<Void> deleteMenu(@PathVariable Long menuId) {
        menuService.deleteMenu(menuId);
        return BaseResponse.success(null);
    }

    @PatchMapping("/{menuId}/visible")
    @AuditEvent(AuditEventType.TOGGLE_VISIBLE)
    public BaseResponse<Void> toggleVisible(@PathVariable Long menuId,
                                            @RequestParam boolean visible) {
        menuService.toggleVisible(menuId, visible);
        return BaseResponse.success(null);
    }
}
