package com.taipei.iot.tenant;

/**
 * ThreadLocal 容器，存放當前執行緒的租戶 ID。
 * <p>
 * 租戶功能貫穿 Spring Filter / MVC Interceptor 層級，
 * 最終帶動到 JPA Hibernate tenantFilter。
 */
public final class TenantContext {

    private TenantContext() {}

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final String SYSTEM_TENANT_MARKER = "SYSTEM";

    public static String getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /** 排程任務用：跳過 tenant filter */
    public static void setSystemContext() {
        CURRENT_TENANT.set(SYSTEM_TENANT_MARKER);
    }

    public static boolean isSystemContext() {
        return SYSTEM_TENANT_MARKER.equals(CURRENT_TENANT.get());
    }

    /**
     * 在 SYSTEM context 中執行給定動作，執行完畢後自動恢復先前的 context。
     * 適用於需要跨租戶操作的場景（如建立 UserTenantMapping）。
     */
    public static void runInSystemContext(Runnable action) {
        String previous = CURRENT_TENANT.get();
        try {
            setSystemContext();
            action.run();
        } finally {
            if (previous != null) {
                CURRENT_TENANT.set(previous);
            } else {
                CURRENT_TENANT.remove();
            }
        }
    }
}
