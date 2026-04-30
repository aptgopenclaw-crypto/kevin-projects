package com.taipei.iot.kpi.collector;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 從材料模組收集 KPI 數據。
 * <p>指標: 材料耗用量</p>
 * <p>使用 native query 避免修改既有 repository。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialDataCollector implements KpiDataCollector {

    private final EntityManager entityManager;

    @Override
    public Map<String, BigDecimal> collect(LocalDate date) {
        Map<String, BigDecimal> result = new HashMap<>();

        try {
            // 當月材料領用總量
            Number totalIssued = (Number) entityManager.createNativeQuery(
                    "SELECT COALESCE(SUM(quantity), 0) FROM issue_items ii " +
                    "JOIN issue_requests ir ON ii.issue_request_id = ir.id " +
                    "WHERE EXTRACT(YEAR FROM ir.created_at) = :y AND EXTRACT(MONTH FROM ir.created_at) = :m")
                    .setParameter("y", date.getYear())
                    .setParameter("m", date.getMonthValue())
                    .getSingleResult();
            if (totalIssued != null && totalIssued.longValue() > 0) {
                result.put("MATERIAL_USAGE", BigDecimal.valueOf(totalIssued.longValue()));
            }
        } catch (Exception e) {
            log.error("MaterialDataCollector 收集失敗: {}", e.getMessage(), e);
        }

        return result;
    }

    @Override
    public String getSourceName() {
        return "MATERIAL";
    }
}
