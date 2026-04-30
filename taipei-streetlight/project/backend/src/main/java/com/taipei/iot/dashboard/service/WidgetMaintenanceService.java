package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.MaintenanceStatsResponse;
import com.taipei.iot.dashboard.dto.MaintenanceTrendResponse;
import com.taipei.iot.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WidgetMaintenanceService {

    @PersistenceContext
    private EntityManager em;

    public MaintenanceStatsResponse getStats(LocalDate startDate, LocalDate endDate, Long contractId) {
        String tenantId = TenantContext.getCurrentTenantId();

        // 報修統計
        String baseSql = "SELECT " +
                "COUNT(*) AS total, " +
                "COUNT(*) FILTER (WHERE status = 'CLOSED') AS completed, " +
                "COUNT(*) FILTER (WHERE status NOT IN ('CLOSED')) AS pending " +
                "FROM repair_tickets WHERE tenant_id = :tenantId";

        StringBuilder sb = new StringBuilder(baseSql);
        if (startDate != null) sb.append(" AND reported_at >= :startDate");
        if (endDate != null) sb.append(" AND reported_at < :endDate");
        if (contractId != null) sb.append(" AND contract_id = :contractId");

        var q = em.createNativeQuery(sb.toString());
        q.setParameter("tenantId", tenantId);
        if (startDate != null) q.setParameter("startDate", startDate.atStartOfDay());
        if (endDate != null) q.setParameter("endDate", endDate.plusDays(1).atStartOfDay());
        if (contractId != null) q.setParameter("contractId", contractId);

        Object[] row = (Object[]) q.getSingleResult();
        long total = ((Number) row[0]).longValue();
        long completed = ((Number) row[1]).longValue();
        long pending = ((Number) row[2]).longValue();
        BigDecimal rate = total > 0
                ? BigDecimal.valueOf(completed * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 平均修復時間
        BigDecimal avgHours = getAvgRepairHours(tenantId, startDate, endDate, contractId);

        // 來源分布
        Map<String, Long> sourceDist = getDistribution(tenantId, "source", startDate, endDate, contractId);

        // 故障分類分布
        Map<String, Long> faultDist = getDistribution(tenantId, "fault_category", startDate, endDate, contractId);

        // 照明妥善率
        BigDecimal illuminationRate = getIlluminationRate(tenantId, contractId);

        return MaintenanceStatsResponse.builder()
                .totalRepairs(total)
                .completedRepairs(completed)
                .pendingRepairs(pending)
                .completionRate(rate)
                .avgRepairHours(avgHours)
                .illuminationRate(illuminationRate)
                .sourceDistribution(sourceDist)
                .faultCategoryDistribution(faultDist)
                .build();
    }

    public MaintenanceTrendResponse getTrend(LocalDate startDate, LocalDate endDate, Long contractId) {
        String tenantId = TenantContext.getCurrentTenantId();

        String sql = "SELECT to_char(reported_at, 'YYYY-MM') AS month, " +
                "COUNT(*) AS total, " +
                "COUNT(*) FILTER (WHERE status = 'CLOSED') AS completed " +
                "FROM repair_tickets WHERE tenant_id = :tenantId " +
                "AND reported_at >= :startDate AND reported_at < :endDate";
        if (contractId != null) sql += " AND contract_id = :contractId";
        sql += " GROUP BY month ORDER BY month";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);
        q.setParameter("startDate", startDate.atStartOfDay());
        q.setParameter("endDate", endDate.plusDays(1).atStartOfDay());
        if (contractId != null) q.setParameter("contractId", contractId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<MaintenanceTrendResponse.MonthlyPoint> points = new ArrayList<>();
        for (Object[] r : rows) {
            long t = ((Number) r[1]).longValue();
            long c = ((Number) r[2]).longValue();
            points.add(MaintenanceTrendResponse.MonthlyPoint.builder()
                    .month((String) r[0])
                    .repairCount(t)
                    .completionRate(t > 0
                            ? BigDecimal.valueOf(c * 100.0 / t).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO)
                    .build());
        }

        return MaintenanceTrendResponse.builder().months(points).build();
    }

    private BigDecimal getAvgRepairHours(String tenantId, LocalDate start, LocalDate end, Long contractId) {
        String sql = "SELECT AVG(EXTRACT(EPOCH FROM (completed_at - reported_at)) / 3600) " +
                "FROM repair_tickets WHERE tenant_id = :tenantId AND completed_at IS NOT NULL";
        if (start != null) sql += " AND reported_at >= :start";
        if (end != null) sql += " AND reported_at < :end";
        if (contractId != null) sql += " AND contract_id = :contractId";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);
        if (start != null) q.setParameter("start", start.atStartOfDay());
        if (end != null) q.setParameter("end", end.plusDays(1).atStartOfDay());
        if (contractId != null) q.setParameter("contractId", contractId);

        Object result = q.getSingleResult();
        return result != null
                ? new BigDecimal(result.toString()).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private Map<String, Long> getDistribution(String tenantId, String column,
                                               LocalDate start, LocalDate end, Long contractId) {
        String sql = "SELECT COALESCE(" + column + ", '未分類') AS cat, COUNT(*) " +
                "FROM repair_tickets WHERE tenant_id = :tenantId";
        if (start != null) sql += " AND reported_at >= :start";
        if (end != null) sql += " AND reported_at < :end";
        if (contractId != null) sql += " AND contract_id = :contractId";
        sql += " GROUP BY cat ORDER BY COUNT(*) DESC";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);
        if (start != null) q.setParameter("start", start.atStartOfDay());
        if (end != null) q.setParameter("end", end.plusDays(1).atStartOfDay());
        if (contractId != null) q.setParameter("contractId", contractId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] r : rows) {
            map.put((String) r[0], ((Number) r[1]).longValue());
        }
        return map;
    }

    private BigDecimal getIlluminationRate(String tenantId, Long contractId) {
        String sql = "SELECT " +
                "COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active, " +
                "COUNT(*) AS total " +
                "FROM devices WHERE tenant_id = :tenantId AND device_type IN ('LUMINAIRE', 'POLE')";
        if (contractId != null) sql += " AND contract_id = :contractId";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);
        if (contractId != null) q.setParameter("contractId", contractId);

        Object[] row = (Object[]) q.getSingleResult();
        long active = ((Number) row[0]).longValue();
        long totalDevices = ((Number) row[1]).longValue();
        return totalDevices > 0
                ? BigDecimal.valueOf(active * 100.0 / totalDevices).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
}
