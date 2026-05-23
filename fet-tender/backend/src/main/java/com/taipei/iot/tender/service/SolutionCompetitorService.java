package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.*;
import com.taipei.iot.tender.repository.TenderAwardRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Solution 競品分析 — 以 solution + matched_keyword 維度，
 * 分析各 Solution 下哪些廠商最常得標、金額多寡，協助業務掌握競爭情勢。
 */
@Service
@RequiredArgsConstructor
public class SolutionCompetitorService {

    private final TenderAwardRepository repository;

    /** 取得所有 Solution 選項（供下拉選單）。 */
    @Transactional(readOnly = true)
    public List<String> listSolutions() {
        return repository.findDistinctSolutions(TenantContext.getCurrentTenantId())
                .stream()
                .map(VendorProjections.SolutionOption::getSolution)
                .collect(Collectors.toList());
    }

    /** KPI 摘要卡片。 */
    @Transactional(readOnly = true)
    public SolutionCompetitorSummaryResponse getSummary(
            String solution, String keyword, String dateFrom, String dateTo) {
        String tenantId = TenantContext.getCurrentTenantId();
        return repository.getSolutionOverview(tenantId, solution, nullIfBlank(keyword), nullIfBlank(dateFrom), nullIfBlank(dateTo))
                .stream().findFirst()
                .map(p -> SolutionCompetitorSummaryResponse.builder()
                        .totalTenders(p.getTotalTenders() == null ? 0L : p.getTotalTenders())
                        .totalAmount(p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                        .vendorCount(p.getVendorCount() == null ? 0L : p.getVendorCount())
                        .keywordCount(p.getKeywordCount() == null ? 0L : p.getKeywordCount())
                        .build())
                .orElse(SolutionCompetitorSummaryResponse.builder()
                        .totalTenders(0).totalAmount(BigDecimal.ZERO).vendorCount(0).keywordCount(0)
                        .build());
    }

    /** 廠商排行榜（分頁），附上名次序號。 */
    @Transactional(readOnly = true)
    public Page<SolutionVendorRankResponse> getVendorRank(
            String solution, String keyword, String dateFrom, String dateTo, Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenantId();
        Page<VendorProjections.SolutionVendorRank> raw =
                repository.getVendorRankBySolution(
                        tenantId, solution, nullIfBlank(keyword), nullIfBlank(dateFrom), nullIfBlank(dateTo), pageable);

        // 名次從全局 offset 開始 (e.g. 第 2 頁 page=1, size=10 → rank 從 11 開始)
        AtomicInteger rankStart = new AtomicInteger((int) pageable.getOffset() + 1);
        List<SolutionVendorRankResponse> content = raw.getContent().stream()
                .map(p -> SolutionVendorRankResponse.builder()
                        .rank(rankStart.getAndIncrement())
                        .vendorName(p.getVendorName())
                        .vendorTaxId(p.getVendorTaxId())
                        .winCount(p.getWinCount() == null ? 0L : p.getWinCount())
                        .totalAmount(p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, raw.getTotalElements());
    }

    /** Keyword 分布（用於長條圖）。 */
    @Transactional(readOnly = true)
    public List<SolutionKeywordSummaryResponse> getKeywordSummary(
            String solution, String dateFrom, String dateTo) {
        String tenantId = TenantContext.getCurrentTenantId();
        return repository.getKeywordSummaryBySolution(
                        tenantId, solution, nullIfBlank(dateFrom), nullIfBlank(dateTo))
                .stream()
                .map(p -> SolutionKeywordSummaryResponse.builder()
                        .keyword(p.getKeyword())
                        .vendorCount(p.getVendorCount() == null ? 0L : p.getVendorCount())
                        .winCount(p.getWinCount() == null ? 0L : p.getWinCount())
                        .totalAmount(p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                        .build())
                .collect(Collectors.toList());
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
