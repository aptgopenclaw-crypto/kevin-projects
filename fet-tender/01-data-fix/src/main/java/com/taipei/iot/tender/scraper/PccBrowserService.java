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
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 政府採購網 Playwright 瀏覽器服務（決標公告列表 + 詳細頁）。
 * 非 Spring Bean，由 TenderAwardScraperService 在 try-with-resources 中實例化。
 */
@Slf4j
public class PccBrowserService implements AutoCloseable {

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

    // ── 決標列表頁爬取 ────────────────────────────────────────────────────────

    public List<PccAwardListRow> scrapeAwardListPage(Page page, String keyword, String orgName) {
        return scrapeAwardListPage(page, keyword, orgName, LocalDate.now(), LocalDate.now());
    }

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

            if (pageCount < 50) break;

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

        String detailUrl = "";
        ElementHandle viewLink = row.querySelector("a[href*='QueryAtmAwardDetail'], a[href*='AtmAward']");
        if (viewLink == null) viewLink = row.querySelector("a");
        if (viewLink != null) {
            String href = viewLink.getAttribute("href");
            if (href != null && !href.isEmpty()) {
                detailUrl = href.startsWith("http") ? href : "https://web.pcc.gov.tw" + href;
            }
        }

        if (tenderNumber.isEmpty() && tenderName.isEmpty()) return null;

        return new PccAwardListRow(
                agencyName, tenderNumber, tenderName,
                tenderMethod, procurementType,
                awardDateRaw, awardAmountRaw, awardAnnounceSeq,
                detailUrl
        );
    }

    // ── 詳細頁 + CAPTCHA ──────────────────────────────────────────────────────

    public String fetchDetailPageHtml(Page page, String detailUrl) {
        if (detailUrl == null || detailUrl.isEmpty()) return null;

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

        if (isDetailPage(bodyText)) {
            return page.content();
        }
        if (isCaptchaPage(bodyText)) {
            return solveCaptchaAndGetHtml(page);
        }

        log.warn("[PccBrowser] 未知頁面: {}", page.url());
        return null;
    }

    private String solveCaptchaAndGetHtml(Page page) {
        List<int[]> allCombos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            for (int j = i + 1; j < 6; j++) {
                allCombos.add(new int[]{i, j});
            }
        }

        Random random = new Random();
        int attempt = 0;

        while (true) {
            attempt++;
            int[] combo = allCombos.get(random.nextInt(allCombos.size()));
            log.debug("[PccBrowser] CAPTCHA 嘗試 {}，組合 [{},{}]", attempt, combo[0], combo[1]);

            List<ElementHandle> checkboxes = page.querySelectorAll("input[name='choose']");
            if (checkboxes.size() < 6) {
                log.warn("[PccBrowser] B區牌數不足 ({})，嘗試刷新", checkboxes.size());
                ElementHandle refresh = page.querySelector("#b_refresh");
                if (refresh != null) {
                    try { refresh.click(new ElementHandle.ClickOptions().setNoWaitAfter(true)); }
                    catch (Exception e) { log.warn("[PccBrowser] refresh click 例外（忽略）: {}", e.getMessage()); }
                } else {
                    log.warn("[PccBrowser] 找不到 #b_refresh，重新載入頁面");
                    try {
                        page.reload(new Page.ReloadOptions().setTimeout(pageTimeoutMs));
                    } catch (Exception e) {
                        log.warn("[PccBrowser] page.reload() 失敗（忽略）: {}", e.getMessage());
                    }
                }
                try {
                    page.waitForLoadState(LoadState.NETWORKIDLE,
                            new Page.WaitForLoadStateOptions().setTimeout(pageTimeoutMs));
                } catch (Exception ignored) {}
                page.waitForTimeout(2000);
                if (isDetailPage(page.textContent("body"))) {
                    log.info("[PccBrowser] 刷新後已進入詳細頁，attempt={}", attempt);
                    return page.content();
                }
                continue;
            }

            for (ElementHandle cb : checkboxes) {
                cb.evaluate("el => el.checked = false");
            }

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

            ElementHandle submit = page.querySelector("#b_submit");
            if (submit == null) submit = page.querySelector("input[value='確認送出']");
            if (submit == null) submit = page.querySelector("button");
            if (submit != null) {
                try { submit.click(new ElementHandle.ClickOptions().setNoWaitAfter(true)); }
                catch (Exception e) { log.warn("[PccBrowser] submit click 例外（忽略）: {}", e.getMessage()); }
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

    // ── 招標公告列表頁爬取 ─────────────────────────────────────────────────────

    public List<PccListRow> scrapeListPage(Page page, String keyword, String orgName,
                                           LocalDate startDate, LocalDate endDate) {
        String url = buildSearchUrl(keyword, orgName, startDate, endDate);
        String label = orgName.isEmpty() ? "keyword='" + keyword + "'" : "orgName='" + orgName + "'";
        log.info("[PccBrowser] 招標搜尋列表 ({})", label);

        try {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(pageTimeoutMs)
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            page.waitForTimeout(1000);
        } catch (Exception e) {
            log.error("[PccBrowser] 載入招標列表頁失敗: {}", e.getMessage());
            return List.of();
        }

        List<PccListRow> results = new ArrayList<>();
        int pageIndex = 1;

        while (true) {
            List<ElementHandle> rows = page.querySelectorAll(
                    "table.tb_01 tbody tr, table#tpam tbody tr, tr.tb_b2");

            int before = results.size();
            for (ElementHandle row : rows) {
                try {
                    PccListRow listRow = parseListRow(row);
                    if (listRow != null) results.add(listRow);
                } catch (Exception e) {
                    log.warn("[PccBrowser] 解析招標列表行失敗: {}", e.getMessage());
                }
            }
            int pageCount = results.size() - before;
            log.info("[PccBrowser] 招標 ({}) 第{}頁 找到 {} 筆，累計 {} 筆", label, pageIndex, pageCount, results.size());

            if (pageCount < 50) break;

            ElementHandle nextBtn = page.querySelector("a[title='下一頁'], a:text('下一頁'), input[value='下一頁'], " +
                    ".pageNav a:last-child, #pageNav a:last-child, a.next, li.next a");
            if (nextBtn == null) break;

            log.info("[PccBrowser] 招標 ({}) 翻頁至第 {} 頁", label, pageIndex + 1);
            try {
                nextBtn.click(new ElementHandle.ClickOptions().setNoWaitAfter(true));
                page.waitForLoadState(LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(pageTimeoutMs));
                page.waitForTimeout(1000);
            } catch (Exception e) {
                log.warn("[PccBrowser] 招標 ({}) 翻頁失敗: {}，停止翻頁", label, e.getMessage());
                break;
            }
            pageIndex++;
        }

        log.info("[PccBrowser] 招標 ({}) 全部共 {} 筆（共 {} 頁）", label, results.size(), pageIndex);
        return results;
    }

    private PccListRow parseListRow(ElementHandle row) {
        List<ElementHandle> cols = row.querySelectorAll("td");
        if (cols.size() < 9) return null;

        String agencyName = cellText(cols.get(1));

        String col2Text = cellText(cols.get(2));
        String tenderNameFromScript = extractScriptName(cols.get(2));
        String[] parts = Arrays.stream(col2Text.split("\\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        String tenderNumber = parts.length > 0 ? parts[0] : "";
        String tenderName = tenderNameFromScript.isEmpty()
                ? (parts.length > 1 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)) : "")
                : tenderNameFromScript;

        String detailUrl = "";
        ElementHandle viewLink = row.querySelector("a[href*=\"tpam?pk=\"], a[href*=\"urlSelector\"]");
        if (viewLink == null) viewLink = row.querySelector("a");
        if (viewLink != null) {
            String href = viewLink.getAttribute("href");
            if (href != null && !href.isEmpty()) {
                detailUrl = href.startsWith("http") ? href : "https://web.pcc.gov.tw" + href;
            }
        }

        if (tenderNumber.isEmpty() && tenderName.isEmpty()) return null;

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

    private Integer toInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private String buildSearchUrl(String keyword, String orgName, LocalDate startDate, LocalDate endDate) {
        String start = startDate.format(DATE_FMT);
        String end = endDate.format(DATE_FMT);
        return BASE_URL
                + "?firstSearch=true"
                + "&searchType=basic"
                + "&isBinding=N"
                + "&isLogIn=N"
                + "&orgName=" + encode(orgName)
                + "&orgId="
                + "&tenderName=" + encode(keyword)
                + "&tenderId="
                + "&tenderType=TENDER_DECLARATION"
                + "&tenderWay=TENDER_WAY_ALL_DECLARATION"
                + "&dateType=isSpdt"
                + "&tenderStartDate=" + encode(start)
                + "&tenderEndDate=" + encode(end)
                + "&radProctrgCate="
                + "&policyAdvocacy=";
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
}
