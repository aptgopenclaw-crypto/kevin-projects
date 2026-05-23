package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.*;
import com.taipei.iot.tender.service.SolutionCompetitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Solution 競品分析 API
 *
 * 以 solution + matched_keyword 為維度，分析哪些廠商在各 Solution 領域最常得標。
 */
@RestController
@RequestMapping("/v1/tender/solution-competitor")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('tender:award:view')")
public class SolutionCompetitorController {

    private final SolutionCompetitorService service;

    /**
     * 取得所有 Solution 下拉選項。
     * GET /v1/tender/solution-competitor/solutions
     */
    @GetMapping("/solutions")
    public BaseResponse<List<String>> listSolutions() {
        return BaseResponse.success(service.listSolutions());
    }

    /**
     * KPI 摘要卡片。
     * GET /v1/tender/solution-competitor/summary?solution=IoT&keyword=xxx&dateFrom=2024-01-01&dateTo=2024-12-31
     */
    @GetMapping("/summary")
    public BaseResponse<SolutionCompetitorSummaryResponse> summary(
            @RequestParam String solution,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return BaseResponse.success(service.getSummary(solution, keyword, dateFrom, dateTo));
    }

    /**
     * 廠商排行榜（分頁，依得標件數降冪）。
     * GET /v1/tender/solution-competitor/vendor-rank?solution=IoT&page=0&size=10
     */
    @GetMapping("/vendor-rank")
    public BaseResponse<Page<SolutionVendorRankResponse>> vendorRank(
            @RequestParam String solution,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(size, 50);
        return BaseResponse.success(
                service.getVendorRank(solution, keyword, dateFrom, dateTo, PageRequest.of(page, safeSize)));
    }

    /**
     * Keyword 分布長條圖資料。
     * GET /v1/tender/solution-competitor/keyword-summary?solution=IoT
     */
    @GetMapping("/keyword-summary")
    public BaseResponse<List<SolutionKeywordSummaryResponse>> keywordSummary(
            @RequestParam String solution,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return BaseResponse.success(service.getKeywordSummary(solution, dateFrom, dateTo));
    }
}
