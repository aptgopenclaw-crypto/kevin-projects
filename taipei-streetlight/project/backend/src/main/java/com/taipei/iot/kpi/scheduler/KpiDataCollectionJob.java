package com.taipei.iot.kpi.scheduler;

import com.taipei.iot.kpi.service.KpiDataService;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * KPI 數據收集排程。
 * 每日 01:00 自動從各模組收集前一天的 KPI 原始數據。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KpiDataCollectionJob {

    private final KpiDataService kpiDataService;

    @Scheduled(cron = "0 0 1 * * *")
    public void collectDaily() {
        log.info("KpiDataCollectionJob: 開始每日數據收集");
        TenantContext.setSystemContext();
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            // TODO: 多租戶環境需遍歷所有 tenant，目前使用預設租戶
            kpiDataService.autoCollect(yesterday, "default");
            log.info("KpiDataCollectionJob: 完成 (date={})", yesterday);
        } catch (Exception e) {
            log.error("KpiDataCollectionJob 執行失敗: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
