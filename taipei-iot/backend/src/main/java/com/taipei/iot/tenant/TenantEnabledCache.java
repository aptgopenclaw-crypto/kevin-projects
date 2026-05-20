package com.taipei.iot.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體快取：已停用的 tenantId 集合。
 * <p>
 * 用於 {@code JwtAuthenticationFilter} 中即時拒絕已停用場域的請求，
 * 避免每次請求都查詢資料庫。當 SUPER_ADMIN 透過 API 停用場域時，
 * {@code TenantAdminService} 會更新此快取。
 * <p>
 * 注意：此為單節點快取。若部署多實例，需改用 Redis pub/sub 或類似機制同步。
 */
@Component
@RequiredArgsConstructor
public class TenantEnabledCache {

    private final Set<String> disabledTenantIds = ConcurrentHashMap.newKeySet();
    private volatile boolean initialized = false;

    private final TenantRepository tenantRepository;

    /**
     * 應用啟動後首次呼叫時從 DB 載入已停用租戶。
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    tenantRepository.findAll().stream()
                            .filter(t -> !Boolean.TRUE.equals(t.getEnabled()))
                            .forEach(t -> disabledTenantIds.add(t.getTenantId()));
                    initialized = true;
                }
            }
        }
    }

    public boolean isTenantDisabled(String tenantId) {
        if (tenantId == null) return false;
        ensureInitialized();
        return disabledTenantIds.contains(tenantId);
    }

    public void markDisabled(String tenantId) {
        ensureInitialized();
        disabledTenantIds.add(tenantId);
    }

    public void markEnabled(String tenantId) {
        disabledTenantIds.remove(tenantId);
    }
}
