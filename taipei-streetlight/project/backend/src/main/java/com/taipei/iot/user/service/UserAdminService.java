package com.taipei.iot.user.service;

import com.taipei.iot.auth.entity.RoleEntity;
import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.entity.UserTenantMappingEntity;
import com.taipei.iot.auth.repository.RoleRepository;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.auth.repository.UserTenantMappingRepository;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.dept.repository.DeptInfoRepository;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.rbac.service.RoleService;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.user.dto.request.AddTenantRoleRequest;
import com.taipei.iot.user.dto.request.CreateUserRequest;
import com.taipei.iot.user.dto.request.UpdateUserRequest;
import com.taipei.iot.user.dto.response.PageResponse;
import com.taipei.iot.user.dto.response.UserListItemDto;
import com.taipei.iot.user.dto.response.UserTenantMappingDto;
import com.taipei.iot.user.entity.PasswordHistoryEntity;
import com.taipei.iot.user.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final UserTenantMappingRepository userTenantMappingRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final UserAuditService userAuditService;
    private final DataScopeHelper dataScopeHelper;
    private final DeptInfoRepository deptInfoRepository;
    private final RoleService roleService;

    @Transactional(readOnly = true)
    public PageResponse<UserListItemDto> listUsers(int page, int size, String keyword) {
        String tenantId = TenantContext.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<UserTenantMappingEntity> mappingPage;
        if (TenantContext.isSystemContext()) {
            mappingPage = userTenantMappingRepository.findAllActive(kw, pageable);
        } else {
            List<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();
            if (visibleDeptIds.isEmpty()) {
                // ALL scope → 不限制部門
                mappingPage = userTenantMappingRepository.findActiveByTenantId(tenantId, kw, pageable);
            } else if (visibleDeptIds.size() == 1) {
                mappingPage = userTenantMappingRepository.findActiveByTenantIdAndDeptId(
                        tenantId, visibleDeptIds.get(0), kw, pageable);
            } else {
                mappingPage = userTenantMappingRepository.findActiveByTenantIdAndDeptIdIn(
                        tenantId, visibleDeptIds, kw, pageable);
            }
        }

        List<UserListItemDto> items = mappingPage.getContent().stream()
                .map(this::toUserListItemDto)
                .collect(Collectors.toList());

        return PageResponse.<UserListItemDto>builder()
                .content(items)
                .totalElements(mappingPage.getTotalElements())
                .totalPages(mappingPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    @Transactional
    public UserListItemDto createUser(String adminUserId, CreateUserRequest req) {
        // DataScope 檢核：操作者只能在自己的部門範圍內建立帳號
        if (!dataScopeHelper.isDeptInScope(req.getDeptId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "無權在該部門建立帳號");
        }

        // 角色指派檢核：操作者只能指派自己權限範圍內的角色
        if (!roleService.isRoleAssignable(req.getRoleId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "無權指派該角色");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        passwordValidator.validate(req.getInitialPassword());

        String userId = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(req.getInitialPassword());

        UserEntity user = UserEntity.builder()
                .userId(userId)
                .email(req.getEmail())
                .displayName(req.getDisplayName())
                .phone(req.getPhone())
                .passwordHash(passwordHash)
                .enabled(true)
                .locked(false)
                .loginFailCount(0)
                .isSuperAdmin(false)
                .build();
        userRepository.save(user);

        String tenantId = req.getTenantId() != null ? req.getTenantId()
                : TenantContext.getCurrentTenantId();

        UserTenantMappingEntity mapping = UserTenantMappingEntity.builder()
                .userId(userId)
                .tenantId(tenantId)
                .roleId(req.getRoleId())
                .deptId(req.getDeptId())
                .enabled(true)
                .build();
        UserTenantMappingEntity savedMapping = userTenantMappingRepository.save(mapping);

        passwordHistoryRepository.save(PasswordHistoryEntity.builder()
                .userId(userId)
                .passwordHash(passwordHash)
                .build());

        userAuditService.logAction("CREATE", adminUserId, userId, "建立帳號 + 指派場域");

        return toUserListItemDto(savedMapping);
    }

    @Transactional
    public UserListItemDto updateUser(String adminUserId, String userId, UpdateUserRequest req) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (req.getDisplayName() != null) {
            user.setDisplayName(req.getDisplayName());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }

        userRepository.save(user);

        String tenantId = TenantContext.getCurrentTenantId();
        UserTenantMappingEntity mapping = userTenantMappingRepository
                .findByUserIdAndTenantId(userId, tenantId)
                .orElse(null);

        if (mapping != null) {
            boolean mappingChanged = false;
            if (req.getRoleId() != null && !req.getRoleId().equals(mapping.getRoleId())) {
                roleRepository.findById(req.getRoleId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
                mapping.setRoleId(req.getRoleId());
                mappingChanged = true;
            }
            if (req.getDeptId() != null && !req.getDeptId().equals(mapping.getDeptId())) {
                mapping.setDeptId(req.getDeptId());
                mappingChanged = true;
            }
            if (mappingChanged) {
                userTenantMappingRepository.save(mapping);
            }
            userAuditService.logAction("UPDATE", adminUserId, userId, "管理端更新帳號資料");
            return toUserListItemDto(mapping);
        }

        userAuditService.logAction("UPDATE", adminUserId, userId, "管理端更新帳號資料");
        return UserListItemDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .phone(user.getPhone())
                .enabled(user.getEnabled())
                .locked(user.getLocked())
                .build();
    }

    @Transactional
    public void disableUser(String adminUserId, String targetUserId) {
        if (adminUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "不可停用自己的帳號");
        }

        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setEnabled(false);
        userRepository.save(user);

        userAuditService.logAction("DISABLE", adminUserId, targetUserId, "停用帳號");
    }

    @Transactional
    public void softDeleteUser(String adminUserId, String targetUserId) {
        if (adminUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "不可刪除自己的帳號");
        }

        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setDeleted(true);
        user.setDeletedAt(java.time.LocalDateTime.now());
        user.setEnabled(false);
        userRepository.save(user);

        userAuditService.logAction("DELETE", adminUserId, targetUserId, "軟刪除帳號");
    }

    @Transactional
    public UserTenantMappingDto addTenantRole(String adminUserId, String targetUserId,
                                               AddTenantRoleRequest req) {
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String tenantId = req.getTenantId() != null ? req.getTenantId()
                : TenantContext.getCurrentTenantId();

        userTenantMappingRepository.findByUserIdAndTenantId(targetUserId, tenantId)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS,
                            "該帳號已存在此場域的存取權限");
                });

        UserTenantMappingEntity mapping = UserTenantMappingEntity.builder()
                .userId(targetUserId)
                .tenantId(tenantId)
                .roleId(req.getRoleId())
                .deptId(req.getDeptId())
                .enabled(true)
                .build();
        UserTenantMappingEntity saved = userTenantMappingRepository.save(mapping);

        userAuditService.logAction("ADD_TENANT", adminUserId, targetUserId,
                "新增場域: " + tenantId);

        return toUserTenantMappingDto(saved);
    }

    @Transactional
    public void removeTenantRole(String adminUserId, String targetUserId, Long mappingId) {
        UserTenantMappingEntity mapping = userTenantMappingRepository.findById(mappingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAPPING_NOT_FOUND));

        if (!mapping.getUserId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.MAPPING_NOT_FOUND);
        }

        userTenantMappingRepository.delete(mapping);

        userAuditService.logAction("REMOVE_TENANT", adminUserId, targetUserId,
                "移除場域: " + mapping.getTenantId());
    }

    @Transactional(readOnly = true)
    public List<UserTenantMappingDto> getUserTenantMappings(String targetUserId) {
        List<UserTenantMappingEntity> mappings = userTenantMappingRepository.findByUserId(targetUserId);

        String currentTenantId = TenantContext.getCurrentTenantId();
        boolean isSystem = TenantContext.isSystemContext();

        return mappings.stream()
                .filter(m -> isSystem || m.getTenantId().equals(currentTenantId))
                .map(this::toUserTenantMappingDto)
                .collect(Collectors.toList());
    }

    private UserListItemDto toUserListItemDto(UserTenantMappingEntity mapping) {
        UserEntity user = mapping.getUser();
        if (user == null) {
            user = userRepository.findById(mapping.getUserId()).orElse(null);
        }

        RoleEntity role = mapping.getRole();
        if (role == null) {
            role = roleRepository.findById(mapping.getRoleId()).orElse(null);
        }

        String deptName = null;
        if (mapping.getDeptId() != null) {
            deptName = deptInfoRepository.findById(mapping.getDeptId())
                    .map(dept -> dept.getDeptName())
                    .orElse(null);
        }

        UserListItemDto.UserListItemDtoBuilder builder = UserListItemDto.builder()
                .roleId(mapping.getRoleId())
                .deptId(mapping.getDeptId())
                .deptName(deptName)
                .mappingId(mapping.getId())
                .mappingEnabled(mapping.getEnabled());

        if (user != null) {
            builder.userId(user.getUserId())
                    .email(user.getEmail())
                    .displayName(user.getDisplayName())
                    .phone(user.getPhone())
                    .enabled(user.getEnabled())
                    .locked(user.getLocked())
                    .deleted(user.getDeleted());
        }

        if (role != null) {
            builder.roleCode(role.getCode())
                    .roleName(role.getName());
        }

        return builder.build();
    }

    private UserTenantMappingDto toUserTenantMappingDto(UserTenantMappingEntity mapping) {
        RoleEntity role = mapping.getRole();
        if (role == null) {
            role = roleRepository.findById(mapping.getRoleId()).orElse(null);
        }

        String tenantName = null;
        if (mapping.getTenant() != null) {
            tenantName = mapping.getTenant().getTenantName();
        }

        return UserTenantMappingDto.builder()
                .mappingId(mapping.getId())
                .tenantId(mapping.getTenantId())
                .tenantName(tenantName)
                .roleId(mapping.getRoleId())
                .roleCode(role != null ? role.getCode() : null)
                .roleName(role != null ? role.getName() : null)
                .deptId(mapping.getDeptId())
                .enabled(mapping.getEnabled())
                .build();
    }
}
