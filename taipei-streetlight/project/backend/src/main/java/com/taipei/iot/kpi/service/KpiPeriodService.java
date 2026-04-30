package com.taipei.iot.kpi.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.kpi.dto.PeriodResponse;
import com.taipei.iot.kpi.entity.KpiPeriod;
import com.taipei.iot.kpi.repository.KpiPeriodRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiPeriodService {

    private final KpiPeriodRepository periodRepository;

    public List<PeriodResponse> getPeriods() {
        String tenantId = TenantContext.getCurrentTenantId();
        return periodRepository.findByTenantIdOrderByPeriodYearDescPeriodMonthDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PeriodResponse lock(int year, int month) {
        String tenantId = TenantContext.getCurrentTenantId();
        String username = SecurityContextUtils.getCurrentUsername();

        KpiPeriod period = periodRepository
                .findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, month)
                .orElseGet(() -> KpiPeriod.builder()
                        .periodYear(year)
                        .periodMonth(month)
                        .build());

        if (Boolean.TRUE.equals(period.getLocked())) {
            throw new BusinessException(ErrorCode.KPI_PERIOD_LOCKED);
        }

        period.setLocked(true);
        period.setLockedAt(LocalDateTime.now());
        period.setLockedBy(username);
        period.setUnlockReason(null);

        return toResponse(periodRepository.save(period));
    }

    @Transactional
    public PeriodResponse unlock(int year, int month, String reason) {
        String tenantId = TenantContext.getCurrentTenantId();

        KpiPeriod period = periodRepository
                .findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, month)
                .orElseThrow(() -> new BusinessException(ErrorCode.KPI_PERIOD_NOT_LOCKED));

        if (!Boolean.TRUE.equals(period.getLocked())) {
            throw new BusinessException(ErrorCode.KPI_PERIOD_NOT_LOCKED);
        }

        period.setLocked(false);
        period.setUnlockReason(reason);

        return toResponse(periodRepository.save(period));
    }

    private PeriodResponse toResponse(KpiPeriod p) {
        return PeriodResponse.builder()
                .id(p.getId())
                .periodYear(p.getPeriodYear())
                .periodMonth(p.getPeriodMonth())
                .locked(p.getLocked())
                .lockedAt(p.getLockedAt())
                .lockedBy(p.getLockedBy())
                .unlockReason(p.getUnlockReason())
                .build();
    }
}
