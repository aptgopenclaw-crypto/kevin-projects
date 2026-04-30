package com.taipei.iot.kpi.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.kpi.dto.PeriodResponse;
import com.taipei.iot.kpi.service.KpiPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/kpi/periods")
@RequiredArgsConstructor
public class KpiPeriodController {

    private final KpiPeriodService periodService;

    @GetMapping
    @PreAuthorize("hasAuthority('KPI_VIEW')")
    public BaseResponse<List<PeriodResponse>> list() {
        return BaseResponse.success(periodService.getPeriods());
    }

    @PutMapping("/{year}-{month}/lock")
    @PreAuthorize("hasAuthority('KPI_LOCK')")
    @AuditEvent(AuditEventType.LOCK_KPI_PERIOD)
    public BaseResponse<PeriodResponse> lock(@PathVariable int year, @PathVariable int month) {
        return BaseResponse.success(periodService.lock(year, month));
    }

    @PutMapping("/{year}-{month}/unlock")
    @PreAuthorize("hasAuthority('KPI_UNLOCK')")
    @AuditEvent(AuditEventType.UNLOCK_KPI_PERIOD)
    public BaseResponse<PeriodResponse> unlock(@PathVariable int year, @PathVariable int month,
                                               @RequestParam String reason) {
        return BaseResponse.success(periodService.unlock(year, month, reason));
    }
}
