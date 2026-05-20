package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.TenderScrapeResult;
import com.taipei.iot.tender.service.TenderScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/tender/scrape")
@RequiredArgsConstructor
public class TenderScrapeController {

    private final TenderScraperService scraperService;

    /**
     * 手動觸發爬蟲（管理員用）。
     * 正常情況由排程自動執行；此端點用於即時補爬或測試。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('tender:scrape:run')")
    public BaseResponse<Map<String, Object>> triggerScrape() {
        try {
            TenderScrapeResult result = scraperService.runAndImport();
            return BaseResponse.success(Map.of(
                    "message", "爬蟲執行成功",
                    "importedCount", result.total()
            ));
        } catch (Exception e) {
            log.error("[TenderScraper] 手動觸發失敗", e);
            return BaseResponse.success(Map.of(
                    "message", "爬蟲執行失敗: " + e.getMessage(),
                    "importedCount", 0
            ));
        }
    }
}
