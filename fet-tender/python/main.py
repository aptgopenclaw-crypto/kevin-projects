"""
決標公告爬蟲入口。

用法：
    # 爬取今日
    python main.py

    # 爬取指定日期區間（YYYY-MM-DD）
    python main.py --from 2025-05-01 --to 2025-05-19
"""
import argparse
import logging
import sys
from datetime import date

from scraper_service import run_and_import

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)

logger = logging.getLogger(__name__)


def _parse_date(s: str) -> date:
    try:
        return date.fromisoformat(s)
    except ValueError:
        raise argparse.ArgumentTypeError(f"日期格式錯誤（需 YYYY-MM-DD）: {s}")


def main() -> None:
    parser = argparse.ArgumentParser(description="政府採購網決標公告爬蟲")
    parser.add_argument("--from", dest="from_date", type=_parse_date, default=None,
                        help="起始日期 YYYY-MM-DD（預設今日）")
    parser.add_argument("--to",   dest="to_date",   type=_parse_date, default=None,
                        help="結束日期 YYYY-MM-DD（預設今日）")
    args = parser.parse_args()

    result = run_and_import(from_date=args.from_date, to_date=args.to_date)

    logger.info("===== 執行完成 =====")
    logger.info("upsert 總筆數: %d", result.total)
    for sol, key_counts in result.solution_key_counts.items():
        for key, cnt in key_counts.items():
            logger.info("  [%s] '%s' → %d 筆", sol, key, cnt)


if __name__ == "__main__":
    main()
