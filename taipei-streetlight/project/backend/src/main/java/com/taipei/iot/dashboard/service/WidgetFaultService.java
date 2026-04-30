package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.FaultCategoryResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WidgetFaultService {

    @PersistenceContext
    private EntityManager em;

    /**
     * 故障熱力圖資料 — 依行政區分組回傳故障計數 + 經緯度中心點
     * (Phase 5C PostGIS 完成後可改為 ST_Centroid 空間聚合)
     */
    public Map<String, Object> getHeatmapData(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getCurrentTenantId();

        String sql = "SELECT d.lat, d.lng, COUNT(ft.id) AS fault_count " +
                "FROM fault_tickets ft " +
                "JOIN devices d ON ft.device_id = d.id " +
                "WHERE ft.tenant_id = :tenantId " +
                "AND d.lat IS NOT NULL AND d.lng IS NOT NULL";
        if (startDate != null) sql += " AND ft.reported_at >= :start";
        if (endDate != null) sql += " AND ft.reported_at < :end";
        sql += " GROUP BY d.lat, d.lng";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);
        if (startDate != null) q.setParameter("start", startDate.atStartOfDay());
        if (endDate != null) q.setParameter("end", endDate.plusDays(1).atStartOfDay());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<Map<String, Object>> features = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> feature = new HashMap<>();
            feature.put("lat", r[0]);
            feature.put("lng", r[1]);
            feature.put("count", ((Number) r[2]).longValue());
            features.add(feature);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("type", "heatmap");
        result.put("points", features);
        result.put("total", features.size());
        return result;
    }

    public FaultCategoryResponse getCategoryStats(LocalDate startDate, LocalDate endDate) {
        String tenantId = TenantContext.getCurrentTenantId();

        String sql = "SELECT COALESCE(fault_category, '未分類') AS cat, COUNT(*) " +
                "FROM repair_tickets WHERE tenant_id = :tenantId";
        if (startDate != null) sql += " AND reported_at >= :start";
        if (endDate != null) sql += " AND reported_at < :end";
        sql += " GROUP BY cat ORDER BY COUNT(*) DESC";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);
        if (startDate != null) q.setParameter("start", startDate.atStartOfDay());
        if (endDate != null) q.setParameter("end", endDate.plusDays(1).atStartOfDay());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<FaultCategoryResponse.CategoryItem> items = new ArrayList<>();
        for (Object[] r : rows) {
            long count = ((Number) r[1]).longValue();
            items.add(FaultCategoryResponse.CategoryItem.builder()
                    .category((String) r[0])
                    .count(count)
                    .percentage(total > 0
                            ? BigDecimal.valueOf(count * 100.0 / total).setScale(1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO)
                    .build());
        }

        return FaultCategoryResponse.builder().categories(items).build();
    }
}
