package com.taipei.iot.tender.scraper;

/**
 * 政府採購網招標公告列表頁單筆資料（內部 DTO）。
 */
public record PccListRow(
        String agencyName,          // 機關名稱
        String tenderNumber,        // 標案案號
        String tenderName,          // 標案名稱
        Integer transmissionCount,  // 傳輸次數
        String tenderMethod,        // 招標方式
        String procurementType,     // 採購性質
        String announcementDateRaw, // 公告日期（民國年，如 "115/05/08"）
        String deadlineRaw,         // 截止投標（民國年時間，如 "115/05/21 17:00"）
        String budgetRaw,           // 預算金額原始字串（如 "98,283,000元"）
        String detailUrl            // 詳細頁連結
) {}
