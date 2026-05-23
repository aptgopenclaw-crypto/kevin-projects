package com.taipei.iot.tender.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tender.award-scraper")
public class TenderAwardScraperProperties {

    /** 每次搜尋請求之間的延遲（毫秒）。建議 >= 2000。 */
    private long requestDelayMs = 2000;

    /** Playwright 頁面操作逾時（毫秒）。 */
    private long pageTimeoutMs = 30000;

    /** 採購性質過濾清單。空清單 = 不過濾（全部納入）。 */
    private List<String> procurementTypeFilter = List.of("工程", "財物", "勞務");
}
