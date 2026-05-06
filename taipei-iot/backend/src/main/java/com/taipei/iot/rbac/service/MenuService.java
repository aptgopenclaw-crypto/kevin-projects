package com.taipei.iot.rbac.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.rbac.dto.request.CreateMenuRequest;
import com.taipei.iot.rbac.dto.request.UpdateMenuRequest;
import com.taipei.iot.rbac.dto.response.MenuDto;
import com.taipei.iot.rbac.dto.response.UserMenuDto;
import com.taipei.iot.rbac.entity.MenuEntity;
import com.taipei.iot.rbac.entity.RolePermissionEntity;
import com.taipei.iot.rbac.repository.MenuRepository;
import com.taipei.iot.rbac.repository.PermissionRepository;
import com.taipei.iot.rbac.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<MenuDto> getMenuTree() {
        List<MenuEntity> allMenus = menuRepository.findAllByOrderBySortOrder();
        return buildMenuTree(allMenus);
    }

    @Transactional(readOnly = true)
    public List<UserMenuDto> getMyMenus(List<String> roleIds, String tenantId) {
        // SUPER_ADMIN bypass: return all visible menus
        if (roleIds.stream().anyMatch(r -> r.equals("ROLE_SUPER_ADMIN"))) {
            List<MenuEntity> allVisible = menuRepository.findAllByOrderBySortOrder().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getVisible()))
                    .collect(Collectors.toList());
            return buildUserMenuTree(allVisible);
        }

        // 1. Get permission codes for the user's roles (with tenant scope)
        List<RolePermissionEntity> rps = rolePermissionRepository.findByRoleIdInAndTenantScope(roleIds, tenantId);
        List<String> permissionIds = rps.stream()
                .map(RolePermissionEntity::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        if (permissionIds.isEmpty()) {
            return List.of();
        }

        // 2. Get permission codes from permission IDs
        List<String> permissionCodes = permissionRepository.findAllById(permissionIds).stream()
                .map(com.taipei.iot.rbac.entity.PermissionEntity::getCode)
                .collect(Collectors.toList());

        if (permissionCodes.isEmpty()) {
            return List.of();
        }

        // 3. Get visible menus matching permission codes
        List<MenuEntity> permittedMenus = menuRepository.findByPermissionCodeInAndVisibleTrue(permissionCodes);

        // 4. Also include DIRECTORY parents that have visible children
        List<MenuEntity> allMenus = menuRepository.findAllByOrderBySortOrder();
        List<Long> permittedMenuIds = permittedMenus.stream()
                .map(MenuEntity::getMenuId)
                .collect(Collectors.toList());

        // Include parent directories
        List<MenuEntity> result = new ArrayList<>(permittedMenus);
        for (MenuEntity menu : allMenus) {
            if ("DIRECTORY".equals(menu.getMenuType())
                    && Boolean.TRUE.equals(menu.getVisible())
                    && !permittedMenuIds.contains(menu.getMenuId())
                    && hasVisibleChild(menu.getMenuId(), permittedMenus)) {
                result.add(menu);
            }
        }

        return buildUserMenuTree(result);
    }

    @Transactional
    public MenuDto createMenu(CreateMenuRequest req) {
        MenuEntity entity = MenuEntity.builder()
                .parentId(req.getParentId())
                .name(req.getName())
                .menuType(req.getMenuType())
                .routeName(req.getRouteName())
                .routePath(req.getRoutePath())
                .component(req.getComponent())
                .permissionCode(req.getPermissionCode())
                .icon(req.getIcon())
                .sortOrder(req.getSortOrder())
                .visible(req.isVisible())
                .keepAlive(req.isKeepAlive())
                .redirect(req.getRedirect())
                .createTime(LocalDateTime.now())
                .build();

        MenuEntity saved = menuRepository.save(entity);
        return toMenuDto(saved);
    }

    @Transactional
    public MenuDto updateMenu(UpdateMenuRequest req) {
        MenuEntity entity = menuRepository.findById(req.getMenuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        entity.setParentId(req.getParentId());
        entity.setName(req.getName());
        entity.setMenuType(req.getMenuType());
        entity.setRouteName(req.getRouteName());
        entity.setRoutePath(req.getRoutePath());
        entity.setComponent(req.getComponent());
        entity.setPermissionCode(req.getPermissionCode());
        entity.setIcon(req.getIcon());
        entity.setSortOrder(req.getSortOrder());
        entity.setVisible(req.isVisible());
        entity.setKeepAlive(req.isKeepAlive());
        entity.setRedirect(req.getRedirect());
        entity.setUpdateTime(LocalDateTime.now());

        MenuEntity saved = menuRepository.save(entity);
        return toMenuDto(saved);
    }

    @Transactional
    public void deleteMenu(Long menuId) {
        if (!menuRepository.existsById(menuId)) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        if (menuRepository.existsByParentId(menuId)) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        }
        menuRepository.deleteById(menuId);
    }

    @Transactional
    public void toggleVisible(Long menuId, boolean visible) {
        MenuEntity entity = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
        entity.setVisible(visible);
        entity.setUpdateTime(LocalDateTime.now());
        menuRepository.save(entity);
    }

    // ---- private helpers ----

    private boolean hasVisibleChild(Long parentId, List<MenuEntity> permittedMenus) {
        return permittedMenus.stream()
                .anyMatch(m -> parentId.equals(m.getParentId()));
    }

    private List<MenuDto> buildMenuTree(List<MenuEntity> allMenus) {
        Map<Long, List<MenuEntity>> groupedByParent = allMenus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(MenuEntity::getParentId));

        return allMenus.stream()
                .filter(m -> m.getParentId() == null)
                .map(m -> toMenuDtoWithChildren(m, groupedByParent))
                .collect(Collectors.toList());
    }

    private MenuDto toMenuDtoWithChildren(MenuEntity entity, Map<Long, List<MenuEntity>> groupedByParent) {
        MenuDto dto = toMenuDto(entity);
        List<MenuEntity> children = groupedByParent.getOrDefault(entity.getMenuId(), List.of());
        dto.setChildren(children.stream()
                .map(c -> toMenuDtoWithChildren(c, groupedByParent))
                .collect(Collectors.toList()));
        return dto;
    }

    private List<UserMenuDto> buildUserMenuTree(List<MenuEntity> menus) {
        Map<Long, List<MenuEntity>> groupedByParent = menus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(MenuEntity::getParentId));

        return menus.stream()
                .filter(m -> m.getParentId() == null)
                .map(m -> toUserMenuDtoWithChildren(m, groupedByParent))
                .collect(Collectors.toList());
    }

    private UserMenuDto toUserMenuDtoWithChildren(MenuEntity entity, Map<Long, List<MenuEntity>> groupedByParent) {
        UserMenuDto dto = toUserMenuDto(entity);
        List<MenuEntity> children = groupedByParent.getOrDefault(entity.getMenuId(), List.of());
        dto.setChildren(children.stream()
                .map(c -> toUserMenuDtoWithChildren(c, groupedByParent))
                .collect(Collectors.toList()));
        return dto;
    }

    private MenuDto toMenuDto(MenuEntity entity) {
        return MenuDto.builder()
                .menuId(entity.getMenuId())
                .parentId(entity.getParentId())
                .name(entity.getName())
                .menuType(entity.getMenuType())
                .routeName(entity.getRouteName())
                .routePath(entity.getRoutePath())
                .component(entity.getComponent())
                .permissionCode(entity.getPermissionCode())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .visible(entity.getVisible())
                .keepAlive(entity.getKeepAlive())
                .redirect(entity.getRedirect())
                .children(new ArrayList<>())
                .build();
    }

    private UserMenuDto toUserMenuDto(MenuEntity entity) {
        return UserMenuDto.builder()
                .menuId(entity.getMenuId())
                .parentId(entity.getParentId())
                .name(entity.getName())
                .menuType(entity.getMenuType())
                .routeName(entity.getRouteName())
                .routePath(entity.getRoutePath())
                .component(entity.getComponent())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .redirect(entity.getRedirect())
                .children(new ArrayList<>())
                .build();
    }
}
