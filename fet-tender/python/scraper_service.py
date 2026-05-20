"""
決標公告爬蟲主服務（對應 Java TenderAwardScraperService）。

搜尋邏輯：
  1. 讀取資料庫中的 announcement_search_keywords / announcement_agency_filters
  2. 以關鍵字搜尋 readTenderAgent（tenderStatus=TENDER_STATUS_1）
  3. 機關名稱後過濾
  4. 進入決標詳細頁，破解撲克牌 CAPTCHA 後解析決標 + 廠商資料
  5. 每筆決標公告的每家得標廠商各 upsert 一列至 tender_award 表
"""
from __future__ import annotations

import copy
import logging
import re
from collections import defaultdict
from dataclasses import replace
from datetime import date, datetime
from decimal import Decimal, InvalidOperation
from typing import Optional

from sqlalchemy import text
from sqlalchemy.engine import Engine

import pcc_award_detail_parser as detail_parser
from config import PROCUREMENT_TYPE_FILTER
from db import create_db_engine, upsert_all
from models import AwardScrapeResult, PccAwardListRow, TenderAward
from pcc_browser import PccBrowserService

logger = logging.getLogger(__name__)


# ── 資料庫讀取輔助 ─────────────────────────────────────────────────────────────

def _load_keywords(engine: Engine) -> list[dict]:
    """讀取 announcement_search_keywords（is_active=true）。"""
    with engine.connect() as conn:
        rows = conn.execute(
            text(
                "SELECT solution, keyword FROM tenderdb.announcement_search_keywords "
                "WHERE is_active = true "
                "ORDER BY solution, keyword"
            )
        ).fetchall()
    return [{"solution": r.solution, "keyword": r.keyword} for r in rows]


def _load_agency_filters(engine: Engine) -> list[dict]:
    """讀取 announcement_agency_filters（is_active=true）。"""
    with engine.connect() as conn:
        rows = conn.execute(
            text(
                "SELECT solution, agency_keyword, is_org_only_search "
                "FROM tenderdb.announcement_agency_filters "
                "WHERE is_active = true "
                "ORDER BY solution, agency_keyword"
            )
        ).fetchall()
    return [
        {
            "solution": r.solution,
            "agency_keyword": r.agency_keyword,
            "is_org_only_search": r.is_org_only_search,
        }
        for r in rows
    ]


# ── 主流程 ─────────────────────────────────────────────────────────────────────

def run_and_import(
    from_date: Optional[date] = None,
    to_date: Optional[date] = None,
) -> AwardScrapeResult:
    """
    執行決標爬蟲並將結果寫入資料庫。

    :param from_date: 起始日期（含），預設今日
    :param to_date:   結束日期（含），預設今日
    :return: AwardScrapeResult
    """
    today = date.today()
    from_date = from_date or today
    to_date   = to_date   or today

    logger.info("[AwardScraper] ===== 開始執行（日期區間 %s ~ %s）=====", from_date, to_date)

    engine = create_db_engine()

    keywords       = _load_keywords(engine)
    all_filters    = _load_agency_filters(engine)

    # 按 solution 分組
    filters_by_solution: dict[str, list[dict]] = defaultdict(list)
    for f in all_filters:
        filters_by_solution[f["solution"]].append(f)

    # 哪些 solution 全部使用機關名稱搜尋（org-only）
    org_only_solutions: set[str] = {
        sol
        for sol, flist in filters_by_solution.items()
        if all(f["is_org_only_search"] for f in flist)
    }

    logger.info(
        "[AwardScraper] 關鍵字: %d 個，org-only solutions: %s",
        len(keywords), org_only_solutions,
    )

    all_results: list[TenderAward] = []
    solution_key_counts: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
    scraped_at = datetime.now()

    # 逐日爬取
    dates: list[date] = []
    d = from_date
    while d <= to_date:
        dates.append(d)
        from datetime import timedelta
        d = d + timedelta(days=1)

    with PccBrowserService() as browser:
        page = browser.new_page()

        for current_date in dates:
            logger.info("[AwardScraper] 處理日期: %s", current_date)

            # 2a. 標案名稱關鍵字搜尋
            for kw in keywords:
                if kw["solution"] in org_only_solutions:
                    continue

                rows = browser.scrape_award_list_page(
                    page, kw["keyword"], "", current_date, current_date
                )
                rows = _filter_by_procurement_type(rows, kw["keyword"])

                agency_keywords = [
                    f["agency_keyword"]
                    for f in filters_by_solution.get(kw["solution"], [])
                    if not f["is_org_only_search"]
                ]
                if agency_keywords:
                    before = len(rows)
                    rows = [
                        r for r in rows
                        if any(ak in r.agency_name for ak in agency_keywords)
                    ]
                    logger.info(
                        "[AwardScraper] keyword='%s' 機關名稱過濾: %d -> %d 筆",
                        kw["keyword"], before, len(rows),
                    )

                for row in rows:
                    all_results.extend(
                        _fetch_and_build(browser, page, row, kw["solution"], kw["keyword"], scraped_at)
                    )
                    browser.delay()

                solution_key_counts[kw["solution"]][kw["keyword"]] += len(rows)
                browser.delay()

            # 2b. 機關名稱直接搜尋（org-only solutions）
            for solution in org_only_solutions:
                for f in filters_by_solution.get(solution, []):
                    rows = browser.scrape_award_list_page(
                        page, "", f["agency_keyword"], current_date, current_date
                    )
                    rows = _filter_by_procurement_type(rows, f["agency_keyword"])

                    for row in rows:
                        all_results.extend(
                            _fetch_and_build(browser, page, row, solution, f["agency_keyword"], scraped_at)
                        )
                        browser.delay()

                    solution_key_counts[solution][f["agency_keyword"]] += len(rows)
                    browser.delay()

    logger.info("[AwardScraper] 爬蟲完成，共 %d 筆廠商記錄，開始寫入 DB", len(all_results))
    count = upsert_all(engine, all_results)
    logger.info("[AwardScraper] ===== 完成，upsert %d 筆 =====", count)

    return AwardScrapeResult(
        total=count,
        solution_key_counts={k: dict(v) for k, v in solution_key_counts.items()},
        awards=all_results,
    )


# ── 採購性質過濾 ───────────────────────────────────────────────────────────────

def _filter_by_procurement_type(
    rows: list[PccAwardListRow], label: str
) -> list[PccAwardListRow]:
    if not PROCUREMENT_TYPE_FILTER:
        return rows
    before = len(rows)
    filtered = [
        r for r in rows
        if r.procurement_type and any(pt in r.procurement_type for pt in PROCUREMENT_TYPE_FILTER)
    ]
    if len(filtered) != before:
        logger.info(
            "[AwardScraper] '%s' 採購性質過濾: %d -> %d 筆", label, before, len(filtered)
        )
    return filtered


# ── 詳細頁抓取 + Entity 組裝 ───────────────────────────────────────────────────

def _fetch_and_build(
    browser: PccBrowserService,
    page,
    row: PccAwardListRow,
    solution: str,
    matched_keyword: str,
    scraped_at: datetime,
) -> list[TenderAward]:
    base = _build_from_list_row(row, solution, matched_keyword, scraped_at)

    if not row.detail_url:
        base.vendor_order_seq = 1
        return [base]

    html = browser.fetch_detail_page_html(page, row.detail_url)
    if html is None:
        base.vendor_order_seq = 1
        return [base]

    parsed = detail_parser.parse(html)
    _enrich_from_detail(base, parsed.fields)

    # 只保留得標廠商（是否得標 != 否），排除廠商名稱為空的記錄
    vendors = [
        v for v in parsed.vendors
        if v.get("是否得標") != "否" and v.get("廠商名稱", "").strip()
    ]

    if not vendors:
        base.vendor_order_seq = 1
        return [base]

    result: list[TenderAward] = []
    for i, vendor in enumerate(vendors):
        award = _clone_base(base)
        award.vendor_order_seq = i + 1
        _enrich_from_vendor(award, vendor)
        result.append(award)

    return result


def _build_from_list_row(
    row: PccAwardListRow, solution: str, matched_keyword: str, scraped_at: datetime
) -> TenderAward:
    amount_raw = row.award_amount_raw
    return TenderAward(
        solution=solution,
        matched_keyword=matched_keyword,
        agency_name=row.agency_name,
        tender_number=row.tender_number,
        tender_name=row.tender_name,
        tender_method=row.tender_method,
        procurement_type=row.procurement_type,
        award_announce_date=_parse_roc_date(row.award_announce_date_raw),
        award_amount_raw=amount_raw,
        award_amount=_parse_amount(amount_raw),
        award_announce_seq=row.award_announce_seq,
        detail_url=row.detail_url,
        scraped_at=scraped_at,
    )


def _enrich_from_detail(award: TenderAward, fields: dict[str, str]) -> None:
    if fields.get("標案名稱"):
        award.tender_name = fields["標案名稱"]
    if fields.get("標案案號"):
        award.tender_number = fields["標案案號"]

    award.agency_code              = fields.get("機關代碼")
    award.unit_name                = fields.get("單位名稱")
    award.agency_address           = fields.get("機關地址")
    award.contact_person           = fields.get("聯絡人")
    award.contact_phone            = fields.get("聯絡電話")
    award.contact_email            = fields.get("電子郵件信箱")
    award.tender_category          = fields.get("標的分類")
    award.procurement_amount_range = fields.get("採購金額級距")
    award.award_method             = fields.get("決標方式")
    award.has_base_price           = _parse_boolean(fields.get("是否訂有底價"))
    award.award_date               = _parse_roc_date(fields.get("決標日期"))
    award.performance_period       = fields.get("履約期限")
    award.performance_location     = fields.get("履約地點")


def _enrich_from_vendor(award: TenderAward, vendor: dict[str, str]) -> None:
    award.vendor_name = vendor.get("廠商名稱")
    tax_id = (
        vendor.get("廠商代碼")
        or vendor.get("統一編號")
        or vendor.get("登記字號")
    )
    award.vendor_tax_id       = tax_id
    award.vendor_address      = vendor.get("廠商地址")
    award.vendor_phone        = vendor.get("廠商電話")
    amt_raw                   = vendor.get("決標金額")
    award.vendor_award_amount_raw = amt_raw
    award.vendor_award_amount     = _parse_amount(amt_raw)


def _clone_base(base: TenderAward) -> TenderAward:
    """淺複製 base（不含廠商欄位）。"""
    return TenderAward(
        solution=base.solution,
        matched_keyword=base.matched_keyword,
        agency_name=base.agency_name,
        tender_number=base.tender_number,
        tender_name=base.tender_name,
        tender_method=base.tender_method,
        procurement_type=base.procurement_type,
        award_announce_date=base.award_announce_date,
        award_amount_raw=base.award_amount_raw,
        award_amount=base.award_amount,
        award_announce_seq=base.award_announce_seq,
        detail_url=base.detail_url,
        agency_code=base.agency_code,
        unit_name=base.unit_name,
        agency_address=base.agency_address,
        contact_person=base.contact_person,
        contact_phone=base.contact_phone,
        contact_email=base.contact_email,
        tender_category=base.tender_category,
        procurement_amount_range=base.procurement_amount_range,
        award_method=base.award_method,
        has_base_price=base.has_base_price,
        award_date=base.award_date,
        performance_period=base.performance_period,
        performance_location=base.performance_location,
        scraped_at=base.scraped_at,
    )


# ── 日期 / 金額 / 布林解析 ─────────────────────────────────────────────────────

def _parse_roc_date(s: Optional[str]) -> Optional[date]:
    """解析民國年日期字串（如 '115/05/12'）。"""
    if not s or not s.strip():
        return None
    try:
        parts = s.strip().split("/")
        year  = int(parts[0]) + 1911
        month = int(parts[1])
        day   = int(parts[2].split()[0])
        return date(year, month, day)
    except Exception:
        return None


def _parse_amount(raw: Optional[str]) -> Optional[Decimal]:
    if not raw or not raw.strip():
        return None
    cleaned = re.sub(r"[^\d,]", "", raw).replace(",", "")
    if not cleaned:
        return None
    try:
        return Decimal(cleaned)
    except InvalidOperation:
        return None


def _parse_boolean(s: Optional[str]) -> Optional[bool]:
    if not s or not s.strip():
        return None
    return s.strip() == "是"
