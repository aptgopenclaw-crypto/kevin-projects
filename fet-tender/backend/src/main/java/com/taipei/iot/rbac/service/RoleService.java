package com.taipei.iot.rbac.service;

import com.taipei.iot.auth.entity.RoleEntity;
import com.taipei.iot.auth.repository.RoleRepository;
import com.taipei.iot.common.dto.UserInfo;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.enums.DataScopeEnum;
import com.taipei.iot.rbac.dto.request.AssignRolePermissionsRequest;
import com.taipei.iot.rbac.dto.request.CreateRoleRequest;
import com.taipei.iot.rbac.dto.request.UpdateRoleRequest;
import com.taipei.iot.rbac.dto.response.PermissionDto;
import com.taipei.iot.rbac.dto.response.RoleDto;
import com.taipei.iot.rbac.dto.response.RolePermissionListDto;
import com.taipei.iot.rbac.entity.RolePermissionEntity;
import com.taipei.iot.rbac.repository.PermissionRepository;
import com.taipei.iot.rbac.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleDto> listRoles() {
        return roleRepository.findAllByOrderByBuiltInDescCodeAsc().stream()
                .map(this::toRoleDto)
                .collect(Collectors.toList());
    }

    /**
     * 依據當前使用者的 DataScope 回傳可指派的角色清單。
     * ALL scope → 可指派所有啟用角色。
     * THIS_LEVEL / THIS_LEVEL_AND_BELOW → 只能指派 scope 非 ALL 的角色。
     */
    public List<RoleDto> listAssignableRoles() {
        UserInfo user = SecurityContextUtils.getUserInfo();
        DataScopeEnum callerScope = (user != null && user.getDataScope() != null)
                ? DataScopeEnum.fromString(user.getDataScope()) : DataScopeEnum.ALL;

        return roleRepository.findAllByOrderByBuiltInDescCodeAsc().stream()
                .filter(RoleEntity::getEnabled)
                .filter(role -> {
                    if (callerScope == DataScopeEnum.ALL) {
                        return true;
                    }
                    // 非 ALL scope 的操作者只能指派 scope 不為 ALL 的角色
                    DataScopeEnum roleScope = DataScopeEnum.fromString(role.getDataScope());
                    return roleScope != DataScopeEnum.ALL;
                })
                .map(this::toRoleDto)
                .collect(Collectors.toList());
    }

    /**
     * 檢查目標 roleId 是否為當前使用者可指派的角色。
     */
    public boolean isRoleAssignable(String roleId) {
        return listAssignableRoles().stream()
                .anyMatch(r -> r.getRoleId().equals(roleId));
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ErrorCode.ROLE_CODE_DUPLICATE);
        }

        RoleEntity entity = RoleEntity.builder()
                .roleId("ROLE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .builtIn(false)
                .enabled(true)
                .dataScope(request.getDataScope() != null ? request.getDataScope() : "ALL")
                .build();

        RoleEntity saved = roleRepository.save(entity);
        return toRoleDto(saved);
    }

    @Transactional
    public RoleDto updateRole(String roleId, UpdateRoleRequest request) {
        RoleEntity entity = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setEnabled(request.isEnabled());
        if (request.getDataScope() != null) {
            entity.setDataScope(request.getDataScope());
        }

        RoleEntity saved = roleRepository.save(entity);
        return toRoleDto(saved);
    }

    @Transactional
    public void toggleEnabled(String roleId, boolean enabled) {
        RoleEntity entity = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        entity.setEnabled(enabled);
        roleRepository.save(entity);
    }

    @Transactional
    public RolePermissionListDto assignPermissions(String roleId, AssignRolePermissionsRequest request) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        // Remove all existing permissions for this role (global scope only)
        rolePermissionRepository.deleteByRoleId(roleId);

        // Insert new permissions
        List<RolePermissionEntity> newRps = request.getPermissionIds().stream()
                .map(permId -> RolePermissionEntity.builder()
                        .roleId(roleId)
                        .permissionId(permId)
                        .tenantId(null)
                        .build())
                .collect(Collectors.toList());
        rolePermissionRepository.saveAll(newRps);

        // Return updated permission list
        List<PermissionDto> permissions = permissionRepository.findAllById(request.getPermissionIds()).stream()
                .map(this::toPermissionDto)
                .collect(Collectors.toList());

        return RolePermissionListDto.builder()
                .roleId(role.getRoleId())
                .roleCode(role.getCode())
                .permissions(permissions)
                .build();
    }

    public RolePermissionListDto getRolePermissions(String roleId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        List<RolePermissionEntity> rps = rolePermissionRepository.findByRoleId(roleId);
        List<String> permIds = rps.stream()
                .map(RolePermissionEntity::getPermissionId)
                .collect(Collectors.toList());

        List<PermissionDto> permissions = permissionRepository.findAllById(permIds).stream()
                .map(this::toPermissionDto)
                .collect(Collectors.toList());

        return RolePermissionListDto.builder()
                .roleId(role.getRoleId())
                .roleCode(role.getCode())
                .permissions(permissions)
                .build();
    }

    public RolePermissionListDto getRolePermissions(String roleId, String tenantId) {
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        List<RolePermissionEntity> rps = rolePermissionRepository.findByRoleIdAndTenantScope(roleId, tenantId);
        List<String> permIds = rps.stream()
                .map(RolePermissionEntity::getPermissionId)
                .collect(Collectors.toList());

        List<PermissionDto> permissions = permissionRepository.findAllById(permIds).stream()
                .map(this::toPermissionDto)
                .collect(Collectors.toList());

        return RolePermissionListDto.builder()
                .roleId(role.getRoleId())
                .roleCode(role.getCode())
                .permissions(permissions)
                .build();
    }

    private RoleDto toRoleDto(RoleEntity entity) {
        return RoleDto.builder()
                .roleId(entity.getRoleId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .builtIn(entity.getBuiltIn())
                .enabled(entity.getEnabled())
                .dataScope(entity.getDataScope())
                .build();
    }

    private PermissionDto toPermissionDto(com.taipei.iot.rbac.entity.PermissionEntity entity) {
        return PermissionDto.builder()
                .permissionId(entity.getPermissionId())
                .code(entity.getCode())
                .name(entity.getName())
                .groupName(entity.getGroupName())
                .build();
    }
}
