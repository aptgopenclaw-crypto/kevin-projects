package com.taipei.iot.tender.job;

import com.taipei.iot.tender.dto.TenderScrapeResult;
import com.taipei.iot.tender.service.TenderMailService;
import com.taipei.iot.tender.service.TenderScraperService;
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
 * 招標公告定期爬蟲排程。
 * 預設：每個工作日上午 8:00 執行（可透過 application.yml 調整 cron 表達式）。
 *
 * 執行流程：
 *   1. 遍歷所有啟用的租戶
 *   2. 以各租戶的關鍵字設定，搜尋政府採購網招標公告
 *   3. 爬取結果寫入該租戶的 tender_announcement 資料
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenderScrapeJob {

    private final TenderScraperService scraperService;
    private final TenderMailService mailService;
    private final TenantRepository tenantRepository;

    /**
     * 每個工作日 08:00 執行。
     * 若需修改排程時間，在 application.yml 覆蓋：
     * <pre>
     * tender:
     *   scraper:
     *     cron: "0 0 8 * * MON-FRI"
     * </pre>
     */
    @Scheduled(cron = "${tender.scraper.cron:0 00 21 * * MON-FRI}")
    @SchedulerLock(name = "TenderScrapeJob", lockAtLeastFor = "5m", lockAtMostFor = "60m")
    public void execute() {
        log.info("[TenderScrapeJob] 排程開始執行");

        List<TenantEntity> tenants = tenantRepository.findByEnabledTrue();
        log.info("[TenderScrapeJob] 共 {} 個啟用租戶", tenants.size());

        for (TenantEntity tenant : tenants) {
            String tenantId = tenant.getTenantId();
            TenantContext.setCurrentTenantId(tenantId);
            try {
                log.info("[TenderScrapeJob] 開始處理租戶: {}", tenantId);
                TenderScrapeResult result = scraperService.runAndImport();
                log.info("[TenderScrapeJob] 租戶 {} 完成，寫入 {} 筆，開始寄送日報", tenantId, result.total());
                mailService.sendReport(result);
            } catch (Exception e) {
                log.error("[TenderScrapeJob] 租戶 {} 執行失敗", tenantId, e);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("[TenderScrapeJob] 排程執行完畢");
    }
}
