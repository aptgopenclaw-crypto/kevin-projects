package com.taipei.iot.tender.scraper;

/**
 * 政府採購網決標查詢列表頁單筆資料（內部 DTO）。
 * 欄位對應 readTenderAgent 列表頁 table 的各 td 欄位。
 *
 * 欄位順序（0-based td index）：
 *   0 = 序號
 *   1 = 機關名稱
 *   2 = 標案案號 + 標案名稱（可能含 script 編碼）
 *   3 = 招標方式
 *   4 = 採購性質
 *   5 = 決標公告日期（民國年）
 *   6 = 決標金額
 *   7 = 決標公告序號（如 001）
 *   8 = （可能空白）
 *   9 = 檢視連結（詳細頁）
 */
public record PccAwardListRow(
        String agencyName,              // 機關名稱
        String tenderNumber,            // 標案案號
        String tenderName,              // 標案名稱
        String tenderMethod,            // 招標方式
        String procurementType,         // 採購性質
        String awardAnnounceDateRaw,    // 決標公告日期（民國年，如 "115/05/12"）
        String awardAmountRaw,          // 決標金額原始字串（如 "2,950,000"）
        String awardAnnounceSeq,        // 決標公告序號（如 "001"）
        String detailUrl                // 詳細頁連結
) {}
