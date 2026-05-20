package com.taipei.iot.tender.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

/**
 * 政府採購網決標詳細頁 HTML 解析器。
 *
 * 解析邏輯：
 *   1. 依 table summary 屬性定位機關資料、採購資料、決標資料等區塊（同 PccDetailParser）
 *   2. 以 HTML id 補充特定欄位
 *   3. 解析得標廠商資料區塊（可能有多家廠商）
 *
 * 注意：PCC 決標詳細頁 HTML 結構可能因招標方式/決標方式而略有差異。
 * 本解析器採寬鬆策略：盡量解析所有可取得欄位，缺漏欄位以 null 處理。
 */
public class PccAwardDetailParser {

    private static final Set<String> LABEL_CLASSES = Set.of("tbg_1", "tbg_4", "tbg_5", "tbg_6", "tbg_7", "tbg_9");
    private static final Set<String> VALUE_CLASSES  = Set.of("tbg_2", "tbg_4R");

    /**
     * 決標詳細頁的表格以 CSS class 區分（無 summary 屬性）：
     *   tb_01 → 機關資料（label: tbg_1, value: tbg_2）
     *   tb_04 → 採購資料（label: tbg_4, value: tbg_4R）—— 含決標方式、採購金額級距、標的分類
     *   tb_09 → 決標資料（label: tbg_9, value: tbg_4R）—— 含決標日期、公告序號等
     */
    private static final String[] TABLE_CLASS_SECTIONS = {
            "tb_01", "tb_04", "tb_09"
    };

    /** HTML id → 欄位中文名稱 */
    private static final Map<String, String> ID_FIELD_MAP = Map.ofEntries(
            Map.entry("fkPmsTenderWay",        "招標方式"),
            Map.entry("fkPmsAwardWay",         "決標方式"),
            Map.entry("isGovernmentEstimate",  "是否訂有底價"),
            Map.entry("fkPmsProcurementRange", "採購金額級距"),
            Map.entry("tenderNameText",        "標案名稱"),
            Map.entry("awardDate",             "決標日期"),
            Map.entry("fkPmsExecuteLocation",  "履約地點")
    );

    private PccAwardDetailParser() {}

    /**
     * 解析決標詳細頁 HTML。
     *
     * @return 包含決標層級欄位 map 與得標廠商列表的結果
     */
    public static ParseResult parse(String html) {
        Document doc = Jsoup.parse(html);
        Map<String, String> fields = new HashMap<>();

        // 1. 以 class 選取各表格區塊（決標頁無 summary 屬性，改用 CSS class）
        for (String cls : TABLE_CLASS_SECTIONS) {
            parseTableByClass(doc, cls, fields);
        }

        // 2. 以 id 補充/覆蓋（部分頁面以 id 標記特定欄位）
        parseByIds(doc, fields);

        // 3. 補充特定欄位（label 文字直接比對，作為最後備援）
        parseFieldByLabelText(doc, "決標日期", fields);
        parseFieldByLabelText(doc, "履約期限", fields);
        parseFieldByLabelText(doc, "履約地點", fields);
        parseFieldByLabelText(doc, "是否訂有底價", fields);
        parseFieldByLabelText(doc, "電子郵件信箱", fields);

        // 4. 解析廠商資料
        List<Map<String, String>> vendors = parseVendors(doc);

        return new ParseResult(fields, vendors);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * 解析得標廠商資料。
     *
     * PCC 決標頁含兩種「廠商」相關表格：
     *   - 「投標廠商」（summary 含「投標廠商」）：每筆含廠商代碼/名稱/是否得標/決標金額 → 需解析
     *   - 「決標品項」（summary 含「品項」）：含品項名稱/得標廠商/品項金額 → 不應解析為廠商記錄
     *
     * 因此只取 summary 含「投標廠商」且不含「品項」的表格。
     * 最後過濾掉廠商名稱為空的記錄（防止決標品項區塊漏入）。
     */
    private static List<Map<String, String>> parseVendors(Document doc) {
        List<Map<String, String>> vendors = new ArrayList<>();

        // 只取 投標廠商 表格，排除 決標品項 等其他含「廠商」文字的表格
        List<Element> vendorTables = doc.select("table[summary]").stream()
                .filter(t -> t.attr("summary").contains("投標廠商")
                          && !t.attr("summary").contains("品項"))
                .toList();

        if (vendorTables.isEmpty()) {
            // 備援：找含「廠商名稱」label 但不含「品項名稱」的表格
            vendorTables = doc.select("table").stream()
                    .filter(t -> t.text().contains("廠商名稱")
                              && !t.text().contains("品項名稱"))
                    .toList();
        }

        for (Element table : vendorTables) {
            Map<String, String> current = new LinkedHashMap<>();
            for (Element row : table.select("tr")) {
                // 一行可能有多組 label-value 對
                List<Element> cells = row.select("td");
                for (int i = 0; i + 1 < cells.size(); i++) {
                    Element cell = cells.get(i);
                    if (hasAnyClass(cell, LABEL_CLASSES)) {
                        // 找右邊第一個 value cell
                        for (int j = i + 1; j < cells.size(); j++) {
                            if (hasAnyClass(cells.get(j), VALUE_CLASSES)) {
                                String label = clean(cell.text());
                                String value = clean(cells.get(j).text());
                                if (!label.isEmpty()) {
                                    // 遇到「廠商名稱」時，若已有廠商資料則另起一筆
                                    if ("廠商名稱".equals(label) && current.containsKey("廠商名稱")) {
                                        vendors.add(current);
                                        current = new LinkedHashMap<>();
                                    }
                                    current.put(label, value);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            if (!current.isEmpty()) {
                vendors.add(current);
            }
        }

        // 移除廠商名稱為空的記錄（決標品項區塊漏入的空殼資料）
        vendors.removeIf(v -> v.getOrDefault("廠商名稱", "").isBlank());

        // 去重：同一廠商可能因多張表格被解析兩次
        return deduplicateVendors(vendors);
    }

    private static List<Map<String, String>> deduplicateVendors(List<Map<String, String>> vendors) {
        List<Map<String, String>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, String> v : vendors) {
            // PCC award pages use 廠商代碼 (8-digit code) as the vendor identifier, not 統一編號
            String id = v.getOrDefault("廠商代碼", v.getOrDefault("統一編號", v.getOrDefault("登記字號", "")));
            String key = v.getOrDefault("廠商名稱", "") + "##" + id;
            if (seen.add(key)) {
                result.add(v);
            }
        }
        return result;
    }

    /**
     * 以 CSS class 選取表格並解析所有 label-value 對。
     * 一行可能有多組（如：機關代碼 | xxx | 單位名稱 | yyy），依序掃描每個 label cell，往右找第一個 value cell。
     */
    private static void parseTableByClass(Document doc, String tableClass, Map<String, String> result) {
        Element table = doc.selectFirst("table." + tableClass);
        if (table == null) return;

        for (Element row : table.select("tr")) {
            List<Element> cells = row.select("td");
            for (int i = 0; i < cells.size(); i++) {
                Element cell = cells.get(i);
                if (!hasAnyClass(cell, LABEL_CLASSES)) continue;
                for (int j = i + 1; j < cells.size(); j++) {
                    if (hasAnyClass(cells.get(j), VALUE_CLASSES)) {
                        String label = clean(cell.text());
                        String value = clean(cells.get(j).text());
                        if (!label.isEmpty() && !value.isEmpty()) {
                            result.put(label, value);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void parseByIds(Document doc, Map<String, String> result) {
        for (Map.Entry<String, String> entry : ID_FIELD_MAP.entrySet()) {
            Element el = doc.getElementById(entry.getKey());
            if (el != null) {
                String text = clean(el.text());
                if (!text.isEmpty()) {
                    result.put(entry.getValue(), text);
                }
            }
        }
    }

    private static void parseFieldByLabelText(Document doc, String labelText, Map<String, String> result) {
        if (result.containsKey(labelText)) return;
        for (Element td : doc.select("td")) {
            if (labelText.equals(clean(td.text()))) {
                Element sibling = td.nextElementSibling();
                if (sibling != null) {
                    String value = clean(sibling.text());
                    if (!value.isEmpty()) {
                        result.put(labelText, value);
                    }
                }
                break;
            }
        }
    }

    private static boolean hasAnyClass(Element el, Set<String> classes) {
        for (String cls : el.classNames()) {
            if (classes.contains(cls)) return true;
        }
        return false;
    }

    private static String clean(String s) {
        if (s == null) return "";
        // U+3000 (ideographic space 　) is used as a label prefix in PCC vendor tables
        // and is NOT matched by Java's \s, so we handle it explicitly.
        return s.replaceAll("[\\s\u3000]+", " ").trim();
    }

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * 決標詳細頁解析結果。
     *
     * @param fields  決標層級欄位（機關/採購/決標資料區塊）
     * @param vendors 得標廠商資料列表（每家廠商一個 map）
     */
    public record ParseResult(
            Map<String, String> fields,
            List<Map<String, String>> vendors
    ) {}
}
