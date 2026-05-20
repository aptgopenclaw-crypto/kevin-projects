package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.*;
import com.taipei.iot.tender.repository.TenderAwardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorDashboardService {

    private final TenderAwardRepository repository;

    // ── 廠商搜尋建議 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VendorSuggestResponse> suggest(String q) {
        String keyword = q == null ? "" : q.trim();
        return repository.suggestVendors(keyword).stream()
                .map(p -> VendorSuggestResponse.builder()
                        .vendorName(p.getVendorName())
                        .vendorTaxId(p.getVendorTaxId())
                        .winCount(p.getWinCount() == null ? 0L : p.getWinCount())
                        .build())
                .collect(Collectors.toList());
    }

    // ── KPI 摘要 ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VendorOverviewResponse getOverview(String vendorTaxId, String vendorName) {
        return repository.getVendorOverview(nullIfBlank(vendorTaxId), vendorName)
                .stream().findFirst()
                .map(p -> VendorOverviewResponse.builder()
                        .vendorName(p.getVendorName())
                        .vendorTaxId(p.getVendorTaxId())
                        .totalWins(p.getTotalWins() == null ? 0L : p.getTotalWins())
                        .totalAmount(p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                        .agencyCount(p.getAgencyCount() == null ? 0L : p.getAgencyCount())
                        .solutionCount(p.getSolutionCount() == null ? 0L : p.getSolutionCount())
                        .firstAwardDate(p.getFirstAwardDate())
                        .latestAwardDate(p.getLatestAwardDate())
                        .build())
                .orElse(VendorOverviewResponse.builder().vendorName(vendorName).build());
    }

    // ── 時間趨勢（自動粒度） ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VendorTrendResponse getTrend(String vendorTaxId, String vendorName) {
        String taxId = nullIfBlank(vendorTaxId);

        var dateRange = repository.getVendorDateRange(taxId, vendorName)
                .stream().findFirst().orElse(null);

        if (dateRange == null || dateRange.getMinDate() == null) {
            return VendorTrendResponse.builder().granularity("MONTH").points(List.of()).build();
        }

        long days = ChronoUnit.DAYS.between(dateRange.getMinDate(), dateRange.getMaxDate());
        String granularity;
        List<VendorProjections.Trend> rawPoints;

        if (days < 90) {
            granularity = "DAY";
            rawPoints = repository.trendByDay(taxId, vendorName);
        } else if (days < 730) {
            granularity = "MONTH";
            rawPoints = repository.trendByMonth(taxId, vendorName);
        } else {
            granularity = "QUARTER";
            rawPoints = repository.trendByQuarter(taxId, vendorName);
        }

        List<VendorTrendResponse.TrendPoint> points = rawPoints.stream()
                .map(p -> VendorTrendResponse.TrendPoint.builder()
                        .period(p.getPeriod())
                        .count(p.getCount() == null ? 0L : p.getCount())
                        .totalAmount(p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        return VendorTrendResponse.builder().granularity(granularity).points(points).build();
    }

    // ── Solution × Keyword Treemap ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VendorSolutionNode> getSolutionBreakdown(String vendorTaxId, String vendorName) {
        String taxId = nullIfBlank(vendorTaxId);

        // 先按 solution 分組，再把 keyword rows 掛進去
        Map<String, List<VendorProjections.SolutionRow>> grouped =
                repository.getSolutionBreakdown(taxId, vendorName)
                        .stream()
                        .collect(Collectors.groupingBy(r -> r.getSolution() == null ? "未分類" : r.getSolution()));

        return grouped.entrySet().stream()
                .map(e -> {
                    String solution = e.getKey();
                    List<VendorProjections.SolutionRow> rows = e.getValue();

                    long totalCount = rows.stream().mapToLong(r -> r.getCount() == null ? 0L : r.getCount()).sum();
                    BigDecimal totalAmount = rows.stream()
                            .map(r -> r.getTotalAmount() == null ? BigDecimal.ZERO : r.getTotalAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<VendorSolutionNode.KeywordNode> keywords = rows.stream()
                            .map(r -> VendorSolutionNode.KeywordNode.builder()
                                    .keyword(r.getKeyword() == null ? "未分類" : r.getKeyword())
                                    .count(r.getCount() == null ? 0L : r.getCount())
                                    .totalAmount(r.getTotalAmount() == null ? BigDecimal.ZERO : r.getTotalAmount())
                                    .build())
                            .collect(Collectors.toList());

                    return VendorSolutionNode.builder()
                            .solution(solution)
                            .count(totalCount)
                            .totalAmount(totalAmount)
                            .keywords(keywords)
                            .build();
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }

    // ── 機關排行 ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VendorTopAgencyResponse> getTopAgencies(String vendorTaxId, String vendorName, int limit) {
        String taxId = nullIfBlank(vendorTaxId);
        return repository.getTopAgencies(taxId, vendorName, limit).stream()
                .map(p -> VendorTopAgencyResponse.builder()
                        .agencyName(p.getAgencyName())
                        .agencyCode(p.getAgencyCode())
                        .count(p.getCount() == null ? 0L : p.getCount())
                        .totalAmount(p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                        .build())
                .collect(Collectors.toList());
    }

    // ── 採購屬性三分布 ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VendorProcurementProfileResponse getProcurementProfile(String vendorTaxId, String vendorName) {
        String taxId = nullIfBlank(vendorTaxId);

        List<VendorProcurementProfileResponse.NameCount> tenderMethods =
                toNameCounts(repository.countByTenderMethod(taxId, vendorName));
        List<VendorProcurementProfileResponse.NameCount> procurementTypes =
                toNameCounts(repository.countByProcurementType(taxId, vendorName));
        List<VendorProcurementProfileResponse.NameCount> awardMethods =
                toNameCounts(repository.countByAwardMethod(taxId, vendorName));

        return VendorProcurementProfileResponse.builder()
                .tenderMethods(tenderMethods)
                .procurementTypes(procurementTypes)
                .awardMethods(awardMethods)
                .build();
    }

    // ── 共同得標廠商 ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VendorCoVendorResponse> getCoVendors(String vendorTaxId, String vendorName) {
        String taxId = nullIfBlank(vendorTaxId);
        return repository.getCoVendors(taxId, vendorName).stream()
                .map(p -> VendorCoVendorResponse.builder()
                        .vendorName(p.getVendorName())
                        .vendorTaxId(p.getVendorTaxId())
                        .coCount(p.getCoCount() == null ? 0L : p.getCoCount())
                        .build())
                .collect(Collectors.toList());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private List<VendorProcurementProfileResponse.NameCount> toNameCounts(
            List<VendorProjections.TypeCount> rows) {
        return rows.stream()
                .map(r -> new VendorProcurementProfileResponse.NameCount(
                        r.getTypeName() == null ? "未填" : r.getTypeName(),
                        r.getCount() == null ? 0L : r.getCount()))
                .collect(Collectors.toList());
    }
}
