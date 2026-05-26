package com.taipei.iot.tender.job;

import com.taipei.iot.tender.dto.AwardScrapeResult;
import com.taipei.iot.tender.service.TenderAwardScraperService;
import com.taipei.iot.tender.service.TenderMailService;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.tenant.TenantEntity;
import com.taipei.iot.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 決標公告定期爬蟲排程。
 * 預設：每個工作日上午 8:30 執行（招標爬蟲結束後）。
 *
 * 執行流程：
 *   1. 遍歷所有啟用的租戶
 *   2. 以各租戶的關鍵字設定，搜尋政府採購網決標公告
 *   3. 爬取結果寫入該租戶的 tender_award 資料
 *
 * 可透過 application.yml 調整：
 * <pre>
 * tender:
 *   award-scraper:
 *     cron: "0 30 8 * * MON-FRI"
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenderAwardScrapeJob {

    private final TenderAwardScraperService scraperService;
    private final TenderMailService mailService;
    private final TenantRepository tenantRepository;

    @Scheduled(cron = "${tender.award-scraper.cron:0 0 20 * * MON-FRI}")
    @SchedulerLock(name = "TenderAwardScrapeJob", lockAtLeastFor = "5m", lockAtMostFor = "60m")
    public void execute() {
        log.info("[TenderAwardScrapeJob] 排程開始執行");

        List<TenantEntity> tenants = tenantRepository.findByEnabledTrue();
        log.info("[TenderAwardScrapeJob] 共 {} 個啟用租戶", tenants.size());

        for (TenantEntity tenant : tenants) {
            String tenantId = tenant.getTenantId();
            TenantContext.setCurrentTenantId(tenantId);
            try {
                log.info("[TenderAwardScrapeJob] 開始處理租戶: {}", tenantId);
                AwardScrapeResult result = scraperService.runAndImport();
                log.info("[TenderAwardScrapeJob] 租戶 {} 完成，寫入 {} 筆，開始寄送日報", tenantId, result.total());
                mailService.sendAwardReport(result);
            } catch (Exception e) {
                log.error("[TenderAwardScrapeJob] 租戶 {} 執行失敗", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("[TenderAwardScrapeJob] 排程執行完畢");
    }
}
