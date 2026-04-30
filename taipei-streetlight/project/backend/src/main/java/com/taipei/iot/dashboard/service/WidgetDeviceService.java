package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.LampCountResponse;
import com.taipei.iot.dashboard.dto.LampStatusResponse;
import com.taipei.iot.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WidgetDeviceService {

    @PersistenceContext
    private EntityManager em;

    public LampCountResponse getLampCount(String district, Long contractId) {
        String tenantId = TenantContext.getCurrentTenantId();

        // 總盞數
        String totalSql = "SELECT COUNT(*) FROM devices WHERE tenant_id = :tenantId " +
                "AND device_type IN ('LUMINAIRE', 'POLE')";
        if (contractId != null) totalSql += " AND contract_id = :contractId";

        var q = em.createNativeQuery(totalSql);
        q.setParameter("tenantId", tenantId);
        if (contractId != null) q.setParameter("contractId", contractId);
        long total = ((Number) q.getSingleResult()).longValue();

        // 依廠商分組
        Map<String, Long> byContractor = groupByColumn(tenantId,
                "SELECT COALESCE(c.contractor_name, '未指定') AS name, COUNT(d.id) " +
                "FROM devices d LEFT JOIN contracts c ON d.contract_id = c.id " +
                "WHERE d.tenant_id = :tenantId AND d.device_type IN ('LUMINAIRE', 'POLE') " +
                "GROUP BY name ORDER BY COUNT(d.id) DESC");

        // 依設備類型分組
        Map<String, Long> byType = groupByColumn(tenantId,
                "SELECT device_type, COUNT(*) FROM devices WHERE tenant_id = :tenantId " +
                "AND device_type IN ('LUMINAIRE', 'POLE', 'CONTROLLER') " +
                "GROUP BY device_type ORDER BY COUNT(*) DESC");

        // 依光源分組 (attributes->>'light_source')
        Map<String, Long> byLightSource = groupByColumn(tenantId,
                "SELECT COALESCE(attributes->>'light_source', '未知') AS ls, COUNT(*) " +
                "FROM devices WHERE tenant_id = :tenantId AND device_type = 'LUMINAIRE' " +
                "GROUP BY ls ORDER BY COUNT(*) DESC");

        // 依設施分類 (路燈/園燈/其他)
        Map<String, Long> byFacilityType = groupByColumn(tenantId,
                "SELECT COALESCE(attributes->>'facility_type', '路燈') AS ft, COUNT(*) " +
                "FROM devices WHERE tenant_id = :tenantId AND device_type = 'LUMINAIRE' " +
                "GROUP BY ft ORDER BY COUNT(*) DESC");

        return LampCountResponse.builder()
                .total(total)
                .byContractor(byContractor)
                .byType(byType)
                .byLightSource(byLightSource)
                .byFacilityType(byFacilityType)
                .build();
    }

    public LampStatusResponse getLampStatus() {
        String tenantId = TenantContext.getCurrentTenantId();

        String sql = "SELECT " +
                "COUNT(*) FILTER (WHERE status = 'ACTIVE') AS online, " +
                "COUNT(*) FILTER (WHERE status != 'ACTIVE') AS offline, " +
                "COUNT(*) AS total " +
                "FROM devices WHERE tenant_id = :tenantId " +
                "AND device_type IN ('LUMINAIRE', 'POLE')";

        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);

        Object[] row = (Object[]) q.getSingleResult();
        long online = ((Number) row[0]).longValue();
        long offline = ((Number) row[1]).longValue();
        long totalD = ((Number) row[2]).longValue();

        return LampStatusResponse.builder()
                .online(online)
                .offline(offline)
                .onlineRate(totalD > 0
                        ? BigDecimal.valueOf(online * 100.0 / totalD).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Long> groupByColumn(String tenantId, String sql) {
        var q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] r : rows) {
            map.put(String.valueOf(r[0]), ((Number) r[1]).longValue());
        }
        return map;
    }
}
