package com.taipei.iot.tenant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Entity Listener — 租戶資料寫入防護。
 *
 * <ul>
 *   <li>{@code @PrePersist}：新增時自動填入 tenantId</li>
 *   <li>{@code @PreUpdate}：更新時驗證 tenantId 不被跨租戶篡改</li>
 *   <li>{@code @PreRemove}：刪除時驗證 tenantId 不被跨租戶刪除</li>
 * </ul>
 *
 * <h3>為什麼需要 PreUpdate / PreRemove 驗證？</h3>
 * <p>Hibernate {@code @Filter} 只保護「讀取」路徑（SELECT）。如果攻擊者透過其他途徑
 * （例如 URL 參數猜測 ID）取得了另一個租戶的 Entity 引用，執行 {@code save()} 或
 * {@code delete()} 時 Hibernate Filter 不會阻擋。</p>
 * <p>攻擊場景：</p>
 * <pre>
 *   // 租戶 A 的使用者猜到租戶 B 的 recordId
 *   PUT /api/records/{recordId}
 *   → Service 拿到租戶 B 的 Entity → save() 修改成功 → 跨租戶篡改！
 * </pre>
 * <p>加入 PreUpdate/PreRemove 驗證後，任何嘗試修改/刪除不屬於當前租戶的資料，
 * 都會拋出 {@link SecurityException}，即時阻斷。</p>
 */
@Slf4j
public class TenantEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof TenantAware tenantAware) {
            if (tenantAware.getTenantId() == null) {
                String tenantId = TenantContext.getCurrentTenantId();
                // SYSTEM context 下不自動帶入 tenantId，防止資料被污染為 "SYSTEM"
                // 呼叫方必須自行設定 tenantId
                if (tenantId != null && !TenantContext.isSystemContext()) {
                    tenantAware.setTenantId(tenantId);
                }
            }
        }
    }

    /**
     * 更新前驗證：確保不會跨租戶修改資料。
     *
     * <p>檢查邏輯：</p>
     * <ul>
     *   <li>System Context → 跳過（排程、auth 流程允許跨租戶操作）</li>
     *   <li>TenantContext 未設定 → 跳過（非 HTTP 請求路徑，如啟動初始化）</li>
     *   <li>Entity 的 tenantId ≠ 當前 tenantId → 拋出 SecurityException</li>
     * </ul>
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        verifyTenantOwnership(entity, "UPDATE");
    }

    /**
     * 刪除前驗證：確保不會跨租戶刪除資料。
     */
    @PreRemove
    public void preRemove(Object entity) {
        verifyTenantOwnership(entity, "DELETE");
    }

    /**
     * 驗證當前租戶是否有權操作此 Entity。
     */
    private void verifyTenantOwnership(Object entity, String operation) {
        if (!(entity instanceof TenantAware tenantAware)) {
            return;
        }

        // System Context（排程、auth 流程）：允許跨租戶操作
        if (TenantContext.isSystemContext()) {
            return;
        }

        String currentTenantId = TenantContext.getCurrentTenantId();
        if (currentTenantId == null) {
            // TenantContext 未設定（可能是非 HTTP 路徑），不阻擋但記錄警告
            return;
        }

        String entityTenantId = tenantAware.getTenantId();
        if (entityTenantId != null && !currentTenantId.equals(entityTenantId)) {
            // [安全阻斷] 租戶不匹配 — 這是跨租戶越權操作
            log.error("[SECURITY] TENANT_MISMATCH operation={} entityTenant={} contextTenant={} entityClass={}",
                    operation, entityTenantId, currentTenantId, entity.getClass().getSimpleName());
            throw new SecurityException(
                    "Tenant mismatch: attempting to " + operation + " entity of tenant [" +
                    entityTenantId + "] from context [" + currentTenantId + "]");
        }
    }
}
