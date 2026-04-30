package com.taipei.iot.kpi.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.kpi.dto.KpiResultResponse;
import com.taipei.iot.kpi.repository.KpiResultRepository;
import com.taipei.iot.kpi.entity.KpiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/kpi/contractor")
@RequiredArgsConstructor
public class ContractorKpiController {

    private final KpiResultRepository resultRepository;

    @GetMapping("/results")
    @PreAuthorize("hasAuthority('KPI_CONTRACTOR_VIEW')")
    public BaseResponse<List<KpiResultResponse>> getContractorResults(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long contractId) {

        int queryYear = year != null ? year : java.time.LocalDate.now().getYear();

        List<KpiResult> results = resultRepository
                .findByContractIdAndPeriodYearOrderByPeriodMonthAsc(contractId, queryYear);

        List<KpiResultResponse> response = results.stream()
                .map(r -> KpiResultResponse.builder()
                        .id(r.getId())
                        .indicatorId(r.getIndicator().getId())
                        .indicatorCode(r.getIndicator().getIndicatorCode())
                        .indicatorName(r.getIndicator().getIndicatorName())
                        .category(r.getIndicator().getCategory().name())
                        .periodYear(r.getPeriodYear())
                        .periodMonth(r.getPeriodMonth())
                        .contractId(r.getContractId())
                        .resultValue(r.getResultValue())
                        .targetValue(r.getTargetValue())
                        .achievement(r.getAchievement())
                        .weight(r.getIndicator().getWeight())
                        .calculatedAt(r.getCalculatedAt())
                        .build())
                .toList();

        return BaseResponse.success(response);
    }
}
