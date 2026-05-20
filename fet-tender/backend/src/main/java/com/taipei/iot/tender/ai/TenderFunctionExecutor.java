package com.taipei.iot.tender.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.tender.dto.TenderAnnouncementResponse;
import com.taipei.iot.tender.repository.TenderAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接收 AI 的 function call 請求，執行對應的資料庫查詢，回傳 JSON 字串結果。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenderFunctionExecutor {

    private final TenderAnnouncementRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 根據函數名稱與 AI 傳入的參數 JSON 執行對應查詢。
     *
     * @param functionName AI 呼叫的函數名稱
     * @param argumentsJson AI 傳入的參數（JSON 字串）
     * @return 查詢結果的 JSON 字串
     */
    public String execute(String functionName, String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(
                    (argumentsJson == null || argumentsJson.isBlank()) ? "{}" : argumentsJson);
            return switch (functionName) {
                case "search_tenders"       -> searchTenders(args);
                case "get_tender_by_id"     -> getTenderById(args);
                case "get_recent_tenders"   -> getRecentTenders(args);
                case "get_tenders_by_budget"-> getTendersByBudget(args);
                case "get_tender_stats"     -> getTenderStats(args);
                default -> error("未知的函數: " + functionName);
            };
        } catch (Exception e) {
            log.error("Function '{}' 執行失敗: {}", functionName, e.getMessage(), e);
            return error("執行失敗: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. search_tenders
    // ─────────────────────────────────────────────────────────────────────────
    private String searchTenders(JsonNode args) throws Exception {
        String solution       = text(args, "solution");
        String tenderName     = text(args, "keyword");
        String agencyName     = text(args, "agencyName");
        String procType       = text(args, "procurementType");
        String tenderMethod   = text(args, "tenderMethod");
        LocalDate dateFrom    = date(args, "dateFrom");
        LocalDate dateTo      = date(args, "dateTo");
        int page = intVal(args, "page", 0);
        int size = Math.min(intVal(args, "size", 10), 20);

        var pageResult = repository.searchByAi(
                solution, tenderName, agencyName, dateFrom, dateTo,
                procType, tenderMethod, PageRequest.of(page, size));

        var content = pageResult.map(TenderAnnouncementResponse::from).getContent();

        return objectMapper.writeValueAsString(Map.of(
                "total",    pageResult.getTotalElements(),
                "page",     page,
                "size",     size,
                "tenders",  content
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. get_tender_by_id
    // ─────────────────────────────────────────────────────────────────────────
    private String getTenderById(JsonNode args) throws Exception {
        long id = args.get("id").asLong();
        return repository.findById(id)
                .map(t -> {
                    try {
                        return objectMapper.writeValueAsString(TenderAnnouncementResponse.from(t));
                    } catch (Exception ex) {
                        return error("序列化失敗: " + ex.getMessage());
                    }
                })
                .orElse(error("找不到 ID: " + id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. get_recent_tenders
    // ─────────────────────────────────────────────────────────────────────────
    private String getRecentTenders(JsonNode args) throws Exception {
        int days  = Math.min(intVal(args, "days",  7),  90);
        int limit = Math.min(intVal(args, "limit", 10), 20);
        LocalDate fromDate = LocalDate.now().minusDays(days);

        var tenders = repository.findRecentTenders(fromDate, PageRequest.of(0, limit))
                .stream()
                .map(TenderAnnouncementResponse::from)
                .toList();

        return objectMapper.writeValueAsString(Map.of(
                "since",   fromDate.toString(),
                "total",   tenders.size(),
                "tenders", tenders
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. get_tenders_by_budget
    // ─────────────────────────────────────────────────────────────────────────
    private String getTendersByBudget(JsonNode args) throws Exception {
        BigDecimal minBudget = decimal(args, "minBudget");
        BigDecimal maxBudget = decimal(args, "maxBudget");
        int page = intVal(args, "page", 0);
        int size = Math.min(intVal(args, "size", 10), 20);

        var pageResult = repository.findByBudgetRange(minBudget, maxBudget, PageRequest.of(page, size));
        var content = pageResult.map(TenderAnnouncementResponse::from).getContent();

        return objectMapper.writeValueAsString(Map.of(
                "total",   pageResult.getTotalElements(),
                "page",    page,
                "size",    size,
                "tenders", content
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. get_tender_stats
    // ─────────────────────────────────────────────────────────────────────────
    private String getTenderStats(JsonNode args) throws Exception {
        LocalDate dateFrom = date(args, "dateFrom");
        LocalDate dateTo   = date(args, "dateTo");

        var rows = repository.getStatsBySolution(dateFrom, dateTo);
        var stats = rows.stream().map(row -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("solution",    row[0] != null ? row[0] : "未分類");
            m.put("count",       row[1]);
            m.put("totalBudget", row[2] != null ? row[2] : 0);
            m.put("avgBudget",   row[3] != null ? row[3] : 0);
            return m;
        }).toList();

        return objectMapper.writeValueAsString(Map.of(
                "dateFrom", dateFrom != null ? dateFrom.toString() : "不限",
                "dateTo",   dateTo   != null ? dateTo.toString()   : "不限",
                "stats",    stats
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 輔助方法
    // ─────────────────────────────────────────────────────────────────────────
    private String text(JsonNode args, String field) {
        return (args.has(field) && !args.get(field).isNull() && !args.get(field).asText().isBlank())
                ? args.get(field).asText() : null;
    }

    private LocalDate date(JsonNode args, String field) {
        String val = text(args, field);
        if (val == null) return null;
        try { return LocalDate.parse(val); } catch (Exception e) { return null; }
    }

    private int intVal(JsonNode args, String field, int def) {
        return args.has(field) && !args.get(field).isNull() ? args.get(field).asInt(def) : def;
    }

    private BigDecimal decimal(JsonNode args, String field) {
        if (!args.has(field) || args.get(field).isNull()) return null;
        try { return BigDecimal.valueOf(args.get(field).asDouble()); } catch (Exception e) { return null; }
    }

    private String error(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }
}
