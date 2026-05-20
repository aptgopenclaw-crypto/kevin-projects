package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.TenderAward;

import java.util.List;
import java.util.Map;

/**
 * 決標爬蟲執行結果。
 *
 * @param total              本次新增/更新的總廠商列數
 * @param solutionKeyCounts  solution → (關鍵字/機關名稱 → 筆數)，用於產生郵件摘要
 * @param awards             本次抓到的所有決標記錄（含詳細資料）
 */
public record AwardScrapeResult(
        int total,
        Map<String, Map<String, Integer>> solutionKeyCounts,
        List<TenderAward> awards
) {}
