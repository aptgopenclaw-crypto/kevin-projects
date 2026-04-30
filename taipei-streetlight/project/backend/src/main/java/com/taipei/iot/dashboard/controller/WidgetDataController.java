package com.taipei.iot.dashboard.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.dashboard.dto.AttachmentStatsResponse;
import com.taipei.iot.dashboard.dto.FaultCategoryResponse;
import com.taipei.iot.dashboard.dto.KpiSummaryResponse;
import com.taipei.iot.dashboard.dto.KpiTrendResponse;
import com.taipei.iot.dashboard.dto.LampCountResponse;
import com.taipei.iot.dashboard.dto.LampStatusResponse;
import com.taipei.iot.dashboard.dto.MaintenanceStatsResponse;
import com.taipei.iot.dashboard.dto.MaintenanceTrendResponse;
import com.taipei.iot.dashboard.dto.OutageAlertResponse;
import com.taipei.iot.dashboard.dto.OutageTrendResponse;
import com.taipei.iot.dashboard.dto.WidgetUnavailableResponse;
import com.taipei.iot.dashboard.service.WidgetAttachmentService;
import com.taipei.iot.dashboard.service.WidgetDeviceService;
import com.taipei.iot.dashboard.service.WidgetFaultService;
import com.taipei.iot.dashboard.service.WidgetKpiService;
import com.taipei.iot.dashboard.service.WidgetMaintenanceService;
import com.taipei.iot.dashboard.service.WidgetOutageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth/dashboard/widgets")
@RequiredArgsConstructor
public class WidgetDataController {

    private final WidgetMaintenanceService maintenanceService;
    private final WidgetOutageService outageService;
    private final WidgetFaultService faultService;
    private final WidgetKpiService kpiService;
    private final WidgetDeviceService deviceService;
    private final WidgetAttachmentService attachmentService;

    // ── 養護統計 ──

    @GetMapping("/maintenance")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<MaintenanceStatsResponse> maintenanceStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long contractId) {
        return BaseResponse.success(maintenanceService.getStats(startDate, endDate, contractId));
    }

    @GetMapping("/maintenance/trend")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<MaintenanceTrendResponse> maintenanceTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long contractId) {
        return BaseResponse.success(maintenanceService.getTrend(startDate, endDate, contractId));
    }

    // ── 停電告警 ──

    @GetMapping("/outage")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<OutageAlertResponse> outageAlert() {
        return BaseResponse.success(outageService.getCurrentOutages());
    }

    @GetMapping("/outage/trend")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<OutageTrendResponse> outageTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return BaseResponse.success(outageService.getOutageTrend(startDate, endDate));
    }

    // ── 故障 ──

    @GetMapping("/fault-heatmap")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<Map<String, Object>> faultHeatmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return BaseResponse.success(faultService.getHeatmapData(startDate, endDate));
    }

    @GetMapping("/fault-category")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<FaultCategoryResponse> faultCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return BaseResponse.success(faultService.getCategoryStats(startDate, endDate));
    }

    // ── KPI 績效 ──

    @GetMapping("/kpi")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<KpiSummaryResponse> kpiSummary(
            @RequestParam int year,
            @RequestParam int month) {
        return BaseResponse.success(kpiService.getSummary(year, month));
    }

    @GetMapping("/kpi/trend")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<KpiTrendResponse> kpiTrend(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "6") int months) {
        return BaseResponse.success(kpiService.getTrend(year, month, months));
    }

    // ── 路燈 ──

    @GetMapping("/lamp-count")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<LampCountResponse> lampCount(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Long contractId) {
        return BaseResponse.success(deviceService.getLampCount(district, contractId));
    }

    @GetMapping("/lamp-status")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<LampStatusResponse> lampStatus() {
        return BaseResponse.success(deviceService.getLampStatus());
    }

    // ── 附件 ──

    @GetMapping("/attachments")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<AttachmentStatsResponse> attachments() {
        return BaseResponse.success(attachmentService.getStats());
    }

    // ── Stub Widgets (Phase 7 IoT 完成後替換) ──

    @GetMapping("/panel-box")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<WidgetUnavailableResponse> panelBox() {
        return BaseResponse.success(WidgetUnavailableResponse.of("panel-box"));
    }

    @GetMapping("/panel-box/alerts")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<WidgetUnavailableResponse> panelBoxAlerts() {
        return BaseResponse.success(WidgetUnavailableResponse.of("panel-box"));
    }

    @GetMapping("/electricity-cost")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<WidgetUnavailableResponse> electricityCost() {
        return BaseResponse.success(WidgetUnavailableResponse.of("electricity-cost"));
    }

    @GetMapping("/electricity-cost/trend")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<WidgetUnavailableResponse> electricityCostTrend() {
        return BaseResponse.success(WidgetUnavailableResponse.of("electricity-cost"));
    }

    @GetMapping("/meter")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<WidgetUnavailableResponse> meter() {
        return BaseResponse.success(WidgetUnavailableResponse.of("meter"));
    }

    @GetMapping("/meter/trend")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<WidgetUnavailableResponse> meterTrend() {
        return BaseResponse.success(WidgetUnavailableResponse.of("meter"));
    }

    // ── GIS 總覽 (部分可用) ──

    @GetMapping("/gis")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public BaseResponse<WidgetUnavailableResponse> gisOverview() {
        // TODO: Phase 5C 完成後替換為 WidgetGisService 多圖層聚合
        return BaseResponse.success(WidgetUnavailableResponse.builder()
                .widgetType("gis-overview")
                .available(false)
                .message("GIS 功能待空間模組整合完成")
                .build());
    }
}
