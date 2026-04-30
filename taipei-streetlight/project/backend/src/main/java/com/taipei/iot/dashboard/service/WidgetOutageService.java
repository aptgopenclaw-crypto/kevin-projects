package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.OutageAlertResponse;
import com.taipei.iot.dashboard.dto.OutageTrendResponse;
import com.taipei.iot.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WidgetOutageService {

    @PersistenceContext
    private EntityManager em;

    public OutageAlertResponse getCurrentOutages() {
        String tenantId = TenantContext.getCurrentTenantId();

        String sql = "SELECT root_cause_type, affected_count, detected_at " +
                "FROM fault_correlations " +
                "WHERE tenant_id = :tenantId AND status = 'ACTIVE' AND root_cause_type = 'POWER_OUTAGE' " +
                "ORDER BY detected_at DESC";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<OutageAlertResponse.OutageZone> zones = new ArrayList<>();
        for (Object[] r : rows) {
            zones.add(OutageAlertResponse.OutageZone.builder()
                    .zone((String) r[0])
                    .affectedCount(((Number) r[1]).intValue())
                    .since(r[2] instanceof java.sql.Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) r[2])
                    .build());
        }

        return OutageAlertResponse.builder()
                .currentOutageCount(zones.size())
                .outageZones(zones)
                .build();
    }

    public OutageTrendResponse getOutageTrend(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getCurrentTenantId();

        String sql = "SELECT to_char(detected_at, 'YYYY-MM') AS month, " +
                "COUNT(*) AS cnt, " +
                "AVG(EXTRACT(EPOCH FROM (COALESCE(resolved_at, now()) - detected_at)) / 3600) AS avg_hrs " +
                "FROM fault_correlations " +
                "WHERE tenant_id = :tenantId AND root_cause_type = 'POWER_OUTAGE' " +
                "AND detected_at >= :start AND detected_at < :end " +
                "GROUP BY month ORDER BY month";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);
        q.setParameter("start", startDate.atStartOfDay());
        q.setParameter("end", endDate.plusDays(1).atStartOfDay());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<OutageTrendResponse.MonthlyOutage> months = new ArrayList<>();
        for (Object[] r : rows) {
            months.add(OutageTrendResponse.MonthlyOutage.builder()
                    .month((String) r[0])
                    .outageCount(((Number) r[1]).intValue())
                    .avgRecoveryHours(r[2] != null
                            ? new BigDecimal(r[2].toString()).setScale(1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO)
                    .build());
        }

        return OutageTrendResponse.builder().months(months).build();
    }
}
