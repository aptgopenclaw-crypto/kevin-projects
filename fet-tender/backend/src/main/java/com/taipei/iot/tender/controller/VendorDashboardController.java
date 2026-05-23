package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.*;
import com.taipei.iot.tender.service.VendorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 廠商得標分析 Dashboard API
 *
 * 所有端點共用 tender:award:view 權限。
 * 廠商識別以 vendorTaxId（精確）為主，無 taxId 時退回 vendorName 完全相符。
 */
@RestController
@RequestMapping("/v1/tender/vendor-dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('tender:award:view')")
public class VendorDashboardController {

    private final VendorDashboardService service;

    /**
     * 廠商模糊搜尋建議。
     * GET /v1/tender/vendor-dashboard/suggest?q=中華電
     */
    @GetMapping("/suggest")
    public BaseResponse<List<VendorSuggestResponse>> suggest(@RequestParam(defaultValue = "") String q) {
        if (q == null || q.trim().length() < 2) {
            return BaseResponse.success(Collections.emptyList());
        }
        return BaseResponse.success(service.suggest(q));
    }

    /**
     * KPI 摘要卡片。
     * GET /v1/tender/vendor-dashboard/overview?vendorTaxId=12345678&vendorName=xxx
     */
    @GetMapping("/overview")
    public BaseResponse<VendorOverviewResponse> overview(
            @RequestParam(required = false) String vendorTaxId,
            @RequestParam String vendorName) {
        return BaseResponse.success(service.getOverview(vendorTaxId, vendorName));
    }

    /**
     * 得標時間趨勢（自動依資料跨度選擇日/月/季粒度）。
     * GET /v1/tender/vendor-dashboard/trend?vendorTaxId=12345678&vendorName=xxx
     */
    @GetMapping("/trend")
    public BaseResponse<VendorTrendResponse> trend(
            @RequestParam(required = false) String vendorTaxId,
            @RequestParam String vendorName) {
        return BaseResponse.success(service.getTrend(vendorTaxId, vendorName));
    }

    /**
     * Solution × Keyword 業務版圖（Treemap 資料）。
     * GET /v1/tender/vendor-dashboard/solution-breakdown?vendorTaxId=xxx&vendorName=xxx
     */
    @GetMapping("/solution-breakdown")
    public BaseResponse<List<VendorSolutionNode>> solutionBreakdown(
            @RequestParam(required = false) String vendorTaxId,
            @RequestParam String vendorName) {
        return BaseResponse.success(service.getSolutionBreakdown(vendorTaxId, vendorName));
    }

    /**
     * 發包機關排行榜。
     * GET /v1/tender/vendor-dashboard/top-agencies?vendorTaxId=xxx&vendorName=xxx&limit=10
     */
    @GetMapping("/top-agencies")
    public BaseResponse<List<VendorTopAgencyResponse>> topAgencies(
            @RequestParam(required = false) String vendorTaxId,
            @RequestParam String vendorName,
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(limit, 30);
        return BaseResponse.success(service.getTopAgencies(vendorTaxId, vendorName, safeLimit));
    }

    /**
     * 採購屬性分布（招標方式 / 採購類型 / 決標方式）。
     * GET /v1/tender/vendor-dashboard/procurement-profile?vendorTaxId=xxx&vendorName=xxx
     */
    @GetMapping("/procurement-profile")
    public BaseResponse<VendorProcurementProfileResponse> procurementProfile(
            @RequestParam(required = false) String vendorTaxId,
            @RequestParam String vendorName) {
        return BaseResponse.success(service.getProcurementProfile(vendorTaxId, vendorName));
    }

    /**
     * 共同得標廠商排行（出現在同一標案的其他廠商）。
     * GET /v1/tender/vendor-dashboard/co-vendors?vendorTaxId=xxx&vendorName=xxx
     */
    @GetMapping("/co-vendors")
    public BaseResponse<List<VendorCoVendorResponse>> coVendors(
            @RequestParam(required = false) String vendorTaxId,
            @RequestParam String vendorName) {
        return BaseResponse.success(service.getCoVendors(vendorTaxId, vendorName));
    }
}
