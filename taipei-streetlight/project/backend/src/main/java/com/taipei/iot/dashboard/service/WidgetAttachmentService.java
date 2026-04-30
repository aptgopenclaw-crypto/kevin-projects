package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.AttachmentStatsResponse;
import com.taipei.iot.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WidgetAttachmentService {

    @PersistenceContext
    private EntityManager em;

    public AttachmentStatsResponse getStats() {
        String tenantId = TenantContext.getCurrentTenantId();

        // 總計
        String totalSql = "SELECT COUNT(*), COALESCE(SUM(file_size), 0) " +
                "FROM ticket_attachments WHERE tenant_id = :tenantId";

        var q = em.createNativeQuery(totalSql);
        q.setParameter("tenantId", tenantId);
        Object[] row = (Object[]) q.getSingleResult();
        long totalCount = ((Number) row[0]).longValue();
        BigDecimal totalBytes = new BigDecimal(row[1].toString());
        BigDecimal totalSizeMB = totalBytes.divide(BigDecimal.valueOf(1024 * 1024), 2, RoundingMode.HALF_UP);

        // 依類型分組
        String typeSql = "SELECT COALESCE(file_type, '其他') AS ft, COUNT(*) " +
                "FROM ticket_attachments WHERE tenant_id = :tenantId " +
                "GROUP BY ft ORDER BY COUNT(*) DESC";

        var q2 = em.createNativeQuery(typeSql);
        q2.setParameter("tenantId", tenantId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q2.getResultList();
        Map<String, Long> byType = new LinkedHashMap<>();
        for (Object[] r : rows) {
            byType.put((String) r[0], ((Number) r[1]).longValue());
        }

        return AttachmentStatsResponse.builder()
                .totalCount(totalCount)
                .totalSizeMB(totalSizeMB)
                .byType(byType)
                .build();
    }
}
