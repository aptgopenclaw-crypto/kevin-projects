package com.taipei.iot.kpi.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.kpi.dto.CompareReportResponse;
import com.taipei.iot.kpi.dto.MonthlyReportResponse;
import com.taipei.iot.kpi.dto.YearlyReportResponse;
import com.taipei.iot.kpi.service.KpiReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/auth/kpi/reports")
@RequiredArgsConstructor
public class KpiReportController {

    private final KpiReportService reportService;

    @GetMapping("/monthly")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public BaseResponse<MonthlyReportResponse> monthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long contractId) {
        return BaseResponse.success(reportService.getMonthlyReport(year, month, contractId));
    }

    @GetMapping("/yearly")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public BaseResponse<YearlyReportResponse> yearlyReport(
            @RequestParam int year,
            @RequestParam(required = false) Long contractId) {
        return BaseResponse.success(reportService.getYearlyReport(year, contractId));
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public BaseResponse<CompareReportResponse> compareReport(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam List<Long> contractIds) {
        return BaseResponse.success(reportService.getCompareReport(year, month, contractIds));
    }

    @GetMapping("/export/xls")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public void exportXls(@RequestParam int year,
                          @RequestParam int month,
                          @RequestParam(required = false) Long contractId,
                          HttpServletResponse response) throws IOException {
        MonthlyReportResponse report = reportService.getMonthlyReport(year, month, contractId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                String.format("attachment; filename=\"kpi-report-%d-%02d.xlsx\"", year, month));
        reportService.exportXls(report, response.getOutputStream());
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public void exportCsv(@RequestParam int year,
                          @RequestParam int month,
                          @RequestParam(required = false) Long contractId,
                          HttpServletResponse response) throws IOException {
        MonthlyReportResponse report = reportService.getMonthlyReport(year, month, contractId);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                String.format("attachment; filename=\"kpi-report-%d-%02d.csv\"", year, month));
        reportService.exportCsv(report, response.getOutputStream());
    }
}
