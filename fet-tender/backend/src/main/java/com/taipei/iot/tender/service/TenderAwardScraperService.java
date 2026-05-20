package com.taipei.iot.tender.service;

import com.microsoft.playwright.Page;
import com.taipei.iot.tender.config.TenderAwardScraperProperties;
import com.taipei.iot.tender.dto.AwardScrapeResult;
import com.taipei.iot.tender.entity.AnnouncementAgencyFilter;
import com.taipei.iot.tender.entity.AnnouncementSearchKeyword;
import com.taipei.iot.tender.entity.TenderAward;
import com.taipei.iot.tender.repository.AnnouncementAgencyFilterRepository;
import com.taipei.iot.tender.repository.AnnouncementSearchKeywordRepository;
import com.taipei.iot.tender.repository.TenderAwardRepository;
import com.taipei.iot.tender.scraper.PccAwardDetailParser;
import com.taipei.iot.tender.scraper.PccAwardListRow;
import com.taipei.iot.tender.scraper.PccBrowserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 決標公告爬蟲主服務。
 *
 * 搜尋邏輯與招標公告（TenderScraperService）一致：
 *   1. 讀取 announcement_search_keywords / announcement_agency_filters
 *   2. 以關鍵字搜尋 readTenderAgent（tenderStatus=TENDER_STATUS_1）
 *   3. 機關名稱後過濾
 *   4. 進入決標詳細頁，破解撲克牌 CAPTCHA 後解析決標 + 廠商資料
 *   5. 每筆決標公告的每家得標廠商各 upsert 一列至 tender_award 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenderAwardScraperService {

    private final TenderAwardScraperProperties props;
    private final AnnouncementSearchKeywordRepository keywordRepo;
    private final AnnouncementAgencyFilterRepository agencyFilterRepo;
    private final TenderAwardRepository awardRepo;

    // ── 主流程 ────────────────────────────────────────────────────────────────

    public AwardScrapeResult runAndImport() {
        return runAndImport(LocalDate.now(), LocalDate.now());
    }

    /**
     * 補充歷史決標資料：指定日期區間逐日爬取並匯入。
     *
     * @param from 起始日期（含）
     * @param to   結束日期（含）
     */
    public AwardScrapeResult runAndImport(LocalDate from, LocalDate to) {
        log.info("[AwardScraper] ===== 開始執行（日期區間 {} ~ {}）=====", from, to);

        List<AnnouncementSearchKeyword> keywords =
                keywordRepo.findByIsActiveTrueOrderBySolutionAscKeywordAsc();
        List<AnnouncementAgencyFilter> allFilters =
                agencyFilterRepo.findByIsActiveTrueOrderBySolutionAscAgencyKeywordAsc();

        Map<String, List<AnnouncementAgencyFilter>> filtersBySolution =
                allFilters.stream().collect(Collectors.groupingBy(AnnouncementAgencyFilter::getSolution));

        Set<String> orgOnlySolutions = filtersBySolution.entrySet().stream()
                .filter(e -> e.getValue().stream().allMatch(AnnouncementAgencyFilter::getIsOrgOnlySearch))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        log.info("[AwardScraper] 關鍵字: {} 個，org-only solutions: {}", keywords.size(), orgOnlySolutions);

        List<TenderAward> allResults = new ArrayList<>();
        Map<String, Map<String, Integer>> solutionKeyCounts = new LinkedHashMap<>();
        LocalDateTime scrapedAt = LocalDateTime.now();

        // 逐日爬取
        List<LocalDate> dates = from.datesUntil(to.plusDays(1)).toList();

        try (PccBrowserService browser = new PccBrowserService(props.getRequestDelayMs(), props.getPageTimeoutMs())) {
            Page page = browser.newPage();

            for (LocalDate date : dates) {
                log.info("[AwardScraper] 處理日期: {}", date);

                // 2a. 標案名稱關鍵字搜尋
                for (AnnouncementSearchKeyword kw : keywords) {
                    if (orgOnlySolutions.contains(kw.getSolution())) continue;

                    List<PccAwardListRow> rows = browser.scrapeAwardListPage(page, kw.getKeyword(), "", date, date);
                    rows = filterByProcurementType(rows, kw.getKeyword());

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
                        log.info("[AwardScraper] keyword='{}' 機關名稱過濾: {} -> {} 筆",
                                kw.getKeyword(), before, rows.size());
                    }

                    for (PccAwardListRow row : rows) {
                        allResults.addAll(fetchAndBuild(browser, page, row, kw.getSolution(), kw.getKeyword(), scrapedAt));
                        browser.delay();
                    }
                    solutionKeyCounts
                            .computeIfAbsent(kw.getSolution(), k -> new LinkedHashMap<>())
                            .merge(kw.getKeyword(), rows.size(), Integer::sum);
                    browser.delay();
                }

                // 2b. 機關名稱直接搜尋（org-only solutions）
                for (String solution : orgOnlySolutions) {
                    List<AnnouncementAgencyFilter> filters =
                            filtersBySolution.getOrDefault(solution, List.of());
                    for (AnnouncementAgencyFilter filter : filters) {
                        List<PccAwardListRow> rows = browser.scrapeAwardListPage(page, "", filter.getAgencyKeyword(), date, date);
                        rows = filterByProcurementType(rows, filter.getAgencyKeyword());

                        for (PccAwardListRow row : rows) {
                            allResults.addAll(fetchAndBuild(browser, page, row, solution, filter.getAgencyKeyword(), scrapedAt));
                            browser.delay();
                        }
                        solutionKeyCounts
                                .computeIfAbsent(solution, k -> new LinkedHashMap<>())
                                .merge(filter.getAgencyKeyword(), rows.size(), Integer::sum);
                        browser.delay();
                    }
                }
            }
        }

        log.info("[AwardScraper] 爬蟲完成，共 {} 筆廠商記錄，開始寫入 DB", allResults.size());
        int count = upsertAll(allResults);
        log.info("[AwardScraper] ===== 完成，upsert {} 筆 =====", count);
        return new AwardScrapeResult(count, solutionKeyCounts, allResults);
    }

    // ── 採購性質過濾 ───────────────────────────────────────────────────────────

    private List<PccAwardListRow> filterByProcurementType(List<PccAwardListRow> rows, String label) {
        List<String> filter = props.getProcurementTypeFilter();
        if (filter == null || filter.isEmpty()) return rows;
        int before = rows.size();
        List<PccAwardListRow> filtered = rows.stream()
                .filter(r -> filter.stream().anyMatch(pt ->
                        r.procurementType() != null && r.procurementType().contains(pt)))
                .toList();
        if (filtered.size() != before) {
            log.info("[AwardScraper] '{}' 採購性質過濾: {} -> {} 筆", label, before, filtered.size());
        }
        return filtered;
    }

    // ── 詳細頁抓取 + Entity 組裝（每個廠商一筆）────────────────────────────────

    private List<TenderAward> fetchAndBuild(
            PccBrowserService browser, Page page,
            PccAwardListRow row, String solution, String matchedKeyword, LocalDateTime scrapedAt) {

        TenderAward base = buildFromListRow(row, solution, matchedKeyword, scrapedAt);

        if (row.detailUrl() == null || row.detailUrl().isEmpty()) {
            // 無詳細頁連結：以列表頁資料建立單筆（無廠商資料）
            base.setVendorOrderSeq(1);
            return List.of(base);
        }

        String html = browser.fetchDetailPageHtml(page, row.detailUrl());
        if (html == null) {
            base.setVendorOrderSeq(1);
            return List.of(base);
        }

        PccAwardDetailParser.ParseResult parsed = PccAwardDetailParser.parse(html);
        enrichFromDetail(base, parsed.fields());

        // 只保留得標廠商（是否得標 = 是），排除未得標的投標廠商
        // 同時過濾廠商名稱為空的記錄（防止決標品項區塊漏入）
        List<Map<String, String>> vendors = parsed.vendors().stream()
                .filter(v -> !"否".equals(v.get("是否得標")))
                .filter(v -> !v.getOrDefault("廠商名稱", "").isBlank())
                .toList();

        if (vendors.isEmpty()) {
            base.setVendorOrderSeq(1);
            return List.of(base);
        }

        // 每家廠商建立獨立記錄
        List<TenderAward> result = new ArrayList<>();
        for (int i = 0; i < vendors.size(); i++) {
            TenderAward award = cloneBase(base);
            award.setVendorOrderSeq(i + 1);
            enrichFromVendor(award, vendors.get(i));
            result.add(award);
        }
        return result;
    }

    private TenderAward buildFromListRow(
            PccAwardListRow row, String solution, String matchedKeyword, LocalDateTime scrapedAt) {
        return TenderAward.builder()
                .solution(solution)
                .matchedKeyword(matchedKeyword)
                .agencyName(row.agencyName())
                .tenderNumber(row.tenderNumber())
                .tenderName(row.tenderName())
                .tenderMethod(row.tenderMethod())
                .procurementType(row.procurementType())
                .awardAnnounceDate(parseRocDate(row.awardAnnounceDateRaw()))
                .awardAmountRaw(row.awardAmountRaw())
                .awardAmount(parseAmount(row.awardAmountRaw()))
                .awardAnnounceSeq(row.awardAnnounceSeq())
                .detailUrl(row.detailUrl())
                .scrapedAt(scrapedAt)
                .build();
    }

    private void enrichFromDetail(TenderAward award, Map<String, String> fields) {
        Optional.ofNullable(fields.get("標案名稱")).filter(s -> !s.isEmpty())
                .ifPresent(award::setTenderName);
        Optional.ofNullable(fields.get("標案案號")).filter(s -> !s.isEmpty())
                .ifPresent(award::setTenderNumber);

        award.setAgencyCode(fields.get("機關代碼"));
        award.setUnitName(fields.get("單位名稱"));
        award.setAgencyAddress(fields.get("機關地址"));
        award.setContactPerson(fields.get("聯絡人"));
        award.setContactPhone(fields.get("聯絡電話"));
        award.setContactEmail(fields.get("電子郵件信箱"));
        award.setTenderCategory(fields.get("標的分類"));
        award.setProcurementAmountRange(fields.get("採購金額級距"));
        award.setAwardMethod(fields.get("決標方式"));
        award.setHasBasePrice(parseBoolean(fields.get("是否訂有底價")));
        award.setAwardDate(parseRocDate(fields.get("決標日期")));
        award.setPerformancePeriod(fields.get("履約期限"));
        award.setPerformanceLocation(fields.get("履約地點"));
    }

    private void enrichFromVendor(TenderAward award, Map<String, String> vendor) {
        award.setVendorName(vendor.get("廠商名稱"));
        // PCC 決標頁用「廠商代碼」（8碼）作為廠商識別符，少數情況才會出現「統一編號」或「登記字號」
        String taxId = vendor.getOrDefault("廠商代碼",
                vendor.getOrDefault("統一編號",
                        vendor.get("登記字號")));
        award.setVendorTaxId(taxId);
        award.setVendorAddress(vendor.get("廠商地址"));
        award.setVendorPhone(vendor.get("廠商電話"));
        String amtRaw = vendor.get("決標金額");
        award.setVendorAwardAmountRaw(amtRaw);
        award.setVendorAwardAmount(parseAmount(amtRaw));
    }

    /** 淺複製 base 記錄（不含廠商欄位）以供每家廠商使用 */
    private TenderAward cloneBase(TenderAward base) {
        return TenderAward.builder()
                .solution(base.getSolution())
                .matchedKeyword(base.getMatchedKeyword())
                .agencyName(base.getAgencyName())
                .tenderNumber(base.getTenderNumber())
                .tenderName(base.getTenderName())
                .tenderMethod(base.getTenderMethod())
                .procurementType(base.getProcurementType())
                .awardAnnounceDate(base.getAwardAnnounceDate())
                .awardAmountRaw(base.getAwardAmountRaw())
                .awardAmount(base.getAwardAmount())
                .awardAnnounceSeq(base.getAwardAnnounceSeq())
                .detailUrl(base.getDetailUrl())
                .agencyCode(base.getAgencyCode())
                .unitName(base.getUnitName())
                .agencyAddress(base.getAgencyAddress())
                .contactPerson(base.getContactPerson())
                .contactPhone(base.getContactPhone())
                .contactEmail(base.getContactEmail())
                .tenderCategory(base.getTenderCategory())
                .procurementAmountRange(base.getProcurementAmountRange())
                .awardMethod(base.getAwardMethod())
                .hasBasePrice(base.getHasBasePrice())
                .awardDate(base.getAwardDate())
                .performancePeriod(base.getPerformancePeriod())
                .performanceLocation(base.getPerformanceLocation())
                .scrapedAt(base.getScrapedAt())
                .build();
    }

    // ── DB Upsert ────────────────────────────────────────────────────────────

    @Transactional
    public int upsertAll(List<TenderAward> rows) {
        int count = 0;
        for (TenderAward incoming : rows) {
            if (incoming.getTenderNumber() == null || incoming.getTenderNumber().isBlank()) continue;
            if (incoming.getAwardAnnounceDate() == null) continue;
            if (incoming.getVendorOrderSeq() == null) incoming.setVendorOrderSeq(1);

            String seqKey = incoming.getAwardAnnounceSeq() != null ? incoming.getAwardAnnounceSeq() : "";

            Optional<TenderAward> existing = awardRepo
                    .findBySolutionAndMatchedKeywordAndTenderNumberAndAwardAnnounceDateAndAwardAnnounceSeqAndVendorOrderSeq(
                            incoming.getSolution(),
                            incoming.getMatchedKeyword(),
                            incoming.getTenderNumber(),
                            incoming.getAwardAnnounceDate(),
                            seqKey,
                            incoming.getVendorOrderSeq());

            if (existing.isPresent()) {
                TenderAward e = existing.get();
                // 列表頁欄位
                e.setAwardAmountRaw(incoming.getAwardAmountRaw());
                e.setAwardAmount(incoming.getAwardAmount());
                // 詳細頁欄位（機關/採購/決標資料）
                e.setAgencyCode(incoming.getAgencyCode());
                e.setUnitName(incoming.getUnitName());
                e.setAgencyAddress(incoming.getAgencyAddress());
                e.setContactPerson(incoming.getContactPerson());
                e.setContactPhone(incoming.getContactPhone());
                e.setContactEmail(incoming.getContactEmail());
                e.setTenderCategory(incoming.getTenderCategory());
                e.setProcurementAmountRange(incoming.getProcurementAmountRange());
                e.setAwardMethod(incoming.getAwardMethod());
                e.setHasBasePrice(incoming.getHasBasePrice());
                e.setAwardDate(incoming.getAwardDate());
                e.setPerformancePeriod(incoming.getPerformancePeriod());
                e.setPerformanceLocation(incoming.getPerformanceLocation());
                // 廠商欄位
                e.setVendorName(incoming.getVendorName());
                e.setVendorTaxId(incoming.getVendorTaxId());
                e.setVendorAddress(incoming.getVendorAddress());
                e.setVendorPhone(incoming.getVendorPhone());
                e.setVendorAwardAmountRaw(incoming.getVendorAwardAmountRaw());
                e.setVendorAwardAmount(incoming.getVendorAwardAmount());
                e.setScrapedAt(incoming.getScrapedAt());
                awardRepo.save(e);
            } else {
                // 確保序號欄位有值
                if (incoming.getAwardAnnounceSeq() == null) incoming.setAwardAnnounceSeq(seqKey);
                awardRepo.save(incoming);
            }
            count++;
        }
        return count;
    }

    // ── 日期 / 金額解析 ───────────────────────────────────────────────────────

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

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.replaceAll("[^\\d,]", "").replace(",", "");
        if (cleaned.isEmpty()) return null;
        try { return new BigDecimal(cleaned); } catch (NumberFormatException e) { return null; }
    }

    private Boolean parseBoolean(String s) {
        if (s == null || s.isBlank()) return null;
        return "是".equals(s.trim());
    }
}
