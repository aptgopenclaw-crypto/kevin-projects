package com.taipei.iot.tender.scraper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 政府採購網 Playwright 瀏覽器服務（對應 Python v3/src/scraper_v2.py）。
 *
 * 設計：
 *   - 非 Spring Bean，由 TenderScraperService 在 try-with-resources 中實例化，
 *     確保每次爬蟲結束後瀏覽器資源被正確釋放。
 *   - 每個 scrape-run 建立一個 BrowserContext、可共用同一個 Page 物件跨頁導航。
 *
 * 使用前提：需先安裝 Playwright browser binary：
 *   mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
 */
@Slf4j
public class PccBrowserService implements AutoCloseable {

    // 與 Python v3/src/config.py BASE_URL 一致
    private static final String BASE_URL = "https://web.pcc.gov.tw/prkms/tender/common/basic/readTenderBasic";
    private static final String AWARD_BASE_URL = "https://web.pcc.gov.tw/prkms/tender/common/agent/readTenderAgent";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Pattern SCRIPT_NAME_PATTERN = Pattern.compile("pageCode2Img\\(\"([^\"]+)\"\\)");

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final long requestDelayMs;
    private final long pageTimeoutMs;

    public PccBrowserService(long requestDelayMs, long pageTimeoutMs) {
        this.requestDelayMs = requestDelayMs;
        this.pageTimeoutMs  = pageTimeoutMs;

        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(List.of(
                                "--no-sandbox",
                                "--disable-blink-features=AutomationControlled",
                                "--disable-dev-shm-usage"
                        ))
        );
        this.context = browser.newContext(
                new Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                + "Chrome/120.0.0.0 Safari/537.36")
                        .setViewportSize(1280, 900)
                        .setLocale("zh-TW")
        );
        log.info("[PccBrowser] 瀏覽器已啟動");
    }

    @Override
    public void close() {
        try { context.close();    } catch (Exception ignored) {}
        try { browser.close();    } catch (Exception ignored) {}
        try { playwright.close(); } catch (Exception ignored) {}
        log.info("[PccBrowser] 瀏覽器已關閉");
    }

    public Page newPage() {
        return context.newPage();
    }

    public void delay() {
        try { Thread.sleep(requestDelayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── 列表頁爬取 ────────────────────────────────────────────────────────────

    /**
     * 爬取政府採購網搜尋結果列表頁。
     *
     * @param page     Playwright Page（跨呼叫共用）
     * @param keyword  標案名稱關鍵字（is_org_only_search=false 時使用）
     * @param orgName  機關名稱（is_org_only_search=true 時使用，與 keyword 二擇一）
     */
    public List<PccListRow> scrapeListPage(Page page, String keyword, String orgName) {
        String url = buildSearchUrl(keyword, orgName);
        String label = orgName.isEmpty() ? "keyword='" + keyword + "'" : "orgName='" + orgName + "'";
        log.info("[PccBrowser] 搜尋列表 ({})", label);

        try {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(pageTimeoutMs)
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            page.waitForTimeout(1000);
        } catch (Exception e) {
            log.error("[PccBrowser] 載入列表頁失敗: {}", e.getMessage());
            return List.of();
        }

        // 診斷：記錄目前頁面 URL 與 HTML 長度
        log.debug("[PccBrowser] 實際 URL: {}, HTML 長度: {}", page.url(), page.content().length());

        List<ElementHandle> rows = page.querySelectorAll(
                "table.tb_01 tbody tr, table#tpam tbody tr, tr.tb_b2");
        log.debug("[PccBrowser] ({}) querySelectorAll 原始行數: {}", label, rows.size());

        List<PccListRow> results = new ArrayList<>();
        for (ElementHandle row : rows) {
            try {
                PccListRow listRow = parseListRow(row);
                if (listRow != null) results.add(listRow);
            } catch (Exception e) {
                log.warn("[PccBrowser] 解析列表行失敗: {}", e.getMessage());
            }
        }

        log.info("[PccBrowser] ({}) 找到 {} 筆", label, results.size());
        return results;
    }

    private PccListRow parseListRow(ElementHandle row) {
        List<ElementHandle> cols = row.querySelectorAll("td");
        if (cols.size() < 9) return null;

        String agencyName = cellText(cols.get(1));

        // Col 2: 標案案號 + 標案名稱（可能含 script 編碼）
        String col2Text = cellText(cols.get(2));
        String tenderNameFromScript = extractScriptName(cols.get(2));

        String[] parts = Arrays.stream(col2Text.split("\\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        String tenderNumber = parts.length > 0 ? parts[0] : "";
        String tenderName = tenderNameFromScript.isEmpty()
                ? (parts.length > 1 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)) : "")
                : tenderNameFromScript;

        // 詳細頁連結
        String detailUrl = "";
        ElementHandle viewLink = row.querySelector("a[href*=\"tpam?pk=\"], a[href*=\"urlSelector\"]");
        if (viewLink == null) viewLink = row.querySelector("a");
        if (viewLink != null) {
            String href = viewLink.getAttribute("href");
            if (href != null && !href.isEmpty()) {
                detailUrl = href.startsWith("http") ? href : "https://web.pcc.gov.tw" + href;
            }
        }

        return new PccListRow(
                agencyName,
                tenderNumber,
                tenderName,
                toInt(cellText(cols.get(3))),
                cellText(cols.get(4)),
                cellText(cols.get(5)),
                cellText(cols.get(6)),
                cellText(cols.get(7)),
                cellText(cols.get(8)),
                detailUrl
        );
    }

    // ── 詳細頁 + CAPTCHA ──────────────────────────────────────────────────────

    /**
     * 取得詳細頁 HTML，含撲克牌驗證碼窮舉法（C(6,2)=15 種組合）。
     *
     * @return 詳細頁 HTML，CAPTCHA 失敗或 detailUrl 為空時回傳 null
     */
    private static final String ALLOWED_URL_PREFIX = "https://web.pcc.gov.tw/";

    public String fetchDetailPageHtml(Page page, String detailUrl) {
        if (detailUrl == null || detailUrl.isEmpty()) return null;

        if (!detailUrl.startsWith(ALLOWED_URL_PREFIX)) {
            log.warn("[PccBrowser] URL 不在白名單範圍，已拒絕: {}", detailUrl);
            return null;
        }

        try {
            page.navigate(detailUrl, new Page.NavigateOptions()
                    .setTimeout(pageTimeoutMs)
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            page.waitForTimeout(1000);
        } catch (Exception e) {
            log.error("[PccBrowser] 載入詳細頁失敗: {}", e.getMessage());
            return null;
        }

        String bodyText = page.textContent("body");

        // 直接到詳細頁（無驗證碼）
        if (isDetailPage(bodyText)) {
            return page.content();
        }

        // 需要 CAPTCHA
        if (isCaptchaPage(bodyText)) {
            return solveCaptchaAndGetHtml(page);
        }

        log.warn("[PccBrowser] 未知頁面: {}", page.url());
        return null;
    }

    /**
     * 隨機窮舉 CAPTCHA：每次隨機選 C(6,2) 其中一個組合，命中率 1/15。
     * 伺服器失敗後會換一組驗證碼，繼續嘗試直到成功。
     */
    private String solveCaptchaAndGetHtml(Page page) {
        List<int[]> allCombos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            for (int j = i + 1; j < 6; j++) {
                allCombos.add(new int[]{i, j});
            }
        }

        int attempt = 0;

        while (true) {
            attempt++;
            int[] combo = allCombos.get(ThreadLocalRandom.current().nextInt(allCombos.size()));
            log.debug("[PccBrowser] CAPTCHA 嘗試 {}，組合 [{},{}]", attempt, combo[0], combo[1]);

            List<ElementHandle> checkboxes = page.querySelectorAll("input[name='choose']");
            if (checkboxes.size() < 6) {
                log.warn("[PccBrowser] B區牌數不足 ({})，嘗試刷新", checkboxes.size());
                ElementHandle refresh = page.querySelector("#b_refresh");
                if (refresh != null) {
                    try {
                        refresh.click(new ElementHandle.ClickOptions().setNoWaitAfter(true));
                    } catch (Exception e) {
                        log.warn("[PccBrowser] refresh click 例外（忽略）: {}", e.getMessage());
                    }
                } else {
                    // 找不到刷新按鈕，重新導向詳細頁
                    log.warn("[PccBrowser] 找不到 #b_refresh，重新導向頁面");
                    page.reload(new Page.ReloadOptions().setTimeout(pageTimeoutMs));
                }
                try {
                    page.waitForLoadState(LoadState.NETWORKIDLE,
                            new Page.WaitForLoadStateOptions().setTimeout(pageTimeoutMs));
                } catch (Exception ignored) {}
                page.waitForTimeout(2000);
                // 若已跳出 CAPTCHA 頁（重導向到詳細頁），直接回傳
                if (isDetailPage(page.textContent("body"))) {
                    log.info("[PccBrowser] 刷新後已進入詳細頁，attempt={}", attempt);
                    return page.content();
                }
                continue;
            }

            // 清除所有勾選
            for (ElementHandle cb : checkboxes) {
                cb.evaluate("el => el.checked = false");
            }

            // 點擊選中的兩張牌（setNoWaitAfter 避免等待非預期導航而超時）
            for (int idx : combo) {
                String cbId = checkboxes.get(idx).getAttribute("id");
                if (cbId != null && !cbId.isEmpty()) {
                    ElementHandle label = page.querySelector("label[for='" + cbId + "']");
                    if (label != null) {
                        label.click(new ElementHandle.ClickOptions().setNoWaitAfter(true));
                    } else {
                        checkboxes.get(idx).evaluate("el => el.checked = true");
                    }
                } else {
                    checkboxes.get(idx).evaluate("el => el.checked = true");
                }
                page.waitForTimeout(300);
            }

            // 送出（click 後由 waitForLoadState 負責等待，不讓 Playwright 自動等待導航）
            ElementHandle submit = page.querySelector("#b_submit");
            if (submit == null) submit = page.querySelector("input[value='確認送出']");
            if (submit == null) submit = page.querySelector("button");
            if (submit != null) {
                try {
                    submit.click(new ElementHandle.ClickOptions().setNoWaitAfter(true));
                } catch (Exception e) {
                    log.warn("[PccBrowser] submit click 例外（忽略）: {}", e.getMessage());
                }
            }

            page.waitForTimeout(3000);
            try {
                page.waitForLoadState(LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(10000));
            } catch (Exception ignored) {}

            String newText = page.textContent("body");
            if (isDetailPage(newText)) {
                log.info("[PccBrowser] CAPTCHA 通過，嘗試 {} 次", attempt);
                return page.content();
            }

            if (attempt >= 100) {
                log.error("[PccBrowser] CAPTCHA 嘗試超過 100 次，放棄");
                return null;
            }
        }
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    private String buildSearchUrl(String keyword, String orgName) {
        String today = LocalDate.now().format(DATE_FMT);
        return BASE_URL
                + "?firstSearch=true"
                + "&searchType=basic"
                + "&isBinding=N"
                + "&isLogIn=N"
                + "&orgName="   + encode(orgName)
                + "&orgId="
                + "&tenderName=" + encode(keyword)
                + "&tenderId="
                + "&tenderType=TENDER_DECLARATION"
                + "&tenderWay=TENDER_WAY_ALL_DECLARATION"
                + "&dateType=isNow"
                + "&tenderStartDate=" + today
                + "&tenderEndDate="   + today
                + "&radProctrgCate="
                + "&policyAdvocacy=";
    }

    private String encode(String s) {
        if (s == null || s.isEmpty()) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private boolean isDetailPage(String text) {
        return text.contains("機關資料") || text.contains("招標資料") || text.contains("機關代碼")
                || text.contains("決標資料") || text.contains("得標廠商");
    }

    private boolean isCaptchaPage(String text) {
        return text.contains("A區") || text.contains("撲克牌")
                || text.contains("防止惡意程式") || text.contains("驗證");
    }

    private String cellText(ElementHandle cell) {
        try { return cell.innerText().trim(); } catch (Exception e) { return ""; }
    }

    private String extractScriptName(ElementHandle col) {
        try {
            ElementHandle scriptEl = col.querySelector("script");
            if (scriptEl == null) return "";
            Matcher m = SCRIPT_NAME_PATTERN.matcher(scriptEl.innerHTML());
            return m.find() ? m.group(1) : "";
        } catch (Exception e) { return ""; }
    }

    private Integer toInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    // ── 決標列表頁爬取 ─────────────────────────────────────────────────────────

    /**
     * 爬取政府採購網決標查詢列表頁（readTenderAgent），使用今日日期。
     *
     * @param page    Playwright Page（跨呼叫共用）
     * @param keyword 標案名稱關鍵字
     * @param orgName 機關名稱（與 keyword 二擇一，優先 orgName）
     */
    public List<PccAwardListRow> scrapeAwardListPage(Page page, String keyword, String orgName) {
        return scrapeAwardListPage(page, keyword, orgName, LocalDate.now(), LocalDate.now());
    }

    /**
     * 爬取政府採購網決標查詢列表頁（readTenderAgent），指定日期區間。
     *
     * @param page      Playwright Page（跨呼叫共用）
     * @param keyword   標案名稱關鍵字
     * @param orgName   機關名稱（與 keyword 二擇一，優先 orgName）
     * @param startDate 決標公告起始日期
     * @param endDate   決標公告結束日期
     */
    public List<PccAwardListRow> scrapeAwardListPage(Page page, String keyword, String orgName,
                                                     LocalDate startDate, LocalDate endDate) {
        String url = buildAwardSearchUrl(keyword, orgName, startDate, endDate);
        String label = orgName.isEmpty() ? "keyword='" + keyword + "'" : "orgName='" + orgName + "'";
        log.info("[PccBrowser] 決標搜尋列表 ({})", label);

        try {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(pageTimeoutMs)
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            page.waitForTimeout(1000);
        } catch (Exception e) {
            log.error("[PccBrowser] 載入決標列表頁失敗: {}", e.getMessage());
            return List.of();
        }

        List<PccAwardListRow> results = new ArrayList<>();
        int pageIndex = 1;

        while (true) {
            log.debug("[PccBrowser] 決標 實際 URL: {}, HTML 長度: {}", page.url(), page.content().length());

            List<ElementHandle> rows = page.querySelectorAll(
                    "table.tb_01 tbody tr, table#tpam tbody tr, tr.tb_b2");
            log.debug("[PccBrowser] 決標 ({}) 第{}頁 querySelectorAll 原始行數: {}", label, pageIndex, rows.size());

            int before = results.size();
            for (ElementHandle row : rows) {
                try {
                    PccAwardListRow listRow = parseAwardListRow(row);
                    if (listRow != null) results.add(listRow);
                } catch (Exception e) {
                    log.warn("[PccBrowser] 解析決標列表行失敗: {}", e.getMessage());
                }
            }
            int pageCount = results.size() - before;
            log.info("[PccBrowser] 決標 ({}) 第{}頁 找到 {} 筆，累計 {} 筆", label, pageIndex, pageCount, results.size());

            // 不足 50 筆代表已是最後一頁
            if (pageCount < 50) break;

            // 尋找下一頁按鈕
            ElementHandle nextBtn = page.querySelector("a[title='下一頁'], a:text('下一頁'), input[value='下一頁'], " +
                    ".pageNav a:last-child, #pageNav a:last-child, a.next, li.next a");
            if (nextBtn == null) {
                log.debug("[PccBrowser] 決標 ({}) 找不到下一頁按鈕，結束翻頁", label);
                break;
            }

            log.info("[PccBrowser] 決標 ({}) 翻頁至第 {} 頁", label, pageIndex + 1);
            try {
                nextBtn.click(new ElementHandle.ClickOptions().setNoWaitAfter(true));
                page.waitForLoadState(LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(pageTimeoutMs));
                page.waitForTimeout(1000);
            } catch (Exception e) {
                log.warn("[PccBrowser] 決標 ({}) 翻頁失敗: {}，停止翻頁", label, e.getMessage());
                break;
            }
            pageIndex++;
        }

        log.info("[PccBrowser] 決標 ({}) 全部共 {} 筆（共 {} 頁）", label, results.size(), pageIndex);
        return results;
    }

    private PccAwardListRow parseAwardListRow(ElementHandle row) {
        List<ElementHandle> cols = row.querySelectorAll("td");
        if (cols.size() < 8) return null;

        String agencyName = cellText(cols.get(1));

        // Col 2: 標案案號 + 標案名稱（可能含 script 編碼）
        String col2Text = cellText(cols.get(2));
        String tenderNameFromScript = extractScriptName(cols.get(2));
        String[] parts = Arrays.stream(col2Text.split("\\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        String tenderNumber = parts.length > 0 ? parts[0] : "";
        String tenderName = tenderNameFromScript.isEmpty()
                ? (parts.length > 1 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)) : "")
                : tenderNameFromScript;

        String tenderMethod     = cellText(cols.get(3));
        String procurementType  = cellText(cols.get(4));
        String awardDateRaw     = cellText(cols.get(5));
        String awardAmountRaw   = cellText(cols.get(6));
        String awardAnnounceSeq = cellText(cols.get(7));

        // 決標詳細頁連結：優先找 QueryAtmAwardDetail 或 AtmAward；備援取任意 <a>
        String detailUrl = "";
        ElementHandle viewLink = row.querySelector("a[href*='QueryAtmAwardDetail'], a[href*='AtmAward']");
        if (viewLink == null) viewLink = row.querySelector("a");
        if (viewLink != null) {
            String href = viewLink.getAttribute("href");
            if (href != null && !href.isEmpty()) {
                detailUrl = href.startsWith("http") ? href : "https://web.pcc.gov.tw" + href;
            }
        }

        // 跳過表頭或空行
        if (tenderNumber.isEmpty() && tenderName.isEmpty()) return null;

        return new PccAwardListRow(
                agencyName,
                tenderNumber,
                tenderName,
                tenderMethod,
                procurementType,
                awardDateRaw,
                awardAmountRaw,
                awardAnnounceSeq,
                detailUrl
        );
    }

    private String buildAwardSearchUrl(String keyword, String orgName) {
        return buildAwardSearchUrl(keyword, orgName, LocalDate.now(), LocalDate.now());
    }

    private String buildAwardSearchUrl(String keyword, String orgName, LocalDate startDate, LocalDate endDate) {
        String start = startDate.format(DATE_FMT);
        String end   = endDate.format(DATE_FMT);
        return AWARD_BASE_URL
                + "?pageSize=50"
                + "&firstSearch=false"
                + "&isBinding=N"
                + "&isLogIn=N"
                + "&orgName="   + encode(orgName)
                + "&orgId="
                + "&tenderName=" + encode(keyword)
                + "&tenderId="
                + "&tenderStatus=TENDER_STATUS_1"
                + "&tenderWay=TENDER_WAY_ALL_DECLARATION"
                + "&awardAnnounceStartDate=" + encode(start)
                + "&awardAnnounceEndDate="   + encode(end)
                + "&radProctrgCate="
                + "&tenderRange=TENDER_RANGE_ALL"
                + "&minBudget=&maxBudget=&item="
                + "&gottenVendorName=&gottenVendorId="
                + "&submitVendorName=&submitVendorId="
                + "&execLocation=&priorityCate="
                + "&radReConstruct=&policyAdvocacy=&isCpp=";
    }
}
