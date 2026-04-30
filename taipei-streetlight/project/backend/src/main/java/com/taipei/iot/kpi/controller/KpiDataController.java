package com.taipei.iot.kpi.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.kpi.dto.KpiRawDataResponse;
import com.taipei.iot.kpi.service.KpiDataService;
import com.taipei.iot.user.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/auth/kpi/data")
@RequiredArgsConstructor
public class KpiDataController {

    private final KpiDataService kpiDataService;

    @GetMapping
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public BaseResponse<PageResponse<KpiRawDataResponse>> list(
            @RequestParam(required = false) Long indicatorId,
            @RequestParam(required = false) Integer periodYear,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) Long contractId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<KpiRawDataResponse> result = kpiDataService.list(
                indicatorId, periodYear, periodMonth, contractId, PageRequest.of(page, size));
        return BaseResponse.success(PageResponse.<KpiRawDataResponse>builder()
                .content(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('KPI_MANAGE')")
    @AuditEvent(AuditEventType.IMPORT_KPI_DATA)
    public BaseResponse<Integer> importData(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        int count;
        if (filename != null && filename.endsWith(".csv")) {
            count = kpiDataService.importFromCsv(file);
        } else {
            count = kpiDataService.importFromExcel(file);
        }
        return BaseResponse.success(count);
    }
}
