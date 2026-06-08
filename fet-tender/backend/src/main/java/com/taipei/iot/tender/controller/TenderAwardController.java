package com.taipei.iot.tender.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.AwardScrapeResult;
import com.taipei.iot.tender.dto.TenderAwardQueryRequest;
import com.taipei.iot.tender.dto.TenderAwardResponse;
import com.taipei.iot.tender.service.TenderAwardExcelExporter;
import com.taipei.iot.tender.service.TenderAwardScraperService;
import com.taipei.iot.tender.service.TenderAwardService;
import com.taipei.iot.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1/tender/awards")
@RequiredArgsConstructor
public class TenderAwardController {

    private final TenderAwardService service;
    private final TenderAwardScraperService scraperService;
    private final TenderAwardExcelExporter excelExporter;

    @GetMapping
    @PreAuthorize("hasAuthority('tender:award:view')")
    public BaseResponse<PageResponse<TenderAwardResponse>> search(@Valid TenderAwardQueryRequest req) {
        return BaseResponse.success(service.search(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:award:view')")
    public BaseResponse<TenderAwardResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(service.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:award:delete')")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return BaseResponse.success(null);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('tender:award:export')")
    @AuditEvent(AuditEventType.EXPORT_TENDER_AWARD)
    public void export(@Valid TenderAwardQueryRequest req,
                       HttpServletResponse response) throws IOException {
        var rows = service.queryForExport(req);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=tender-awards.xlsx");
        response.getOutputStream().write(excelExporter.export(rows));
    }

    /**
     * 手動補充歷史決標資料。
     * 指定日期區間，逐日重新爬取政府採購網並 upsert 至 DB。
     *
     * 範例：POST /v1/tender/awards/scrape/history?from=2026-01-01&to=2026-05-13
     */
    @PostMapping("/scrape/history")
    @PreAuthorize("hasAuthority('tender:award:scrape:run')")
    public BaseResponse<AwardScrapeResult> scrapeHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return BaseResponse.success(scraperService.runAndImport(from, to));
    }
}
