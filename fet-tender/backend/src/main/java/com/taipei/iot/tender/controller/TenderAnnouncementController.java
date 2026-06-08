package com.taipei.iot.tender.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.dto.TenderAnnouncementQueryRequest;
import com.taipei.iot.tender.dto.TenderAnnouncementResponse;
import com.taipei.iot.tender.service.TenderAnnouncementExcelExporter;
import com.taipei.iot.tender.service.TenderAnnouncementService;
import com.taipei.iot.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/v1/tender/announcements")
@RequiredArgsConstructor
public class TenderAnnouncementController {

    private final TenderAnnouncementService service;
    private final TenderAnnouncementExcelExporter excelExporter;

    @GetMapping
    @PreAuthorize("hasAuthority('tender:announcement:view')")
    public BaseResponse<PageResponse<TenderAnnouncementResponse>> search(
            @Valid TenderAnnouncementQueryRequest req) {
        return BaseResponse.success(service.search(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:announcement:view')")
    public BaseResponse<TenderAnnouncementResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(service.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('tender:announcement:delete')")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return BaseResponse.success(null);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('tender:announcement:export')")
    @AuditEvent(AuditEventType.EXPORT_TENDER_ANNOUNCEMENT)
    public void export(@Valid TenderAnnouncementQueryRequest req,
                       HttpServletResponse response) throws IOException {
        var rows = service.queryForExport(req);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=tender-announcements.xlsx");
        response.getOutputStream().write(excelExporter.export(rows));
    }
}
