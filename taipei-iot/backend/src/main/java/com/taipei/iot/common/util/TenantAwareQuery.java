package com.taipei.iot.common.util;

import com.taipei.iot.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;

/**
 * 租戶安全的 Native Query 工具類。
 *
 * <h2>為什麼需要這個？</h2>
 * <p>Hibernate {@code @Filter} 只對 HQL/JPQL 和 Spring Data Repository 方法生效。
 * 當開發者使用 {@code entityManager.createNativeQuery(sql)} 時，
 * Hibernate Filter <b>完全不會介入</b>，租戶過濾形同虛設。</p>
 *
 * <p>這意味著一旦有人寫了：</p>
 * <pre>{@code
 *   // ❌ 危險：native query 沒有 tenant_id，會查到所有租戶的資料
 *   String sql = "SELECT * FROM device_data WHERE device_id = :deviceId";
 *   entityManager.createNativeQuery(sql);
 * }</pre>
 * <p>就會產生<b>靜默的跨租戶資料洩漏</b> — 不報錯、不寫 log、code review 難抓、單租戶測試不會發現。</p>
 *
 * <h2>使用方式</h2>
 *
 * <h3>情境 1：查詢有 tenant_id 的表（大部分情況）</h3>
 * <pre>{@code
 *   // ✅ 安全：自動驗證 SQL 中包含 tenant_id，並自動綁定 :tenantId 參數
 *   Query query = TenantAwareQuery.create(entityManager,
 *       "SELECT d.dept_name FROM dept_info d WHERE d.dept_id = :deptId AND d.tenant_id = :tenantId");
 *   query.setParameter("deptId", deptId);
 *   // 不需要手動 setParameter("tenantId", ...) — 已自動綁定
 * }</pre>
 *
 * <h3>情境 2：查詢全域表（如 tenant、permissions 等無 tenant_id 的表）</h3>
 * <pre>{@code
 *   // ✅ 明確聲明這是全域查詢，code review 時一目了然
 *   Query query = TenantAwareQuery.createGlobal(entityManager,
 *       "SELECT t.tenant_name FROM tenant t WHERE t.tenant_id = :tenantId");
 * }</pre>
 *
 * <h3>情境 3：System Context 下的跨租戶查詢（排程、auth 流程）</h3>
 * <pre>{@code
 *   // ✅ 在 TenantContext.setSystemContext() 下，create() 會跳過驗證
 *   TenantContext.setSystemContext();
 *   try {
 *       Query query = TenantAwareQuery.create(entityManager, sql);
 *   } finally {
 *       TenantContext.clear();
 *   }
 * }</pre>
 *
 * <h2>規則</h2>
 * <ul>
 *   <li><b>禁止</b>在業務程式碼中直接使用 {@code entityManager.createNativeQuery()}</li>
 *   <li>所有 native query 必須透過此工具類</li>
 *   <li>ArchUnit 測試會掃描違規使用（見 {@code NativeQueryArchTest}）</li>
 * </ul>
 *
 * @see com.taipei.iot.tenant.TenantContext
 * @see com.taipei.iot.tenant.TenantFilterAspect
 */
@Slf4j
public final class TenantAwareQuery {

    private TenantAwareQuery() {
    }

    /**
     * 建立租戶安全的 native query。
     *
     * <p>行為：</p>
     * <ul>
     *   <li>驗證 SQL 中包含 {@code tenant_id}，否則拋出 {@link IllegalArgumentException}</li>
     *   <li>自動從 {@link TenantContext} 取得當前租戶 ID 並綁定 {@code :tenantId} 參數</li>
     *   <li>如果 {@link TenantContext} 為 System Context，跳過驗證（允許跨租戶查詢）</li>
     *   <li>如果 {@link TenantContext} 未設定，拋出 {@link IllegalStateException}（fail-closed）</li>
     * </ul>
     *
     * @param em  EntityManager
     * @param sql Native SQL，必須包含 {@code tenant_id} 條件
     * @return 已綁定 {@code :tenantId} 的 Query 物件
     * @throws IllegalArgumentException 如果 SQL 不包含 {@code tenant_id}
     * @throws IllegalStateException    如果 TenantContext 未設定
     */
    public static Query create(EntityManager em, String sql) {
        // System Context（排程、auth 流程）：允許不帶 tenant_id 的查詢
        if (TenantContext.isSystemContext()) {
            log.debug("[TenantAwareQuery] System context — skipping tenant_id validation. SQL: {}",
                    truncateSql(sql));
            return em.createNativeQuery(sql);
        }

        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            // Fail-closed：沒有租戶上下文就拒絕查詢，防止資料外洩
            throw new IllegalStateException(
                    "[TenantAwareQuery] TenantContext is not set. " +
                    "All native queries require a tenant context. " +
                    "If this is a global query, use TenantAwareQuery.createGlobal() instead. " +
                    "SQL: " + truncateSql(sql));
        }

        // 驗證 SQL 中包含 tenant_id 條件
        // 為什麼用 contains 而不是 regex？因為 SQL 的寫法很多樣（別名、子查詢、JOIN 條件），
        // 用 contains 是最低限度的防護 — 確保開發者至少有「想到」tenant_id。
        // 真正的正確性還是需要 code review 確認 WHERE 條件的邏輯。
        String sqlLower = sql.toLowerCase();
        if (!sqlLower.contains("tenant_id")) {
            throw new IllegalArgumentException(
                    "[TenantAwareQuery] Native query 必須包含 tenant_id 條件！\n" +
                    "Hibernate @Filter 不會作用於 native query，不加 tenant_id 會導致跨租戶資料洩漏。\n" +
                    "如果查詢的表確實沒有 tenant_id（全域表），請改用 TenantAwareQuery.createGlobal()。\n" +
                    "SQL: " + truncateSql(sql));
        }

        Query query = em.createNativeQuery(sql);

        // 自動綁定 :tenantId 參數（如果 SQL 中有 :tenantId placeholder）
        if (sqlLower.contains(":tenantid")) {
            query.setParameter("tenantId", tenantId);
        }

        return query;
    }

    /**
     * 建立全域表的 native query（不需要 tenant_id 條件）。
     *
     * <p>適用於查詢沒有 {@code tenant_id} 欄位的全域表，例如：
     * {@code tenant}、{@code permissions} 等。</p>
     *
     * <p><b>注意</b>：使用此方法時，請在 code review 中確認查詢的表確實是全域表。
     * 如果表有 {@code tenant_id} 欄位卻使用此方法，等同於繞過租戶隔離。</p>
     *
     * @param em  EntityManager
     * @param sql Native SQL
     * @return Query 物件
     */
    public static Query createGlobal(EntityManager em, String sql) {
        // 防禦性檢查：如果全域查詢的 SQL 竟然包含 tenant_id，提醒開發者可能用錯方法
        if (sql.toLowerCase().contains("tenant_id")) {
            log.warn("[TenantAwareQuery] createGlobal() 被用於包含 tenant_id 的 SQL。" +
                    "如果此表有 tenant_id 欄位，請改用 TenantAwareQuery.create()。SQL: {}",
                    truncateSql(sql));
        }
        return em.createNativeQuery(sql);
    }

    /**
     * 截斷過長的 SQL 避免 log 爆炸。
     */
    private static String truncateSql(String sql) {
        if (sql == null) {
            return "null";
        }
        return sql.length() > 200 ? sql.substring(0, 200) + "..." : sql;
    }
}
