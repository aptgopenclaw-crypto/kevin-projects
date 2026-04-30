package com.taipei.iot.integration;

import com.taipei.iot.common.util.JwtClaimKeys;
import com.taipei.iot.tenant.TenantContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

/**
 * 整合測試輔助工具 — SecurityContext 切換 + TenantContext 管理
 */
public final class IntegrationTestHelper {

    public static final String TENANT_A = "TENANT_A";

    private IntegrationTestHelper() {}

    /**
     * 模擬指定使用者登入，設定 SecurityContext + TenantContext
     */
    public static void loginAs(String userId, Long deptId, String dataScope, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        auth.setDetails(Map.of(
                JwtClaimKeys.TENANT_ID, TENANT_A,
                JwtClaimKeys.DEPT_ID, deptId,
                JwtClaimKeys.DATA_SCOPE, dataScope
        ));

        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setCurrentTenantId(TENANT_A);
    }

    /**
     * 清除登入狀態
     */
    public static void logout() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // ── 預定義角色登入 ──

    /** 北區承辦 張志遠 — DEPT_USER */
    public static void loginAsSquad1Off1(Long deptId) {
        loginAs("u-squad1-off1", deptId, "DEPT",
                List.of("FAULT_VIEW", "FAULT_MANAGE", "REPAIR_VIEW"));
    }

    /** 北區維運 周志豪 — OPERATOR */
    public static void loginAsSquad1Op(Long deptId) {
        loginAs("u-squad1-op", deptId, "DEPT",
                List.of("FAULT_VIEW", "FAULT_MANAGE", "REPAIR_VIEW", "REPAIR_MANAGE",
                        "REPAIR_DISPATCH", "WORKFLOW_VIEW", "DEVICE_VIEW",
                        "REPLACEMENT_VIEW", "REPLACEMENT_MANAGE", "MATERIAL_VIEW"));
    }

    /** 北區外勤 蔡文傑 — FIELD_USER */
    public static void loginAsSquad1Field(Long deptId) {
        loginAs("u-squad1-field", deptId, "DEPT",
                List.of("REPAIR_VIEW", "REPAIR_MANAGE", "WORKFLOW_VIEW", "DEVICE_VIEW"));
    }

    /** 北區分隊長 李明華 — DEPT_ADMIN */
    public static void loginAsSquad1Mgr(Long deptId) {
        loginAs("u-squad1-mgr", deptId, "DEPT",
                List.of("FAULT_VIEW", "FAULT_MANAGE", "REPAIR_VIEW", "REPAIR_MANAGE",
                        "REPAIR_DISPATCH", "WORKFLOW_VIEW", "WORKFLOW_MANAGE",
                        "DEVICE_VIEW", "DEVICE_MANAGE"));
    }

    /** 工程股承辦 吳佳穎 — DEPT_USER */
    public static void loginAsEngOff1(Long deptId) {
        loginAs("u-eng-off1", deptId, "DEPT",
                List.of("REPLACEMENT_VIEW", "REPLACEMENT_MANAGE", "WORKFLOW_VIEW",
                        "DEVICE_VIEW", "MATERIAL_VIEW"));
    }

    /** 工程股股長 黃建中 — DEPT_ADMIN */
    public static void loginAsEngMgr(Long deptId) {
        loginAs("u-eng-mgr", deptId, "DEPT",
                List.of("REPLACEMENT_VIEW", "REPLACEMENT_MANAGE", "WORKFLOW_VIEW",
                        "WORKFLOW_MANAGE", "DEVICE_VIEW", "DEVICE_MANAGE",
                        "MATERIAL_VIEW"));
    }

    /** 工程監造 謝明達 — MONITOR */
    public static void loginAsEngMonitor(Long deptId) {
        loginAs("u-eng-monitor", deptId, "DEPT",
                List.of("REPLACEMENT_VIEW", "WORKFLOW_VIEW", "DEVICE_VIEW"));
    }

    /** 行政股倉管 林淑芬 — OPERATOR */
    public static void loginAsAdmWarehouse(Long deptId) {
        loginAs("u-adm-warehouse", deptId, "DEPT",
                List.of("MATERIAL_VIEW", "MATERIAL_MANAGE", "WORKFLOW_VIEW"));
    }

    /** 設備商工程師 葉建廷 — FIELD_USER */
    public static void loginAsVendorField1(Long deptId) {
        loginAs("u-vendor-field1", deptId, "DEPT",
                List.of("REPLACEMENT_VIEW", "REPLACEMENT_MANAGE", "WORKFLOW_VIEW",
                        "DEVICE_VIEW"));
    }

    /** 南區外勤 劉俊宏 — FIELD_USER */
    public static void loginAsSquad2Field(Long deptId) {
        loginAs("u-squad2-field", deptId, "DEPT",
                List.of("REPAIR_VIEW", "REPAIR_MANAGE", "WORKFLOW_VIEW", "DEVICE_VIEW"));
    }

    /** 南區分隊長 陳國強 — DEPT_ADMIN */
    public static void loginAsSquad2Mgr(Long deptId) {
        loginAs("u-squad2-mgr", deptId, "DEPT",
                List.of("FAULT_VIEW", "FAULT_MANAGE", "REPAIR_VIEW", "REPAIR_MANAGE",
                        "REPAIR_DISPATCH", "WORKFLOW_VIEW", "WORKFLOW_MANAGE",
                        "DEVICE_VIEW", "DEVICE_MANAGE"));
    }
}
