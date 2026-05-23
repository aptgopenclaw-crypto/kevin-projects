package com.taipei.iot.tender.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

/**
 * 政府採購網決標詳細頁 HTML 解析器。
 */
public class PccAwardDetailParser {

    private static final Set<String> LABEL_CLASSES = Set.of("tbg_1", "tbg_4", "tbg_5", "tbg_6", "tbg_7", "tbg_9");
    private static final Set<String> VALUE_CLASSES  = Set.of("tbg_2", "tbg_4R");

    private static final String[] TABLE_CLASS_SECTIONS = {"tb_01", "tb_04", "tb_09"};

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

    public static ParseResult parse(String html) {
        Document doc = Jsoup.parse(html);
        Map<String, String> fields = new HashMap<>();

        for (String cls : TABLE_CLASS_SECTIONS) {
            parseTableByClass(doc, cls, fields);
        }
        parseByIds(doc, fields);
        parseFieldByLabelText(doc, "決標日期", fields);
        parseFieldByLabelText(doc, "履約期限", fields);
        parseFieldByLabelText(doc, "履約地點", fields);
        parseFieldByLabelText(doc, "是否訂有底價", fields);
        parseFieldByLabelText(doc, "電子郵件信箱", fields);

        List<Map<String, String>> vendors = parseVendors(doc);
        return new ParseResult(fields, vendors);
    }

    private static List<Map<String, String>> parseVendors(Document doc) {
        List<Map<String, String>> vendors = new ArrayList<>();

        List<Element> vendorTables = doc.select("table[summary]").stream()
                .filter(t -> t.attr("summary").contains("投標廠商")
                          && !t.attr("summary").contains("品項"))
                .toList();

        if (vendorTables.isEmpty()) {
            vendorTables = doc.select("table").stream()
                    .filter(t -> t.text().contains("廠商名稱")
                              && !t.text().contains("品項名稱"))
                    .toList();
        }

        for (Element table : vendorTables) {
            Map<String, String> current = new LinkedHashMap<>();
            for (Element row : table.select("tr")) {
                List<Element> cells = row.select("td");
                for (int i = 0; i + 1 < cells.size(); i++) {
                    Element cell = cells.get(i);
                    if (hasAnyClass(cell, LABEL_CLASSES)) {
                        for (int j = i + 1; j < cells.size(); j++) {
                            if (hasAnyClass(cells.get(j), VALUE_CLASSES)) {
                                String label = clean(cell.text());
                                String value = clean(cells.get(j).text());
                                if (!label.isEmpty()) {
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

        vendors.removeIf(v -> v.getOrDefault("廠商名稱", "").isBlank());
        return deduplicateVendors(vendors);
    }

    private static List<Map<String, String>> deduplicateVendors(List<Map<String, String>> vendors) {
        List<Map<String, String>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, String> v : vendors) {
            String id = v.getOrDefault("廠商代碼", v.getOrDefault("統一編號", v.getOrDefault("登記字號", "")));
            String key = v.getOrDefault("廠商名稱", "") + "##" + id;
            if (seen.add(key)) {
                result.add(v);
            }
        }
        return result;
    }

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
                    if (!value.isEmpty()) result.put(labelText, value);
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
        return s.replaceAll("[\\s\u3000]+", " ").trim();
    }

    public record ParseResult(
            Map<String, String> fields,
            List<Map<String, String>> vendors
    ) {}
}
