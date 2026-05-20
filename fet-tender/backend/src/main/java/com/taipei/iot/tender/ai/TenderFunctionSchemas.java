package com.taipei.iot.tender.ai;

import java.util.List;
import java.util.Map;

/**
 * 定義 tender_announcement 查詢工具的 OpenAI Tool Format schema。
 *
 * 提供 5 個查詢工具：
 *  1. search_tenders      — 依關鍵字、機關、日期、採購性質等搜尋招標
 *  2. get_tender_by_id    — 依 ID 取得單一標案詳情
 *  3. get_recent_tenders  — 取得最近 N 天的招標公告
 *  4. get_tenders_by_budget — 依預算金額範圍查詢
 *  5. get_tender_stats    — 各 solution 統計（數量、預算）
 */
public final class TenderFunctionSchemas {

    private TenderFunctionSchemas() {}

    public static List<Map<String, Object>> getTools() {
        return List.of(
                searchTenders(),
                getTenderById(),
                getRecentTenders(),
                getTendersByBudget(),
                getTenderStats()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. search_tenders
    // ─────────────────────────────────────────────────────────────────────────
    private static Map<String, Object> searchTenders() {
        return tool("search_tenders",
                "搜尋招標公告，可依標案名稱關鍵字、機關名稱、公告日期範圍、採購性質（工程/財物/勞務）、招標方式、Solution 名稱進行篩選。",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "keyword", prop("string", "標案名稱關鍵字，例如「智慧路燈」"),
                                "agencyName", prop("string", "機關名稱關鍵字，例如「台北市」"),
                                "solution", prop("string", "Solution 名稱，例如「5G」"),
                                "procurementType", prop("string", "採購性質：工程、財物 或 勞務"),
                                "tenderMethod", prop("string", "招標方式，例如：公開招標、限制性招標"),
                                "dateFrom", prop("string", "公告日期起（格式 YYYY-MM-DD）"),
                                "dateTo", prop("string", "公告日期迄（格式 YYYY-MM-DD）"),
                                "page", prop("integer", "頁碼，從 0 開始，預設 0"),
                                "size", prop("integer", "每頁筆數（最多 20），預設 10")
                        ),
                        "required", List.of()
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. get_tender_by_id
    // ─────────────────────────────────────────────────────────────────────────
    private static Map<String, Object> getTenderById() {
        return tool("get_tender_by_id",
                "依招標公告的 ID 取得完整詳細資訊，包含機關聯絡資訊、開標時間、決標方式等。",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "id", prop("integer", "招標公告的唯一識別碼（數字）")
                        ),
                        "required", List.of("id")
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. get_recent_tenders
    // ─────────────────────────────────────────────────────────────────────────
    private static Map<String, Object> getRecentTenders() {
        return tool("get_recent_tenders",
                "取得最近幾天內公告的招標資訊，用於查詢「最新」或「近期」的標案。",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "days", prop("integer", "往前幾天，預設 7（最多 90）"),
                                "limit", prop("integer", "最多回傳筆數，預設 10（最多 20）")
                        ),
                        "required", List.of()
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. get_tenders_by_budget
    // ─────────────────────────────────────────────────────────────────────────
    private static Map<String, Object> getTendersByBudget() {
        return tool("get_tenders_by_budget",
                "依預算金額範圍篩選招標公告，結果依預算金額由高至低排序。",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "minBudget", prop("number", "最低預算金額（元）"),
                                "maxBudget", prop("number", "最高預算金額（元）"),
                                "page", prop("integer", "頁碼，從 0 開始，預設 0"),
                                "size", prop("integer", "每頁筆數（最多 20），預設 10")
                        ),
                        "required", List.of()
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. get_tender_stats
    // ─────────────────────────────────────────────────────────────────────────
    private static Map<String, Object> getTenderStats() {
        return tool("get_tender_stats",
                "取得各 Solution 的招標統計摘要，包含標案數量、預算總額、平均預算，可依日期範圍篩選。",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "dateFrom", prop("string", "統計起始日期（格式 YYYY-MM-DD），不填則不限"),
                                "dateTo", prop("string", "統計結束日期（格式 YYYY-MM-DD），不填則不限")
                        ),
                        "required", List.of()
                )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 輔助方法
    // ─────────────────────────────────────────────────────────────────────────
    private static Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters
                )
        );
    }

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }
}
