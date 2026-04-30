package com.taipei.iot.kpi.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.kpi.dto.KpiResultResponse;
import com.taipei.iot.kpi.service.KpiCalculationService;
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

@RestController
@RequestMapping("/v1/auth/kpi")
@RequiredArgsConstructor
public class KpiCalculationController {

    private final KpiCalculationService calculationService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('KPI_MANAGE')")
    @AuditEvent(AuditEventType.CALCULATE_KPI)
    public BaseResponse<Integer> calculate(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long indicatorId) {
        return BaseResponse.success(calculationService.calculate(year, month, indicatorId));
    }

    @GetMapping("/results")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public BaseResponse<PageResponse<KpiResultResponse>> listResults(
            @RequestParam(required = false) Integer periodYear,
            @RequestParam(required = false) Integer periodMonth,
            @RequestParam(required = false) Long contractId,
            @RequestParam(required = false) Long indicatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<KpiResultResponse> result = calculationService.listResults(
                periodYear, periodMonth, contractId, indicatorId, PageRequest.of(page, size));
        return BaseResponse.success(PageResponse.<KpiResultResponse>builder()
                .content(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build());
    }
}
