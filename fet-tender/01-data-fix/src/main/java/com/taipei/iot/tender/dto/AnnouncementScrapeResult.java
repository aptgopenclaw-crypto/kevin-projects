package com.taipei.iot.tender.dto;

import java.util.Map;

/**
 * 招標公告爬蟲執行結果。
 *
 * @param total              本次新增/更新的總筆數
 * @param solutionKeyCounts  solution → (關鍵字/機關名稱 → 筆數)
 */
public record AnnouncementScrapeResult(
        int total,
        Map<String, Map<String, Integer>> solutionKeyCounts
) {}
