"""
資料模型：對應 Java TenderAward entity 與相關 DTO。
"""
from dataclasses import dataclass, field
from datetime import date, datetime
from decimal import Decimal
from typing import Optional


@dataclass
class PccAwardListRow:
    """決標查詢列表頁單筆資料（對應 Java PccAwardListRow）。"""
    agency_name: str
    tender_number: str
    tender_name: str
    tender_method: str
    procurement_type: str
    award_announce_date_raw: str    # 民國年格式，如 "115/05/12"
    award_amount_raw: str
    award_announce_seq: str
    detail_url: str


@dataclass
class TenderAward:
    """決標公告記錄（對應 Java TenderAward entity）。"""
    # 搜尋來源
    solution: Optional[str] = None
    matched_keyword: Optional[str] = None

    # 列表頁欄位
    agency_name: Optional[str] = None
    tender_number: Optional[str] = None
    tender_name: Optional[str] = None
    tender_method: Optional[str] = None
    procurement_type: Optional[str] = None
    award_announce_date: Optional[date] = None
    award_amount_raw: Optional[str] = None
    award_amount: Optional[Decimal] = None
    award_announce_seq: Optional[str] = None
    detail_url: Optional[str] = None

    # 詳細頁欄位（機關資料）
    agency_code: Optional[str] = None
    unit_name: Optional[str] = None
    agency_address: Optional[str] = None
    contact_person: Optional[str] = None
    contact_phone: Optional[str] = None
    contact_email: Optional[str] = None

    # 詳細頁欄位（採購資料）
    tender_category: Optional[str] = None
    procurement_amount_range: Optional[str] = None

    # 詳細頁欄位（決標資料）
    award_method: Optional[str] = None
    has_base_price: Optional[bool] = None
    award_date: Optional[date] = None
    performance_period: Optional[str] = None
    performance_location: Optional[str] = None

    # 廠商資料
    vendor_order_seq: int = 1
    vendor_name: Optional[str] = None
    vendor_tax_id: Optional[str] = None
    vendor_address: Optional[str] = None
    vendor_phone: Optional[str] = None
    vendor_award_amount_raw: Optional[str] = None
    vendor_award_amount: Optional[Decimal] = None

    # 稽核
    scraped_at: Optional[datetime] = None


@dataclass
class AwardScrapeResult:
    """爬蟲執行結果（對應 Java AwardScrapeResult record）。"""
    total: int
    solution_key_counts: dict[str, dict[str, int]]   # solution → {keyword → count}
    awards: list[TenderAward] = field(default_factory=list)
