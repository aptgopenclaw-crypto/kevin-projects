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

@Slf4j
@Service
@RequiredArgsConstructor
public class TenderAwardScraperService {

    private final TenderAwardScraperProperties props;
    private final AnnouncementSearchKeywordRepository keywordRepo;
    private final AnnouncementAgencyFilterRepository agencyFilterRepo;
    private final TenderAwardRepository awardRepo;

    private String tenantId;

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public AwardScrapeResult runAndImport() {
        return runAndImport(LocalDate.now(), LocalDate.now());
    }

    public AwardScrapeResult runAndImport(LocalDate from, LocalDate to) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("tenantId 未設定，請先呼叫 setTenantId()");
        }

        log.info("[AwardScraper] ===== 開始執行（租戶: {}，日期區間 {} ~ {}）=====", tenantId, from, to);

        List<AnnouncementSearchKeyword> keywords =
                keywordRepo.findByTenantIdAndIsActiveTrueOrderBySolutionAscKeywordAsc(tenantId);
        List<AnnouncementAgencyFilter> allFilters =
                agencyFilterRepo.findByTenantIdAndIsActiveTrueOrderBySolutionAscAgencyKeywordAsc(tenantId);

        Map<String, List<AnnouncementAgencyFilter>> filtersBySolution =
                allFilters.stream().collect(Collectors.groupingBy(AnnouncementAgencyFilter::getSolution));

        Set<String> orgOnlySolutions = filtersBySolution.entrySet().stream()
                .filter(e -> e.getValue().stream().allMatch(AnnouncementAgencyFilter::getIsOrgOnlySearch))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        log.info("[AwardScraper] 關鍵字: {} 個，org-only solutions: {}", keywords.size(), orgOnlySolutions);

        Map<String, Map<String, Integer>> solutionKeyCounts = new LinkedHashMap<>();
        LocalDateTime scrapedAt = LocalDateTime.now();

        List<LocalDate> dates = from.datesUntil(to.plusDays(1)).toList();
        int totalDays = dates.size();

        int totalCount = 0;

        try (PccBrowserService browser = new PccBrowserService(props.getRequestDelayMs(), props.getPageTimeoutMs())) {
            Page page = browser.newPage();

            for (int dayIdx = 0; dayIdx < totalDays; dayIdx++) {
                LocalDate date = dates.get(dayIdx);
                log.info("[AwardScraper] ──────────────────────────────────────");
                log.info("[AwardScraper] 處理日期: {}  （第 {}/{} 天）", date, dayIdx + 1, totalDays);
                log.info("[AwardScraper] ──────────────────────────────────────");
                List<TenderAward> dayResults = new ArrayList<>();

                // 標案名稱關鍵字搜尋
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
                        try {
                            dayResults.addAll(fetchAndBuild(browser, page, row, kw.getSolution(), kw.getKeyword(), scrapedAt));
                        } catch (Exception e) {
                            log.warn("[AwardScraper] 詳細頁處理失敗，略過 ({}): {}", row.tenderNumber(), e.getMessage());
                        }
                        browser.delay();
                    }
                    solutionKeyCounts
                            .computeIfAbsent(kw.getSolution(), k -> new LinkedHashMap<>())
                            .merge(kw.getKeyword(), rows.size(), Integer::sum);
                    browser.delay();
                }

                // 機關名稱直接搜尋（org-only solutions）
                for (String solution : orgOnlySolutions) {
                    List<AnnouncementAgencyFilter> filters =
                            filtersBySolution.getOrDefault(solution, List.of());
                    for (AnnouncementAgencyFilter filter : filters) {
                        List<PccAwardListRow> rows = browser.scrapeAwardListPage(page, "", filter.getAgencyKeyword(), date, date);
                        rows = filterByProcurementType(rows, filter.getAgencyKeyword());

                        for (PccAwardListRow row : rows) {
                            try {
                                dayResults.addAll(fetchAndBuild(browser, page, row, solution, filter.getAgencyKeyword(), scrapedAt));
                            } catch (Exception e) {
                                log.warn("[AwardScraper] 詳細頁處理失敗，略過 ({}): {}", row.tenderNumber(), e.getMessage());
                            }
                            browser.delay();
                        }
                        solutionKeyCounts
                                .computeIfAbsent(solution, k -> new LinkedHashMap<>())
                                .merge(filter.getAgencyKeyword(), rows.size(), Integer::sum);
                        browser.delay();
                    }
                }

                // 每日爬取完畢後立即寫入 DB
                int dayCount = upsertAll(dayResults);
                totalCount += dayCount;
                log.info("[AwardScraper] [{}] 完成，本日 upsert {} 筆，累計 {} 筆（第 {}/{} 天）",
                        date, dayCount, totalCount, dayIdx + 1, totalDays);
                if (dayIdx + 1 < totalDays) {
                    log.info("[AwardScraper] 若中斷，可從下一天繼續：--from={}", dates.get(dayIdx + 1));
                }
            }
        }

        log.info("[AwardScraper] ===== 完成，共 upsert {} 筆 =====", totalCount);
        return new AwardScrapeResult(totalCount, solutionKeyCounts, List.of());
    }

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

    private List<TenderAward> fetchAndBuild(
            PccBrowserService browser, Page page,
            PccAwardListRow row, String solution, String matchedKeyword, LocalDateTime scrapedAt) {

        TenderAward base = buildFromListRow(row, solution, matchedKeyword, scrapedAt);

        if (row.detailUrl() == null || row.detailUrl().isEmpty()) {
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

        List<Map<String, String>> vendors = parsed.vendors().stream()
                .filter(v -> !"否".equals(v.get("是否得標")))
                .filter(v -> !v.getOrDefault("廠商名稱", "").isBlank())
                .toList();

        if (vendors.isEmpty()) {
            base.setVendorOrderSeq(1);
            return List.of(base);
        }

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
                .tenantId(tenantId)
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
        String taxId = vendor.getOrDefault("廠商代碼",
                vendor.getOrDefault("統一編號", vendor.get("登記字號")));
        award.setVendorTaxId(taxId);
        award.setVendorAddress(vendor.get("廠商地址"));
        award.setVendorPhone(vendor.get("廠商電話"));
        String amtRaw = vendor.get("決標金額");
        award.setVendorAwardAmountRaw(amtRaw);
        award.setVendorAwardAmount(parseAmount(amtRaw));
    }

    private TenderAward cloneBase(TenderAward base) {
        return TenderAward.builder()
                .tenantId(base.getTenantId())
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

    @Transactional
    public int upsertAll(List<TenderAward> rows) {
        int count = 0;
        for (TenderAward incoming : rows) {
            if (incoming.getTenderNumber() == null || incoming.getTenderNumber().isBlank()) continue;
            if (incoming.getAwardAnnounceDate() == null) continue;
            if (incoming.getVendorOrderSeq() == null) incoming.setVendorOrderSeq(1);

            String seqKey = incoming.getAwardAnnounceSeq() != null ? incoming.getAwardAnnounceSeq() : "";

            Optional<TenderAward> existing = awardRepo
                    .findByTenantIdAndSolutionAndMatchedKeywordAndTenderNumberAndAwardAnnounceDateAndAwardAnnounceSeqAndVendorOrderSeq(
                            incoming.getTenantId(),
                            incoming.getSolution(),
                            incoming.getMatchedKeyword(),
                            incoming.getTenderNumber(),
                            incoming.getAwardAnnounceDate(),
                            seqKey,
                            incoming.getVendorOrderSeq());

            if (existing.isPresent()) {
                TenderAward e = existing.get();
                e.setAwardAmountRaw(incoming.getAwardAmountRaw());
                e.setAwardAmount(incoming.getAwardAmount());
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
                e.setVendorName(incoming.getVendorName());
                e.setVendorTaxId(incoming.getVendorTaxId());
                e.setVendorAddress(incoming.getVendorAddress());
                e.setVendorPhone(incoming.getVendorPhone());
                e.setVendorAwardAmountRaw(incoming.getVendorAwardAmountRaw());
                e.setVendorAwardAmount(incoming.getVendorAwardAmount());
                e.setScrapedAt(incoming.getScrapedAt());
                awardRepo.save(e);
            } else {
                if (incoming.getAwardAnnounceSeq() == null) incoming.setAwardAnnounceSeq(seqKey);
                awardRepo.save(incoming);
            }
            count++;
        }
        return count;
    }

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
