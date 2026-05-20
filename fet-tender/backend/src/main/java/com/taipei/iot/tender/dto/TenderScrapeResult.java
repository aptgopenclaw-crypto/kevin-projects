package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.TenderAnnouncement;

import java.util.List;
import java.util.Map;

/**
 * 爬蟲執行結果，傳遞給後續的 Excel 匯出與郵件寄送流程。
 *
 * @param total              本次新增/更新的總筆數
 * @param solutionKeyCounts  solution → (關鍵字/機關名稱 → 筆數)，用於產生郵件摘要表格
 * @param announcements      本次抓到的所有公告（含詳細資料），用於產生 Excel 附件
 */
public record TenderScrapeResult(
        int total,
        Map<String, Map<String, Integer>> solutionKeyCounts,
        List<TenderAnnouncement> announcements
) {}
