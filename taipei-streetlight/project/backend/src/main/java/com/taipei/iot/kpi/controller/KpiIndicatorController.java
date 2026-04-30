package com.taipei.iot.kpi.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.kpi.dto.FormulaTestRequest;
import com.taipei.iot.kpi.dto.FormulaTestResponse;
import com.taipei.iot.kpi.dto.KpiIndicatorRequest;
import com.taipei.iot.kpi.dto.KpiIndicatorResponse;
import com.taipei.iot.kpi.enums.KpiCategory;
import com.taipei.iot.kpi.enums.KpiIndicatorStatus;
import com.taipei.iot.kpi.service.KpiIndicatorService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/kpi/indicators")
@RequiredArgsConstructor
public class KpiIndicatorController {

    private final KpiIndicatorService indicatorService;

    @GetMapping
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public BaseResponse<PageResponse<KpiIndicatorResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        KpiCategory cat = category != null ? KpiCategory.valueOf(category) : null;
        KpiIndicatorStatus st = status != null ? KpiIndicatorStatus.valueOf(status) : null;

        Page<KpiIndicatorResponse> result = indicatorService.list(cat, st, keyword, PageRequest.of(page, size));
        return BaseResponse.success(PageResponse.<KpiIndicatorResponse>builder()
                .content(result.getContent())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public BaseResponse<KpiIndicatorResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(indicatorService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('KPI_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_KPI_INDICATOR)
    public BaseResponse<KpiIndicatorResponse> create(@Valid @RequestBody KpiIndicatorRequest request) {
        return BaseResponse.success(indicatorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('KPI_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_KPI_INDICATOR)
    public BaseResponse<KpiIndicatorResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody KpiIndicatorRequest request) {
        return BaseResponse.success(indicatorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('KPI_MANAGE')")
    @AuditEvent(AuditEventType.DELETE_KPI_INDICATOR)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        indicatorService.delete(id);
        return BaseResponse.success(null);
    }

    @PostMapping("/test-formula")
    @PreAuthorize("hasAuthority('KPI_MANAGE')")
    public BaseResponse<FormulaTestResponse> testFormula(@Valid @RequestBody FormulaTestRequest request) {
        return BaseResponse.success(indicatorService.testFormula(request));
    }
}
