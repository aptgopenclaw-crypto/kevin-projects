package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.AwardScrapeResult;
import com.taipei.iot.tender.service.TenderAwardScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/tender/award-scrape")
@RequiredArgsConstructor
public class TenderAwardScrapeController {

    private final TenderAwardScraperService scraperService;

    /**
     * 手動觸發決標爬蟲（管理員用）。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('tender:award:scrape:run')")
    public BaseResponse<Map<String, Object>> triggerScrape() {
        try {
            AwardScrapeResult result = scraperService.runAndImport();
            return BaseResponse.success(Map.of(
                    "message", "決標爬蟲執行成功",
                    "importedCount", result.total()
            ));
        } catch (Exception e) {
            log.error("[AwardScraper] 手動觸發失敗", e);
            return BaseResponse.success(Map.of(
                    "message", "決標爬蟲執行失敗: " + e.getMessage(),
                    "importedCount", 0
            ));
        }
    }
}
