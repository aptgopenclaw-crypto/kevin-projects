package com.taipei.iot.kpi.scheduler;

import com.taipei.iot.kpi.service.KpiCalculationService;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * KPI 月度計算排程。
 * 每月 1 日 02:00 自動計算上月所有指標。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KpiCalculationJob {

    private final KpiCalculationService calculationService;

    @Scheduled(cron = "0 0 2 1 * *")
    public void calculateMonthly() {
        log.info("KpiCalculationJob: 開始月度計算");
        TenantContext.setSystemContext();
        try {
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            int year = lastMonth.getYear();
            int month = lastMonth.getMonthValue();

            // TODO: 多租戶環境需遍歷所有 tenant
            int count = calculationService.calculateMonthly(year, month, "default");
            log.info("KpiCalculationJob: 完成 (period={}/{}, results={})", year, month, count);
        } catch (Exception e) {
            log.error("KpiCalculationJob 執行失敗: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
