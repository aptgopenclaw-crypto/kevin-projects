package com.taipei.iot.dashboard.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dashboard.dto.DefaultLayoutRequest;
import com.taipei.iot.dashboard.dto.DefaultLayoutResponse;
import com.taipei.iot.dashboard.dto.LayoutRequest;
import com.taipei.iot.dashboard.dto.LayoutResponse;
import com.taipei.iot.dashboard.entity.DashboardDefaultLayout;
import com.taipei.iot.dashboard.entity.DashboardLayout;
import com.taipei.iot.dashboard.repository.DashboardDefaultLayoutRepository;
import com.taipei.iot.dashboard.repository.DashboardLayoutRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardLayoutService {

    private final DashboardLayoutRepository layoutRepository;
    private final DashboardDefaultLayoutRepository defaultLayoutRepository;

    /**
     * 取得個人版面；無個人版面時回傳角色預設→全域預設
     */
    public LayoutResponse getLayout() {
        String tenantId = TenantContext.getCurrentTenantId();
        String userId = SecurityContextUtils.getCurrentUserId();

        // 1. 個人版面
        Optional<DashboardLayout> personal = layoutRepository.findByTenantIdAndUserId(tenantId, userId);
        if (personal.isPresent()) {
            return toResponse(personal.get(), false);
        }

        // 2. 角色預設版面 (TODO: 取得使用者角色 roleType, 目前用 null 查全域)
        Optional<DashboardDefaultLayout> roleDefault = defaultLayoutRepository
                .findByTenantIdAndRoleTypeIsNull(tenantId);
        if (roleDefault.isPresent()) {
            return toDefaultResponse(roleDefault.get());
        }

        // 3. 回傳空版面 (前端使用內建預設)
        return LayoutResponse.builder()
                .isDefault(true)
                .layoutJson("[]")
                .build();
    }

    /**
     * 儲存個人版面 (UPSERT)
     */
    @Transactional
    public LayoutResponse saveLayout(LayoutRequest request) {
        String tenantId = TenantContext.getCurrentTenantId();
        String userId = SecurityContextUtils.getCurrentUserId();

        DashboardLayout layout = layoutRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> DashboardLayout.builder()
                        .userId(userId)
                        .build());

        layout.setLayoutJson(request.getLayoutJson());
        layout.setIsDefault(false);

        return toResponse(layoutRepository.save(layout), false);
    }

    /**
     * 重置為預設版面 — 刪除個人版面，回傳預設
     */
    @Transactional
    public LayoutResponse resetLayout() {
        String tenantId = TenantContext.getCurrentTenantId();
        String userId = SecurityContextUtils.getCurrentUserId();

        layoutRepository.deleteByTenantIdAndUserId(tenantId, userId);

        // 回傳預設版面
        Optional<DashboardDefaultLayout> defaultLayout = defaultLayoutRepository
                .findByTenantIdAndRoleTypeIsNull(tenantId);
        if (defaultLayout.isPresent()) {
            return toDefaultResponse(defaultLayout.get());
        }

        return LayoutResponse.builder()
                .isDefault(true)
                .layoutJson("[]")
                .build();
    }

    private LayoutResponse toResponse(DashboardLayout layout, boolean isDefault) {
        return LayoutResponse.builder()
                .id(layout.getId())
                .layoutJson(layout.getLayoutJson())
                .isDefault(isDefault)
                .updatedAt(layout.getUpdatedAt())
                .build();
    }

    private LayoutResponse toDefaultResponse(DashboardDefaultLayout defaultLayout) {
        return LayoutResponse.builder()
                .id(defaultLayout.getId())
                .layoutJson(defaultLayout.getLayoutJson())
                .isDefault(true)
                .updatedAt(null)
                .build();
    }

    // ── Default Layout Management (DASHBOARD_MANAGE) ──

    /**
     * 查詢全域預設版面
     */
    public DefaultLayoutResponse getDefaultLayout() {
        String tenantId = TenantContext.getCurrentTenantId();
        return defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull(tenantId)
                .map(this::toDefaultLayoutResponse)
                .orElse(null);
    }

    /**
     * 儲存全域預設版面 (UPSERT)
     */
    @Transactional
    public DefaultLayoutResponse saveDefaultLayout(DefaultLayoutRequest request) {
        String tenantId = TenantContext.getCurrentTenantId();
        String roleType = request.getRoleType();

        Optional<DashboardDefaultLayout> existing = (roleType == null)
                ? defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull(tenantId)
                : defaultLayoutRepository.findByTenantIdAndRoleType(tenantId, roleType);

        DashboardDefaultLayout layout = existing.orElseGet(() ->
                DashboardDefaultLayout.builder()
                        .roleType(roleType)
                        .build());

        layout.setLayoutJson(request.getLayoutJson());
        return toDefaultLayoutResponse(defaultLayoutRepository.save(layout));
    }

    private DefaultLayoutResponse toDefaultLayoutResponse(DashboardDefaultLayout dl) {
        return DefaultLayoutResponse.builder()
                .id(dl.getId())
                .layoutJson(dl.getLayoutJson())
                .roleType(dl.getRoleType())
                .createdAt(dl.getCreatedAt())
                .build();
    }
}
