"""
爬蟲設定。
可透過環境變數或 .env 檔案覆蓋預設值。
"""
import os
from dotenv import load_dotenv

load_dotenv()

# ── 資料庫 ────────────────────────────────────────────────────────────────────
DB_URL: str = os.getenv(
    "DATABASE_URL",
    "postgresql+psycopg2://postgres:Kali1234!@localhost:5432/mydb"
)
DB_SCHEMA: str = os.getenv("DB_SCHEMA", "tenderdb")

# ── 爬蟲行為 ──────────────────────────────────────────────────────────────────
REQUEST_DELAY_MS: int = int(os.getenv("REQUEST_DELAY_MS", "2000"))
PAGE_TIMEOUT_MS: int  = int(os.getenv("PAGE_TIMEOUT_MS",  "30000"))

# 採購性質過濾清單（空清單 = 不過濾）
PROCUREMENT_TYPE_FILTER: list[str] = ["工程", "財物", "勞務"]

# ── PCC 網址 ──────────────────────────────────────────────────────────────────
AWARD_BASE_URL: str = (
    "https://web.pcc.gov.tw/prkms/tender/common/agent/readTenderAgent"
)
