"""
政府採購網決標詳細頁 HTML 解析器（對應 Java PccAwardDetailParser）。

依賴：
    pip install beautifulsoup4
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Optional

from bs4 import BeautifulSoup, Tag

# CSS class 集合（與 Java 常數一致）
_LABEL_CLASSES = {"tbg_1", "tbg_4", "tbg_5", "tbg_6", "tbg_7", "tbg_9"}
_VALUE_CLASSES  = {"tbg_2", "tbg_4R"}

# 以 table CSS class 選取的三個區塊
_TABLE_CLASS_SECTIONS = ["tb_01", "tb_04", "tb_09"]

# HTML id → 欄位中文名稱
_ID_FIELD_MAP: dict[str, str] = {
    "fkPmsTenderWay":        "招標方式",
    "fkPmsAwardWay":         "決標方式",
    "isGovernmentEstimate":  "是否訂有底價",
    "fkPmsProcurementRange": "採購金額級距",
    "tenderNameText":        "標案名稱",
    "awardDate":             "決標日期",
    "fkPmsExecuteLocation":  "履約地點",
}


@dataclass
class ParseResult:
    """決標詳細頁解析結果（對應 Java PccAwardDetailParser.ParseResult）。"""
    fields: dict[str, str] = field(default_factory=dict)
    vendors: list[dict[str, str]] = field(default_factory=list)


def parse(html: str) -> ParseResult:
    """
    解析決標詳細頁 HTML，回傳決標層級欄位 map 與得標廠商列表。

    對應 Java：PccAwardDetailParser.parse(html)
    """
    doc = BeautifulSoup(html, "html.parser")
    fields: dict[str, str] = {}

    # 1. 以 CSS class 選取各表格區塊
    for cls in _TABLE_CLASS_SECTIONS:
        _parse_table_by_class(doc, cls, fields)

    # 2. 以 id 補充/覆蓋特定欄位
    _parse_by_ids(doc, fields)

    # 3. 以 label 文字補充欄位（最後備援）
    for label_text in ["決標日期", "履約期限", "履約地點", "是否訂有底價", "電子郵件信箱"]:
        _parse_field_by_label_text(doc, label_text, fields)

    # 4. 解析廠商資料
    vendors = _parse_vendors(doc)

    return ParseResult(fields=fields, vendors=vendors)


# ── Private helpers ───────────────────────────────────────────────────────────

def _has_any_class(el: Tag, classes: set[str]) -> bool:
    return bool(set(el.get("class", [])) & classes)


def _clean(text: Optional[str]) -> str:
    if not text:
        return ""
    import re
    return re.sub(r"[\s\u3000]+", " ", text).strip()


def _parse_table_by_class(doc: BeautifulSoup, table_class: str, result: dict[str, str]) -> None:
    table = doc.find("table", class_=table_class)
    if not table:
        return

    for row in table.find_all("tr"):
        cells = row.find_all("td")
        for i, cell in enumerate(cells):
            if not _has_any_class(cell, _LABEL_CLASSES):
                continue
            for j in range(i + 1, len(cells)):
                if _has_any_class(cells[j], _VALUE_CLASSES):
                    label = _clean(cell.get_text())
                    value = _clean(cells[j].get_text())
                    if label and value:
                        result[label] = value
                    break


def _parse_by_ids(doc: BeautifulSoup, result: dict[str, str]) -> None:
    for el_id, field_name in _ID_FIELD_MAP.items():
        el = doc.find(id=el_id)
        if el:
            text = _clean(el.get_text())
            if text:
                result[field_name] = text


def _parse_field_by_label_text(
    doc: BeautifulSoup, label_text: str, result: dict[str, str]
) -> None:
    if label_text in result:
        return
    for td in doc.find_all("td"):
        if label_text == _clean(td.get_text()):
            sibling = td.find_next_sibling("td")
            if sibling:
                value = _clean(sibling.get_text())
                if value:
                    result[label_text] = value
            break


def _parse_vendors(doc: BeautifulSoup) -> list[dict[str, str]]:
    """
    解析得標廠商資料。

    只取 summary 含「投標廠商」且不含「品項」的 table；
    備援：找含「廠商名稱」label 但不含「品項名稱」的表格。
    最後過濾廠商名稱為空的記錄並去重。
    """
    vendor_tables = [
        t for t in doc.find_all("table", summary=True)
        if "投標廠商" in t.get("summary", "") and "品項" not in t.get("summary", "")
    ]

    if not vendor_tables:
        vendor_tables = [
            t for t in doc.find_all("table")
            if "廠商名稱" in t.get_text() and "品項名稱" not in t.get_text()
        ]

    vendors: list[dict[str, str]] = []
    for table in vendor_tables:
        current: dict[str, str] = {}
        for row in table.find_all("tr"):
            cells = row.find_all("td")
            for i in range(len(cells) - 1):
                cell = cells[i]
                if not _has_any_class(cell, _LABEL_CLASSES):
                    continue
                for j in range(i + 1, len(cells)):
                    if _has_any_class(cells[j], _VALUE_CLASSES):
                        label = _clean(cell.get_text())
                        value = _clean(cells[j].get_text())
                        if label:
                            if label == "廠商名稱" and "廠商名稱" in current:
                                vendors.append(current)
                                current = {}
                            current[label] = value
                        break
        if current:
            vendors.append(current)

    # 移除廠商名稱為空的記錄
    vendors = [v for v in vendors if v.get("廠商名稱", "").strip()]

    # 去重
    return _deduplicate_vendors(vendors)


def _deduplicate_vendors(vendors: list[dict[str, str]]) -> list[dict[str, str]]:
    result: list[dict[str, str]] = []
    seen: set[str] = set()
    for v in vendors:
        vendor_id = v.get("廠商代碼") or v.get("統一編號") or v.get("登記字號") or ""
        key = f"{v.get('廠商名稱', '')}##{vendor_id}"
        if key not in seen:
            seen.add(key)
            result.append(v)
    return result
