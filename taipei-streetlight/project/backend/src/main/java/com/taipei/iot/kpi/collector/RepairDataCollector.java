package com.taipei.iot.kpi.collector;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 從報修/資產模組收集 KPI 數據。
 * <p>指標: 照明妥善率、維修完成率</p>
 * <p>使用 native query 避免修改既有 repository。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairDataCollector implements KpiDataCollector {

    private final EntityManager entityManager;

    @Override
    public Map<String, BigDecimal> collect(LocalDate date) {
        Map<String, BigDecimal> result = new HashMap<>();

        try {
            // 照明妥善率: ACTIVE 設備數 / 總設備數 * 100
            Number total = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM devices").getSingleResult();
            if (total.longValue() > 0) {
                Number active = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM devices WHERE status = 'ACTIVE'").getSingleResult();
                BigDecimal rate = BigDecimal.valueOf(active.longValue())
                        .divide(BigDecimal.valueOf(total.longValue()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                result.put("LIGHTING_AVAILABILITY", rate);
            }

            // 維修完成率: 當月已關閉 / 當月總工單 * 100
            int year = date.getYear();
            int month = date.getMonthValue();
            Number totalTickets = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM repair_tickets WHERE EXTRACT(YEAR FROM created_at) = :y AND EXTRACT(MONTH FROM created_at) = :m")
                    .setParameter("y", year).setParameter("m", month).getSingleResult();
            if (totalTickets.longValue() > 0) {
                Number closed = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM repair_tickets WHERE EXTRACT(YEAR FROM created_at) = :y AND EXTRACT(MONTH FROM created_at) = :m AND status = 'CLOSED'")
                        .setParameter("y", year).setParameter("m", month).getSingleResult();
                BigDecimal completionRate = BigDecimal.valueOf(closed.longValue())
                        .divide(BigDecimal.valueOf(totalTickets.longValue()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                result.put("REPAIR_COMPLETION_RATE", completionRate);
            }
        } catch (Exception e) {
            log.error("RepairDataCollector 收集失敗: {}", e.getMessage(), e);
        }

        return result;
    }

    @Override
    public String getSourceName() {
        return "REPAIR";
    }
}
