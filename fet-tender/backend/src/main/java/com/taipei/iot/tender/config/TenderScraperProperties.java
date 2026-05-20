package com.taipei.iot.tender.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tender.scraper")
public class TenderScraperProperties {

    /**
     * 每次搜尋請求之間的延遲（毫秒），避免對政府採購網造成過大壓力。
     * 建議 >= 2000。
     */
    private long requestDelayMs = 2000;

    /**
     * Playwright 頁面操作逾時（毫秒）。
     */
    private long pageTimeoutMs = 30000;

    /**
     * 排程執行時間（Spring cron 格式）。
     * 預設：每個工作日上午 8:00。
     */
    private String cron = "0 0 8 * * MON-FRI";

    /**
     * 採購性質過濾清單（對應 Python FILTER_PROCUREMENT_TYPE）。
     * 空清單 = 不過濾（全部納入）。
     * 預設：工程、財物、勞務。3 種採購性質。
     */
    private List<String> procurementTypeFilter = List.of("工程", "財物", "勞務");
}
