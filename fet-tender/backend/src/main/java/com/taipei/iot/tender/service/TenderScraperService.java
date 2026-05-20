package com.taipei.iot.tender.service;

import com.microsoft.playwright.Page;
import com.taipei.iot.tender.config.TenderScraperProperties;
import com.taipei.iot.tender.dto.TenderScrapeResult;
import com.taipei.iot.tender.entity.AnnouncementAgencyFilter;
import com.taipei.iot.tender.entity.AnnouncementSearchKeyword;
import com.taipei.iot.tender.entity.TenderAnnouncement;
import com.taipei.iot.tender.repository.AnnouncementAgencyFilterRepository;
import com.taipei.iot.tender.repository.AnnouncementSearchKeywordRepository;
import com.taipei.iot.tender.repository.TenderAnnouncementRepository;
import com.taipei.iot.tender.scraper.PccBrowserService;
import com.taipei.iot.tender.scraper.PccDetailParser;
import com.taipei.iot.tender.scraper.PccListRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 招標公告爬蟲主服務（Spring Boot 原生實作，取代 Python v3）。
 *
 * 搜尋邏輯：
 *   1. 讀取 announcement_search_keywords（標案名稱關鍵字）
 *   2. 讀取 announcement_agency_filters（機關過濾設定）
 *   3. 一般 solution：以關鍵字搜尋 → 若有設定則 AND 過濾機關名稱
 *   4. org-only solution（ESG-建研所補助 等）：改以每個機關名稱直接搜尋
 *   5. 每筆結果進詳細頁，破解撲克牌 CAPTCHA 後解析詳細資料
 *   6. Upsert 至 tender_announcement 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenderScraperService {

    private static final Pattern BUDGET_PATTERN = Pattern.compile("[\\d,]+");

    private final TenderScraperProperties props;
    private final AnnouncementSearchKeywordRepository keywordRepo;
    private final AnnouncementAgencyFilterRepository agencyFilterRepo;
    private final TenderAnnouncementRepository announcementRepo;

    // ── 主流程 ────────────────────────────────────────────────────────────────

    public TenderScrapeResult runAndImport() {
        log.info("[TenderScraper] ===== 開始執行 =====");

        // 1. 讀取搜尋設定
        List<AnnouncementSearchKeyword> keywords =
                keywordRepo.findByIsActiveTrueOrderBySolutionAscKeywordAsc();
        List<AnnouncementAgencyFilter> allFilters =
                agencyFilterRepo.findByIsActiveTrueOrderBySolutionAscAgencyKeywordAsc();

        // 按 solution 分組
        Map<String, List<AnnouncementAgencyFilter>> filtersBySolution =
                allFilters.stream().collect(Collectors.groupingBy(AnnouncementAgencyFilter::getSolution));

        // org-only solutions：該 solution 的所有 filter 都是 is_org_only_search=true
        Set<String> orgOnlySolutions = filtersBySolution.entrySet().stream()
                .filter(e -> e.getValue().stream().allMatch(AnnouncementAgencyFilter::getIsOrgOnlySearch))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        log.info("[TenderScraper] 關鍵字: {} 個，org-only solutions: {}", keywords.size(), orgOnlySolutions);

        List<TenderAnnouncement> allResults = new ArrayList<>();
        // solution → (keyword/orgName → count)，用於郵件摘要
        Map<String, Map<String, Integer>> solutionKeyCounts = new LinkedHashMap<>();
        LocalDateTime scrapedAt = LocalDateTime.now();

        // 2. 爬蟲（每次 run 建一個 browser，結束後自動關閉）
        try (PccBrowserService browser = new PccBrowserService(props.getRequestDelayMs(), props.getPageTimeoutMs())) {
            Page page = browser.newPage();

            // 2a. 標案名稱關鍵字搜尋（排除 org-only solutions）
            for (AnnouncementSearchKeyword kw : keywords) {
                if (orgOnlySolutions.contains(kw.getSolution())) continue;

                List<PccListRow> rows = browser.scrapeListPage(page, kw.getKeyword(), "");

                // 採購性質過濾（對應 Python FILTER_PROCUREMENT_TYPE）
                rows = filterByProcurementType(rows, kw.getKeyword());

                // 機關名稱後過濾（is_org_only_search=false 的 filter）
                List<String> agencyKeywords = filtersBySolution
                        .getOrDefault(kw.getSolution(), List.of()).stream()
                        .filter(f -> !f.getIsOrgOnlySearch())
                        .map(AnnouncementAgencyFilter::getAgencyKeyword)
                        .toList();
                if (!agencyKeywords.isEmpty()) {
                    int before = rows.size();
                    rows = rows.stream()
                            .filter(r -> agencyKeywords.stream().anyMatch(ak -> r.agencyName().contains(ak)))
                            .toList();
                    log.info("[TenderScraper] keyword='{}' 機關名稱過濾: {} -> {} 筆",
                            kw.getKeyword(), before, rows.size());
                }

                for (PccListRow row : rows) {
                    allResults.add(fetchAndBuild(browser, page, row, kw.getSolution(), kw.getKeyword(), scrapedAt));
                    browser.delay();
                }
                // 記錄計數
                solutionKeyCounts
                        .computeIfAbsent(kw.getSolution(), k -> new LinkedHashMap<>())
                        .merge(kw.getKeyword(), rows.size(), Integer::sum);
                browser.delay();
            }

            // 2b. 機關名稱直接搜尋（org-only solutions）
            for (String solution : orgOnlySolutions) {
                List<AnnouncementAgencyFilter> filters =
                        filtersBySolution.getOrDefault(solution, List.of());
                log.info("[TenderScraper] 機關搜尋 [{}]: {} 個機關", solution, filters.size());

                for (AnnouncementAgencyFilter filter : filters) {
                    List<PccListRow> rows = browser.scrapeListPage(page, "", filter.getAgencyKeyword());

                    // 採購性質過濾
                    rows = filterByProcurementType(rows, filter.getAgencyKeyword());

                    for (PccListRow row : rows) {
                        allResults.add(fetchAndBuild(browser, page, row, solution, filter.getAgencyKeyword(), scrapedAt));
                        browser.delay();
                    }
                    // 記錄計數
                    solutionKeyCounts
                            .computeIfAbsent(solution, k -> new LinkedHashMap<>())
                            .merge(filter.getAgencyKeyword(), rows.size(), Integer::sum);
                    browser.delay();
                }
            }
        }

        log.info("[TenderScraper] 爬蟲完成，共 {} 筆，開始寫入 DB", allResults.size());
        int count = upsertAll(allResults);
        log.info("[TenderScraper] ===== 完成，upsert {} 筆 =====", count);
        return new TenderScrapeResult(count, solutionKeyCounts, allResults);
    }

    // ── 採購性質過濾 ───────────────────────────────────────────────────────────

    private List<PccListRow> filterByProcurementType(List<PccListRow> rows, String label) {
        List<String> filter = props.getProcurementTypeFilter();
        if (filter == null || filter.isEmpty()) return rows;
        int before = rows.size();
        List<PccListRow> filtered = rows.stream()
                .filter(r -> filter.stream().anyMatch(pt ->
                        r.procurementType() != null && r.procurementType().contains(pt)))
                .toList();
        if (filtered.size() != before) {
            log.info("[TenderScraper] '{}' 採購性質過濾: {} -> {} 筆", label, before, filtered.size());
        }
        return filtered;
    }

    // ── 詳細頁抓取 + Entity 組裝 ─────────────────────────────────────────────

    private TenderAnnouncement fetchAndBuild(
            PccBrowserService browser, Page page,
            PccListRow row, String solution, String matchedKeyword, LocalDateTime scrapedAt) {

        TenderAnnouncement ann = buildFromListRow(row, solution, matchedKeyword, scrapedAt);

        if (row.detailUrl() != null && !row.detailUrl().isEmpty()) {
            String html = browser.fetchDetailPageHtml(page, row.detailUrl());
            if (html != null) {
                Map<String, String> detail = PccDetailParser.parse(html);
                enrichFromDetail(ann, detail);
            }
        }
        return ann;
    }

    private TenderAnnouncement buildFromListRow(
            PccListRow row, String solution, String matchedKeyword, LocalDateTime scrapedAt) {
        return TenderAnnouncement.builder()
                .solution(solution)
                .matchedKeyword(matchedKeyword)
                .agencyName(row.agencyName())
                .tenderNumber(row.tenderNumber())
                .tenderName(row.tenderName())
                .transmissionCount(row.transmissionCount())
                .tenderMethod(row.tenderMethod())
                .procurementType(row.procurementType())
                .announcementDate(parseRocDate(row.announcementDateRaw()))
                .deadline(parseRocDateTime(row.deadlineRaw()))
                .budgetAmountRaw(row.budgetRaw())
                .budgetAmount(parseBudget(row.budgetRaw()))
                .detailUrl(row.detailUrl())
                .scrapedAt(scrapedAt)
                .build();
    }

    private void enrichFromDetail(TenderAnnouncement ann, Map<String, String> detail) {
        Optional.ofNullable(detail.get("標案名稱")).filter(s -> !s.isEmpty())
                .ifPresent(ann::setTenderName);
        Optional.ofNullable(detail.get("標案案號")).filter(s -> !s.isEmpty())
                .ifPresent(ann::setTenderNumber);

        ann.setAgencyCode(detail.get("機關代碼"));
        ann.setUnitName(detail.get("單位名稱"));
        ann.setAgencyAddress(detail.get("機關地址"));
        ann.setContactPerson(detail.get("聯絡人"));
        ann.setContactPhone(detail.get("聯絡電話"));
        ann.setContactEmail(detail.get("電子郵件信箱"));
        ann.setTenderCategory(detail.get("標的分類"));
        ann.setProcurementAmountRange(detail.get("採購金額級距"));
        ann.setHandlingMethod(detail.get("辦理方式"));
        ann.setAwardMethod(detail.get("決標方式"));
        ann.setTenderStatus(detail.get("招標狀態"));
        ann.setOpeningTime(parseRocDateTime(detail.get("開標時間")));
        ann.setOpeningLocation(detail.get("開標地點"));
        ann.setHasBasePrice(parseBoolean(detail.get("是否訂有底價")));
        ann.setPerformanceLocation(detail.get("履約地點"));
    }

    // ── DB Upsert ────────────────────────────────────────────────────────────

    @Transactional
    public int upsertAll(List<TenderAnnouncement> rows) {
        int count = 0;
        for (TenderAnnouncement incoming : rows) {
            if (incoming.getTenderNumber() == null || incoming.getTenderNumber().isBlank()) continue;
            if (incoming.getAnnouncementDate() == null) continue;

            Optional<TenderAnnouncement> existing = announcementRepo
                    .findBySolutionAndMatchedKeywordAndTenderNumberAndAnnouncementDate(
                            incoming.getSolution(),
                            incoming.getMatchedKeyword(),
                            incoming.getTenderNumber(),
                            incoming.getAnnouncementDate());

            if (existing.isPresent()) {
                TenderAnnouncement e = existing.get();
                e.setTransmissionCount(incoming.getTransmissionCount());
                e.setTenderStatus(incoming.getTenderStatus());
                e.setDeadline(incoming.getDeadline());
                e.setBudgetAmountRaw(incoming.getBudgetAmountRaw());
                e.setBudgetAmount(incoming.getBudgetAmount());
                e.setOpeningTime(incoming.getOpeningTime());
                e.setOpeningLocation(incoming.getOpeningLocation());
                e.setScrapedAt(incoming.getScrapedAt());
                announcementRepo.save(e);
            } else {
                announcementRepo.save(incoming);
            }
            count++;
        }
        return count;
    }

    // ── 日期解析（民國曆 -> 西元） ─────────────────────────────────────────────

    private LocalDate parseRocDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String[] parts = s.trim().split("/");
            int year  = Integer.parseInt(parts[0]) + 1911;
            int month = Integer.parseInt(parts[1]);
            int day   = Integer.parseInt(parts[2].split(" ")[0]);
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseRocDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String[] slashParts = s.trim().split("/", 3);
            int year = Integer.parseInt(slashParts[0]) + 1911;
            String[] dayRest = slashParts[2].trim().split(" ", 2);
            int month = Integer.parseInt(slashParts[1]);
            int day   = Integer.parseInt(dayRest[0]);
            if (dayRest.length > 1) {
                String[] tp = dayRest[1].split(":");
                return LocalDateTime.of(year, month, day, Integer.parseInt(tp[0]), Integer.parseInt(tp[1]));
            }
            return LocalDate.of(year, month, day).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBudget(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Matcher m = BUDGET_PATTERN.matcher(raw);
        if (m.find()) {
            try { return new BigDecimal(m.group().replace(",", "")); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Boolean parseBoolean(String s) {
        if (s == null || s.isBlank()) return null;
        return "是".equals(s.trim());
    }
}
