"""
政府採購網 Playwright 瀏覽器服務（對應 Java PccBrowserService）。

使用前提：
    pip install playwright
    playwright install chromium
"""
import logging
import re
import time
from datetime import date
from typing import Optional
from urllib.parse import quote

from playwright.sync_api import (
    Browser,
    BrowserContext,
    Page,
    Playwright,
    sync_playwright,
)

from config import AWARD_BASE_URL, PAGE_TIMEOUT_MS, REQUEST_DELAY_MS
from models import PccAwardListRow

logger = logging.getLogger(__name__)

_DATE_FMT = "%Y/%m/%d"
_SCRIPT_NAME_RE = re.compile(r'pageCode2Img\("([^"]+)"\)')


class PccBrowserService:
    """
    管理單一 Playwright chromium 實例與 BrowserContext，
    可用 with 陳述式確保資源釋放：

        with PccBrowserService() as browser:
            page = browser.new_page()
            rows = browser.scrape_award_list_page(page, keyword="...", org_name="", ...)
    """

    def __init__(
        self,
        request_delay_ms: int = REQUEST_DELAY_MS,
        page_timeout_ms: int = PAGE_TIMEOUT_MS,
    ) -> None:
        self._request_delay_ms = request_delay_ms
        self._page_timeout_ms = page_timeout_ms
        self._pw: Optional[Playwright] = None
        self._browser: Optional[Browser] = None
        self._context: Optional[BrowserContext] = None

    def __enter__(self) -> "PccBrowserService":
        self._pw = sync_playwright().start()
        self._browser = self._pw.chromium.launch(
            headless=True,
            args=[
                "--no-sandbox",
                "--disable-blink-features=AutomationControlled",
                "--disable-dev-shm-usage",
            ],
        )
        self._context = self._browser.new_context(
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
            viewport={"width": 1280, "height": 900},
            locale="zh-TW",
        )
        logger.info("[PccBrowser] 瀏覽器已啟動")
        return self

    def __exit__(self, *_) -> None:
        try:
            self._context and self._context.close()
        except Exception:
            pass
        try:
            self._browser and self._browser.close()
        except Exception:
            pass
        try:
            self._pw and self._pw.stop()
        except Exception:
            pass
        logger.info("[PccBrowser] 瀏覽器已關閉")

    def new_page(self) -> Page:
        assert self._context is not None, "PccBrowserService 尚未啟動，請使用 with 陳述式"
        return self._context.new_page()

    def delay(self) -> None:
        time.sleep(self._request_delay_ms / 1000.0)

    # ── 決標列表頁爬取 ─────────────────────────────────────────────────────────

    def scrape_award_list_page(
        self,
        page: Page,
        keyword: str,
        org_name: str,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
    ) -> list[PccAwardListRow]:
        """
        爬取政府採購網決標查詢列表頁（readTenderAgent）。

        :param keyword:    標案名稱關鍵字（org_name 為空時使用）
        :param org_name:   機關名稱（與 keyword 二擇一）
        :param start_date: 決標公告起始日期（預設今日）
        :param end_date:   決標公告結束日期（預設今日）
        """
        today = date.today()
        start_date = start_date or today
        end_date = end_date or today

        url = self._build_award_search_url(keyword, org_name, start_date, end_date)
        label = f"keyword='{keyword}'" if not org_name else f"orgName='{org_name}'"
        logger.info("[PccBrowser] 決標搜尋列表 (%s)", label)

        try:
            page.goto(url, timeout=self._page_timeout_ms, wait_until="networkidle")
            page.wait_for_timeout(1000)
        except Exception as exc:
            logger.error("[PccBrowser] 載入決標列表頁失敗: %s", exc)
            return []

        results: list[PccAwardListRow] = []
        page_index = 1

        while True:
            logger.debug("[PccBrowser] 決標 實際 URL: %s, HTML 長度: %d", page.url, len(page.content()))

            rows = page.query_selector_all("table.tb_01 tbody tr, table#tpam tbody tr, tr.tb_b2")
            logger.debug("[PccBrowser] 決標 (%s) 第%d頁 row數: %d", label, page_index, len(rows))

            before = len(results)
            for row in rows:
                try:
                    list_row = self._parse_award_list_row(row)
                    if list_row:
                        results.append(list_row)
                except Exception as exc:
                    logger.warning("[PccBrowser] 解析決標列表行失敗: %s", exc)

            page_count = len(results) - before
            logger.info(
                "[PccBrowser] 決標 (%s) 第%d頁 找到 %d 筆，累計 %d 筆",
                label, page_index, page_count, len(results),
            )

            # 不足 50 筆代表已是最後一頁
            if page_count < 50:
                break

            next_btn = page.query_selector(
                "a[title='下一頁'], a:text('下一頁'), input[value='下一頁'], "
                ".pageNav a:last-child, #pageNav a:last-child, a.next, li.next a"
            )
            if next_btn is None:
                logger.debug("[PccBrowser] 決標 (%s) 找不到下一頁按鈕，結束翻頁", label)
                break

            logger.info("[PccBrowser] 決標 (%s) 翻頁至第 %d 頁", label, page_index + 1)
            try:
                next_btn.click(no_wait_after=True)
                page.wait_for_load_state("networkidle", timeout=self._page_timeout_ms)
                page.wait_for_timeout(1000)
            except Exception as exc:
                logger.warning("[PccBrowser] 決標 (%s) 翻頁失敗: %s，停止翻頁", label, exc)
                break

            page_index += 1

        logger.info("[PccBrowser] 決標 (%s) 全部共 %d 筆（共 %d 頁）", label, len(results), page_index)
        return results

    # ── 詳細頁 + CAPTCHA ───────────────────────────────────────────────────────

    def fetch_detail_page_html(self, page: Page, detail_url: str) -> Optional[str]:
        """
        取得決標詳細頁 HTML，含撲克牌驗證碼窮舉法（C(6,2)=15 種組合）。

        :return: 詳細頁 HTML；CAPTCHA 失敗或 detail_url 為空時回傳 None
        """
        if not detail_url:
            return None

        try:
            page.goto(detail_url, timeout=self._page_timeout_ms, wait_until="networkidle")
            page.wait_for_timeout(1000)
        except Exception as exc:
            logger.error("[PccBrowser] 載入詳細頁失敗: %s", exc)
            return None

        body_text = page.text_content("body") or ""

        if self._is_detail_page(body_text):
            return page.content()

        if self._is_captcha_page(body_text):
            return self._solve_captcha_and_get_html(page)

        logger.warning("[PccBrowser] 未知頁面: %s", page.url)
        return None

    def _solve_captcha_and_get_html(self, page: Page) -> Optional[str]:
        """
        隨機窮舉 CAPTCHA：每次隨機從 C(6,2)=15 種組合中選一，
        伺服器失敗後會換一組驗證碼，繼續嘗試直到成功或超過 100 次。
        """
        import random

        all_combos = [(i, j) for i in range(6) for j in range(i + 1, 6)]
        attempt = 0

        while True:
            attempt += 1
            combo = random.choice(all_combos)
            logger.debug("[PccBrowser] CAPTCHA 嘗試 %d，組合 %s", attempt, combo)

            checkboxes = page.query_selector_all("input[name='choose']")
            if len(checkboxes) < 6:
                logger.warning("[PccBrowser] B區牌數不足 (%d)，嘗試刷新", len(checkboxes))
                refresh = page.query_selector("#b_refresh")
                if refresh:
                    try:
                        refresh.click(no_wait_after=True)
                    except Exception as exc:
                        logger.warning("[PccBrowser] refresh click 例外（忽略）: %s", exc)
                else:
                    logger.warning("[PccBrowser] 找不到 #b_refresh，重新載入頁面")
                    page.reload(timeout=self._page_timeout_ms)

                try:
                    page.wait_for_load_state("networkidle", timeout=self._page_timeout_ms)
                except Exception:
                    pass
                page.wait_for_timeout(2000)

                if self._is_detail_page(page.text_content("body") or ""):
                    logger.info("[PccBrowser] 刷新後已進入詳細頁，attempt=%d", attempt)
                    return page.content()
                continue

            # 清除所有勾選
            for cb in checkboxes:
                cb.evaluate("el => el.checked = false")

            # 點擊選中的兩張牌
            for idx in combo:
                cb_id = checkboxes[idx].get_attribute("id")
                if cb_id:
                    label = page.query_selector(f"label[for='{cb_id}']")
                    if label:
                        try:
                            label.click(no_wait_after=True)
                        except Exception:
                            checkboxes[idx].evaluate("el => el.checked = true")
                    else:
                        checkboxes[idx].evaluate("el => el.checked = true")
                else:
                    checkboxes[idx].evaluate("el => el.checked = true")
                page.wait_for_timeout(300)

            # 送出
            submit = (
                page.query_selector("#b_submit")
                or page.query_selector("input[value='確認送出']")
                or page.query_selector("button")
            )
            if submit:
                try:
                    submit.click(no_wait_after=True)
                except Exception as exc:
                    logger.warning("[PccBrowser] submit click 例外（忽略）: %s", exc)

            page.wait_for_timeout(3000)
            try:
                page.wait_for_load_state("networkidle", timeout=10000)
            except Exception:
                pass

            if self._is_detail_page(page.text_content("body") or ""):
                logger.info("[PccBrowser] CAPTCHA 通過，嘗試 %d 次", attempt)
                return page.content()

            if attempt >= 100:
                logger.error("[PccBrowser] CAPTCHA 嘗試超過 100 次，放棄")
                return None

    # ── 私有工具方法 ───────────────────────────────────────────────────────────

    def _parse_award_list_row(self, row) -> Optional[PccAwardListRow]:
        cols = row.query_selector_all("td")
        if len(cols) < 8:
            return None

        agency_name = self._cell_text(cols[1])

        col2_text = self._cell_text(cols[2])
        tender_name_from_script = self._extract_script_name(cols[2])
        parts = [p.strip() for p in col2_text.split("\n") if p.strip()]
        tender_number = parts[0] if parts else ""
        if tender_name_from_script:
            tender_name = tender_name_from_script
        else:
            tender_name = " ".join(parts[1:]) if len(parts) > 1 else ""

        tender_method     = self._cell_text(cols[3])
        procurement_type  = self._cell_text(cols[4])
        award_date_raw    = self._cell_text(cols[5])
        award_amount_raw  = self._cell_text(cols[6])
        award_announce_seq = self._cell_text(cols[7])

        detail_url = ""
        view_link = row.query_selector("a[href*='QueryAtmAwardDetail'], a[href*='AtmAward']")
        if view_link is None:
            view_link = row.query_selector("a")
        if view_link:
            href = view_link.get_attribute("href") or ""
            if href:
                detail_url = href if href.startswith("http") else "https://web.pcc.gov.tw" + href

        if not tender_number and not tender_name:
            return None

        return PccAwardListRow(
            agency_name=agency_name,
            tender_number=tender_number,
            tender_name=tender_name,
            tender_method=tender_method,
            procurement_type=procurement_type,
            award_announce_date_raw=award_date_raw,
            award_amount_raw=award_amount_raw,
            award_announce_seq=award_announce_seq,
            detail_url=detail_url,
        )

    @staticmethod
    def _cell_text(cell) -> str:
        try:
            return (cell.inner_text() or "").strip()
        except Exception:
            return ""

    @staticmethod
    def _extract_script_name(col) -> str:
        try:
            script_el = col.query_selector("script")
            if script_el is None:
                return ""
            html = script_el.inner_html() or ""
            m = _SCRIPT_NAME_RE.search(html)
            return m.group(1) if m else ""
        except Exception:
            return ""

    @staticmethod
    def _is_detail_page(text: str) -> bool:
        return any(kw in text for kw in ["機關資料", "招標資料", "機關代碼", "決標資料", "得標廠商"])

    @staticmethod
    def _is_captcha_page(text: str) -> bool:
        return any(kw in text for kw in ["A區", "撲克牌", "防止惡意程式", "驗證"])

    @staticmethod
    def _build_award_search_url(
        keyword: str, org_name: str, start_date: date, end_date: date
    ) -> str:
        start = start_date.strftime(_DATE_FMT)
        end   = end_date.strftime(_DATE_FMT)
        return (
            AWARD_BASE_URL
            + "?pageSize=50"
            + "&firstSearch=false"
            + "&isBinding=N"
            + "&isLogIn=N"
            + f"&orgName={quote(org_name)}"
            + "&orgId="
            + f"&tenderName={quote(keyword)}"
            + "&tenderId="
            + "&tenderStatus=TENDER_STATUS_1"
            + "&tenderWay=TENDER_WAY_ALL_DECLARATION"
            + f"&awardAnnounceStartDate={quote(start)}"
            + f"&awardAnnounceEndDate={quote(end)}"
            + "&radProctrgCate="
            + "&tenderRange=TENDER_RANGE_ALL"
            + "&minBudget=&maxBudget=&item="
            + "&gottenVendorName=&gottenVendorId="
            + "&submitVendorName=&submitVendorId="
            + "&execLocation=&priorityCate="
            + "&radReConstruct=&policyAdvocacy=&isCpp="
        )
