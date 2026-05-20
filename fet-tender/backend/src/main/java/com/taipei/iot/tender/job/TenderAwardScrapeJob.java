package com.taipei.iot.tender.job;

import com.taipei.iot.tender.dto.AwardScrapeResult;
import com.taipei.iot.tender.service.TenderAwardScraperService;
import com.taipei.iot.tender.service.TenderMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 決標公告定期爬蟲排程。
 * 預設：每個工作日上午 8:30 執行（招標爬蟲結束後）。
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

    @Scheduled(cron = "${tender.award-scraper.cron:0 30 19 * * MON-FRI}")
    public void execute() {
        log.info("[TenderAwardScrapeJob] 排程開始執行");
        try {
            AwardScrapeResult result = scraperService.runAndImport();
            log.info("[TenderAwardScrapeJob排程執行完成，寫入 {} 筆，開始寄送日報", result.total());
            mailService.sendAwardReport(result);
        } catch (Exception e) {
            log.error("[TenderAwardScrapeJob] 排程執行失敗", e);
        }
    }
}
