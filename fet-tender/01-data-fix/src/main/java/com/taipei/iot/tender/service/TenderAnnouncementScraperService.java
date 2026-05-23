package com.taipei.iot.tender.service;

import com.microsoft.playwright.Page;
import com.taipei.iot.tender.config.TenderAnnouncementScraperProperties;
import com.taipei.iot.tender.dto.AnnouncementScrapeResult;
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
 * 招標公告歷史資料補充服務（多租戶版）。
 * 邏輯與 backend TenderScraperService 相同，但支援指定日期區間逐日爬取。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenderAnnouncementScraperService {

    private static final Pattern BUDGET_PATTERN = Pattern.compile("[\\d,]+");

    private final TenderAnnouncementScraperProperties props;
    private final AnnouncementSearchKeywordRepository keywordRepo;
    private final AnnouncementAgencyFilterRepository agencyFilterRepo;
    private final TenderAnnouncementRepository announcementRepo;

    private String tenantId;

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public AnnouncementScrapeResult runAndImport(LocalDate from, LocalDate to) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("tenantId 未設定，請先呼叫 setTenantId()");
        }

        log.info("[AnnScraper] ===== 開始執行（租戶: {}，日期區間 {} ~ {}）=====", tenantId, from, to);

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

        log.info("[AnnScraper] 關鍵字: {} 個，org-only solutions: {}", keywords.size(), orgOnlySolutions);

        Map<String, Map<String, Integer>> solutionKeyCounts = new LinkedHashMap<>();
        LocalDateTime scrapedAt = LocalDateTime.now();

        List<LocalDate> dates = from.datesUntil(to.plusDays(1)).toList();
        int totalDays = dates.size();
        int totalCount = 0;

        try (PccBrowserService browser = new PccBrowserService(props.getRequestDelayMs(), props.getPageTimeoutMs())) {
            Page page = browser.newPage();

            for (int dayIdx = 0; dayIdx < totalDays; dayIdx++) {
                LocalDate date = dates.get(dayIdx);
                log.info("[AnnScraper] ──────────────────────────────────────");
                log.info("[AnnScraper] 處理日期: {}  （第 {}/{} 天）", date, dayIdx + 1, totalDays);
                log.info("[AnnScraper] ──────────────────────────────────────");
                List<TenderAnnouncement> dayResults = new ArrayList<>();

                // 標案名稱關鍵字搜尋
                for (AnnouncementSearchKeyword kw : keywords) {
                    if (orgOnlySolutions.contains(kw.getSolution())) continue;

                    List<PccListRow> rows = browser.scrapeListPage(page, kw.getKeyword(), "", date, date);
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
                        log.info("[AnnScraper] keyword='{}' 機關名稱過濾: {} -> {} 筆",
                                kw.getKeyword(), before, rows.size());
                    }

                    for (PccListRow row : rows) {
                        try {
                            dayResults.add(fetchAndBuild(browser, page, row, kw.getSolution(), kw.getKeyword(), scrapedAt));
                        } catch (Exception e) {
                            log.warn("[AnnScraper] 詳細頁處理失敗，略過 ({}): {}", row.tenderNumber(), e.getMessage());
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
                        List<PccListRow> rows = browser.scrapeListPage(page, "", filter.getAgencyKeyword(), date, date);
                        rows = filterByProcurementType(rows, filter.getAgencyKeyword());

                        for (PccListRow row : rows) {
                            try {
                                dayResults.add(fetchAndBuild(browser, page, row, solution, filter.getAgencyKeyword(), scrapedAt));
                            } catch (Exception e) {
                                log.warn("[AnnScraper] 詳細頁處理失敗，略過 ({}): {}", row.tenderNumber(), e.getMessage());
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
                log.info("[AnnScraper] [{}] 完成，本日 upsert {} 筆，累計 {} 筆（第 {}/{} 天）",
                        date, dayCount, totalCount, dayIdx + 1, totalDays);
                if (dayIdx + 1 < totalDays) {
                    log.info("[AnnScraper] 若中斷，可從下一天繼續：--from={}", dates.get(dayIdx + 1));
                }
            }
        }

        log.info("[AnnScraper] ===== 完成，共 upsert {} 筆 =====", totalCount);
        return new AnnouncementScrapeResult(totalCount, solutionKeyCounts);
    }

    private List<PccListRow> filterByProcurementType(List<PccListRow> rows, String label) {
        List<String> filter = props.getProcurementTypeFilter();
        if (filter == null || filter.isEmpty()) return rows;
        int before = rows.size();
        List<PccListRow> filtered = rows.stream()
                .filter(r -> filter.stream().anyMatch(pt ->
                        r.procurementType() != null && r.procurementType().contains(pt)))
                .toList();
        if (filtered.size() != before) {
            log.info("[AnnScraper] '{}' 採購性質過濾: {} -> {} 筆", label, before, filtered.size());
        }
        return filtered;
    }

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
                .tenantId(tenantId)
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

    @Transactional
    public int upsertAll(List<TenderAnnouncement> rows) {
        int count = 0;
        for (TenderAnnouncement incoming : rows) {
            if (incoming.getTenderNumber() == null || incoming.getTenderNumber().isBlank()) continue;
            if (incoming.getAnnouncementDate() == null) continue;

            Optional<TenderAnnouncement> existing = announcementRepo
                    .findByTenantIdAndSolutionAndMatchedKeywordAndTenderNumberAndAnnouncementDate(
                            incoming.getTenantId(),
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
                e.setAgencyCode(incoming.getAgencyCode());
                e.setUnitName(incoming.getUnitName());
                e.setAgencyAddress(incoming.getAgencyAddress());
                e.setContactPerson(incoming.getContactPerson());
                e.setContactPhone(incoming.getContactPhone());
                e.setContactEmail(incoming.getContactEmail());
                e.setTenderCategory(incoming.getTenderCategory());
                e.setProcurementAmountRange(incoming.getProcurementAmountRange());
                e.setHandlingMethod(incoming.getHandlingMethod());
                e.setAwardMethod(incoming.getAwardMethod());
                e.setHasBasePrice(incoming.getHasBasePrice());
                e.setPerformanceLocation(incoming.getPerformanceLocation());
                e.setScrapedAt(incoming.getScrapedAt());
                announcementRepo.save(e);
            } else {
                announcementRepo.save(incoming);
            }
            count++;
        }
        return count;
    }

    private LocalDate parseRocDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String[] parts = s.trim().split("/");
            int year = Integer.parseInt(parts[0]) + 1911;
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2].split(" ")[0]);
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
            int day = Integer.parseInt(dayRest[0]);
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
