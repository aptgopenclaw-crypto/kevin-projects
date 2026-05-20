"""
PostgreSQL upsert 操作（對應 Java TenderAwardRepository + upsertAll）。

使用 SQLAlchemy Core，直接操作 tenderdb.tender_award 表。
"""
from __future__ import annotations

import logging
from datetime import datetime
from typing import Any

from sqlalchemy import (
    Boolean,
    Column,
    Date,
    DateTime,
    Integer,
    MetaData,
    Numeric,
    String,
    Table,
    Text,
    create_engine,
    select,
    update,
)
from sqlalchemy.engine import Engine

from config import DB_SCHEMA, DB_URL
from models import TenderAward

logger = logging.getLogger(__name__)

metadata = MetaData(schema=DB_SCHEMA)

tender_award_table = Table(
    "tender_award",
    metadata,
    Column("id",                     Integer, primary_key=True, autoincrement=True),
    Column("solution",               String(255)),
    Column("matched_keyword",        String(255)),
    Column("agency_name",            String(500)),
    Column("tender_number",          String(255)),
    Column("tender_name",            String(1000)),
    Column("tender_method",          String(100)),
    Column("procurement_type",       String(100)),
    Column("award_announce_date",    Date),
    Column("award_amount_raw",       String(500)),
    Column("award_amount",           Numeric(18, 2)),
    Column("award_announce_seq",     String(20)),
    Column("detail_url",             Text),
    Column("agency_code",            String(50)),
    Column("unit_name",              String(255)),
    Column("agency_address",         String(500)),
    Column("contact_person",         String(255)),
    Column("contact_phone",          String(255)),
    Column("contact_email",          String(255)),
    Column("tender_category",        Text),
    Column("procurement_amount_range", String(100)),
    Column("award_method",           String(100)),
    Column("has_base_price",         Boolean),
    Column("award_date",             Date),
    Column("performance_period",     String(500)),
    Column("performance_location",   String(500)),
    Column("vendor_order_seq",       Integer, nullable=False),
    Column("vendor_name",            String(500)),
    Column("vendor_tax_id",          String(50)),
    Column("vendor_address",         String(500)),
    Column("vendor_phone",           String(255)),
    Column("vendor_award_amount_raw", String(500)),
    Column("vendor_award_amount",    Numeric(18, 2)),
    Column("scraped_at",             DateTime, nullable=False),
)


def create_db_engine() -> Engine:
    return create_engine(DB_URL, connect_args={"options": f"-csearch_path={DB_SCHEMA}"})


def upsert_all(engine: Engine, rows: list[TenderAward]) -> int:
    """
    將爬取結果 upsert 進 tender_award 表（對應 Java upsertAll）。

    唯一鍵：solution + matched_keyword + tender_number +
            award_announce_date + award_announce_seq + vendor_order_seq
    """
    count = 0
    with engine.begin() as conn:
        for incoming in rows:
            if not incoming.tender_number or not incoming.tender_number.strip():
                continue
            if incoming.award_announce_date is None:
                continue
            if incoming.vendor_order_seq is None:
                incoming.vendor_order_seq = 1

            seq_key = incoming.award_announce_seq or ""

            stmt = select(tender_award_table).where(
                (tender_award_table.c.solution            == incoming.solution) &
                (tender_award_table.c.matched_keyword     == incoming.matched_keyword) &
                (tender_award_table.c.tender_number       == incoming.tender_number) &
                (tender_award_table.c.award_announce_date == incoming.award_announce_date) &
                (tender_award_table.c.award_announce_seq  == seq_key) &
                (tender_award_table.c.vendor_order_seq    == incoming.vendor_order_seq)
            )
            existing = conn.execute(stmt).fetchone()

            if existing:
                conn.execute(
                    update(tender_award_table)
                    .where(tender_award_table.c.id == existing.id)
                    .values(_to_update_values(incoming))
                )
            else:
                conn.execute(
                    tender_award_table.insert().values(_to_insert_values(incoming, seq_key))
                )
            count += 1

    return count


def _to_update_values(a: TenderAward) -> dict[str, Any]:
    return {
        "award_amount_raw":          a.award_amount_raw,
        "award_amount":              a.award_amount,
        "agency_code":               a.agency_code,
        "unit_name":                 a.unit_name,
        "agency_address":            a.agency_address,
        "contact_person":            a.contact_person,
        "contact_phone":             a.contact_phone,
        "contact_email":             a.contact_email,
        "tender_category":           a.tender_category,
        "procurement_amount_range":  a.procurement_amount_range,
        "award_method":              a.award_method,
        "has_base_price":            a.has_base_price,
        "award_date":                a.award_date,
        "performance_period":        a.performance_period,
        "performance_location":      a.performance_location,
        "vendor_name":               a.vendor_name,
        "vendor_tax_id":             a.vendor_tax_id,
        "vendor_address":            a.vendor_address,
        "vendor_phone":              a.vendor_phone,
        "vendor_award_amount_raw":   a.vendor_award_amount_raw,
        "vendor_award_amount":       a.vendor_award_amount,
        "scraped_at":                a.scraped_at,
    }


def _to_insert_values(a: TenderAward, seq_key: str) -> dict[str, Any]:
    return {
        "solution":                  a.solution,
        "matched_keyword":           a.matched_keyword,
        "agency_name":               a.agency_name,
        "tender_number":             a.tender_number,
        "tender_name":               a.tender_name,
        "tender_method":             a.tender_method,
        "procurement_type":          a.procurement_type,
        "award_announce_date":       a.award_announce_date,
        "award_amount_raw":          a.award_amount_raw,
        "award_amount":              a.award_amount,
        "award_announce_seq":        a.award_announce_seq or seq_key,
        "detail_url":                a.detail_url,
        "agency_code":               a.agency_code,
        "unit_name":                 a.unit_name,
        "agency_address":            a.agency_address,
        "contact_person":            a.contact_person,
        "contact_phone":             a.contact_phone,
        "contact_email":             a.contact_email,
        "tender_category":           a.tender_category,
        "procurement_amount_range":  a.procurement_amount_range,
        "award_method":              a.award_method,
        "has_base_price":            a.has_base_price,
        "award_date":                a.award_date,
        "performance_period":        a.performance_period,
        "performance_location":      a.performance_location,
        "vendor_order_seq":          a.vendor_order_seq,
        "vendor_name":               a.vendor_name,
        "vendor_tax_id":             a.vendor_tax_id,
        "vendor_address":            a.vendor_address,
        "vendor_phone":              a.vendor_phone,
        "vendor_award_amount_raw":   a.vendor_award_amount_raw,
        "vendor_award_amount":       a.vendor_award_amount,
        "scraped_at":                a.scraped_at,
    }
