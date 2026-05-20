package com.taipei.iot.tender.job;

import com.taipei.iot.tender.dto.TenderScrapeResult;
import com.taipei.iot.tender.service.TenderMailService;
import com.taipei.iot.tender.service.TenderScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 招標公告定期爬蟲排程。
 * 預設：每個工作日上午 8:00 執行（可透過 application.yml 調整 cron 表達式）。
 *
 * 執行流程：
 *   1. 以 Playwright 啟動 Chromium，搜尋政府採購網招標公告
 *   2. 依 announcement_search_keywords / announcement_agency_filters 設定進行關鍵字與機關過濾
 *   3. 進入各標案詳細頁（自動破解撲克牌 CAPTCHA），以 Jsoup 解析詳細資料
 *   4. 解析並 upsert 至 tender_announcement 表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenderScrapeJob {

    private final TenderScraperService scraperService;
    private final TenderMailService mailService;

    /**
     * 每個工作日 08:00 執行。
     * 若需修改排程時間，在 application.yml 覆蓋：
     * <pre>
     * tender:
     *   scraper:
     *     cron: "0 0 8 * * MON-FRI"
     * </pre>
     */
    @Scheduled(cron = "${tender.scraper.cron:0 30 18 * * MON-FRI}")
    public void execute() {
        log.info("[TenderScrapeJob] 排程開始執行");
        try {
            TenderScrapeResult result = scraperService.runAndImport();
            log.info("[TenderScrapeJob] 排程執行完成，寫入 {} 筆，開始寄送日報", result.total());
            mailService.sendReport(result);
        } catch (Exception e) {
            log.error("[TenderScrapeJob] 排程執行失敗", e);
        }
    }
}
