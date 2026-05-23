package com.taipei.iot.tender.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 政府採購網招標公告詳細頁 HTML 解析器。
 */
public class PccDetailParser {

    private static final Set<String> LABEL_CLASSES = Set.of("tbg_1", "tbg_4", "tbg_5", "tbg_6", "tbg_7");
    private static final Set<String> VALUE_CLASSES = Set.of("tbg_2", "tbg_4R");

    private static final String[] TABLE_SECTIONS = {
            "機關資料", "採購資料", "招標資料", "領投開標", "其他"
    };

    private static final Map<String, String> ID_FIELD_MAP = Map.ofEntries(
            Map.entry("fkPmsTenderWay", "招標方式"),
            Map.entry("fkPmsAwardWay", "決標方式"),
            Map.entry("fkTpamTenderStatus", "招標狀態"),
            Map.entry("isGovernmentEstimate", "是否訂有底價"),
            Map.entry("fkPmsProcurementRange", "採購金額級距"),
            Map.entry("fkTpamHowBid", "辦理方式"),
            Map.entry("fkPmsExecuteLocation", "履約地點"),
            Map.entry("spdt", "截止投標"),
            Map.entry("tenderNameText", "標案名稱")
    );

    private PccDetailParser() {}

    public static Map<String, String> parse(String html) {
        Document doc = Jsoup.parse(html);
        Map<String, String> result = new HashMap<>();

        for (String section : TABLE_SECTIONS) {
            parseTableSection(doc, section, result);
        }

        parseByIds(doc, result);

        parseFieldByLabelText(doc, "開標時間", result);
        parseFieldByLabelText(doc, "開標地點", result);

        return result;
    }

    private static void parseTableSection(Document doc, String sectionName, Map<String, String> result) {
        Element table = doc.select("table[summary]").stream()
                .filter(t -> sectionName.equals(t.attr("summary")))
                .findFirst().orElse(null);
        if (table == null) return;

        for (Element row : table.select("tr")) {
            Element labelCell = null;
            Element valueCell = null;

            for (Element cell : row.select("td")) {
                if (hasAnyClass(cell, LABEL_CLASSES)) {
                    labelCell = cell;
                } else if (hasAnyClass(cell, VALUE_CLASSES)) {
                    valueCell = cell;
                }
            }

            if (labelCell != null && valueCell != null) {
                String label = clean(labelCell.text());
                String value = clean(valueCell.text());
                if (!label.isEmpty() && !value.isEmpty()) {
                    result.put(label, value);
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
        doc.select("td").stream()
                .filter(td -> td.text().contains(labelText))
                .findFirst()
                .ifPresent(td -> {
                    Element next = td.nextElementSibling();
                    if (next != null) {
                        String val = clean(next.text());
                        if (!val.isEmpty()) result.put(labelText, val);
                    }
                });
    }

    private static boolean hasAnyClass(Element el, Set<String> classes) {
        for (String cls : classes) {
            if (el.hasClass(cls)) return true;
        }
        return false;
    }

    private static String clean(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }
}
