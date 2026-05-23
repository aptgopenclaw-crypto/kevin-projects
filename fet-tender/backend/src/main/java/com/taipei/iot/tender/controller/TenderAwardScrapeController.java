package com.taipei.iot.tender.controller;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.AwardScrapeResult;
import com.taipei.iot.tender.service.TenderAwardScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/v1/tender/award-scrape")
@RequiredArgsConstructor
public class TenderAwardScrapeController {

    private static final Duration COOLDOWN = Duration.ofMinutes(10);
    private final AtomicReference<Instant> lastTrigger = new AtomicReference<>(Instant.EPOCH);

    private final TenderAwardScraperService scraperService;

    /**
     * 手動觸發決標爬蟲（管理員用）。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('tender:award:scrape:run')")
    public BaseResponse<Map<String, Object>> triggerScrape() {
        Instant now = Instant.now();
        Instant last = lastTrigger.get();
        if (Duration.between(last, now).compareTo(COOLDOWN) < 0) {
            return BaseResponse.fail(ErrorCode.RATE_LIMIT_EXCEEDED,
                    "決標爬蟲冷卻中，請 " + COOLDOWN.toMinutes() + " 分鐘後再試");
        }
        lastTrigger.set(now);

        try {
            AwardScrapeResult result = scraperService.runAndImport();
            return BaseResponse.success(Map.of(
                    "message", "決標爬蟲執行成功",
                    "importedCount", result.total()
            ));
        } catch (Exception e) {
            log.error("[AwardScraper] 手動觸發失敗", e);
            return BaseResponse.fail(ErrorCode.UNKNOWN_ERROR, "決標爬蟲執行失敗: " + e.getMessage());
        }
    }
}
