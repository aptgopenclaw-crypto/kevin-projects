package com.taipei.iot.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 在每一次 Repository 方法呼叫前啟用 Hibernate tenantFilter。
     * <p>
     * 只有實作 {@link TenantScopedRepository} 的 Repository（即操作具有
     * {@code @Filter(name="tenantFilter")} 之實體的 Repository）才受此 Aspect 管控。
     * 全域實體（如 {@code UserEntity}、{@code TenantEntity}）的 Repository 不受影響。
     */
    @Before("execution(* com.taipei.iot..*.repository..*Repository.*(..))")
    public void enableTenantFilter(JoinPoint jp) {
        // 只處理 TenantScopedRepository — 全域 Repository 直接放行
        if (!(jp.getThis() instanceof TenantScopedRepository)) {
            return;
        }

        if (TenantContext.isSystemContext()) {
            // 系統上下文（排程 / async 寫入）：不套 tenantFilter，允許跨租戶操作
            return;
        }
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            // Fail-closed：未設定 TenantContext 時拒絕查詢，防止資料外洩
            throw new IllegalStateException(
                    "TenantContext is not set. All repository operations require a tenant context.");
        }
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter")
               .setParameter("tenantId", tenantId);
    }
}
