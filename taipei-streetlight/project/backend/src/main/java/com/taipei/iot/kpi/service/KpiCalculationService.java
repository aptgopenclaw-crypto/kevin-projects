package com.taipei.iot.kpi.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.kpi.dto.KpiResultResponse;
import com.taipei.iot.kpi.engine.FormulaEngine;
import com.taipei.iot.kpi.entity.KpiIndicator;
import com.taipei.iot.kpi.entity.KpiPeriod;
import com.taipei.iot.kpi.entity.KpiRawData;
import com.taipei.iot.kpi.entity.KpiResult;
import com.taipei.iot.kpi.enums.KpiIndicatorStatus;
import com.taipei.iot.kpi.repository.KpiIndicatorRepository;
import com.taipei.iot.kpi.repository.KpiPeriodRepository;
import com.taipei.iot.kpi.repository.KpiRawDataRepository;
import com.taipei.iot.kpi.repository.KpiResultRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiCalculationService {

    private final KpiIndicatorRepository indicatorRepository;
    private final KpiRawDataRepository rawDataRepository;
    private final KpiResultRepository resultRepository;
    private final KpiPeriodRepository periodRepository;
    private final FormulaEngine formulaEngine;

    public Page<KpiResultResponse> listResults(Integer periodYear, Integer periodMonth,
                                               Long contractId, Long indicatorId,
                                               Pageable pageable) {
        return resultRepository.findByFilters(periodYear, periodMonth, contractId, indicatorId, pageable)
                .map(this::toResponse);
    }

    /**
     * 手動觸發計算 — 指定期間的全部或單一指標。
     */
    @Transactional
    public int calculate(int year, int month, Long indicatorId) {
        String tenantId = TenantContext.getCurrentTenantId();

        // 檢查鎖定
        checkNotLocked(tenantId, year, month);

        List<KpiIndicator> indicators;
        if (indicatorId != null) {
            KpiIndicator ind = indicatorRepository.findById(indicatorId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.KPI_INDICATOR_NOT_FOUND));
            indicators = List.of(ind);
        } else {
            indicators = indicatorRepository.findByTenantIdAndStatus(tenantId, KpiIndicatorStatus.ACTIVE);
        }

        int count = 0;
        for (KpiIndicator indicator : indicators) {
            count += calculateForIndicator(tenantId, indicator, year, month);
        }

        log.info("KPI 計算完成: tenant={}, period={}/{}, indicators={}, results={}", tenantId, year, month, indicators.size(), count);
        return count;
    }

    /**
     * 排程月度計算 — 供 KpiCalculationJob 呼叫。
     */
    @Transactional
    public int calculateMonthly(int year, int month, String tenantId) {
        // 檢查鎖定
        KpiPeriod period = periodRepository
                .findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, month)
                .orElse(null);
        if (period != null && Boolean.TRUE.equals(period.getLocked())) {
            log.info("期間 {}/{} 已鎖定，跳過計算 (tenant={})", year, month, tenantId);
            return 0;
        }

        List<KpiIndicator> indicators = indicatorRepository
                .findByTenantIdAndStatus(tenantId, KpiIndicatorStatus.ACTIVE);

        int count = 0;
        for (KpiIndicator indicator : indicators) {
            count += calculateForIndicator(tenantId, indicator, year, month);
        }
        return count;
    }

    // ── private ─────────────────────────────────────────────

    private int calculateForIndicator(String tenantId, KpiIndicator indicator, int year, int month) {
        // 取得原始數據 (全市層級)
        List<KpiRawData> rawDataList = rawDataRepository
                .findByIndicatorIdAndPeriodYearAndPeriodMonth(indicator.getId(), year, month);

        if (rawDataList.isEmpty()) return 0;

        int count = 0;
        for (KpiRawData rawData : rawDataList) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("value", rawData.getRawValue());
                variables.put("target", indicator.getTargetValue() != null ? indicator.getTargetValue() : BigDecimal.ZERO);
                variables.put("weight", indicator.getWeight() != null ? indicator.getWeight() : BigDecimal.ONE);

                BigDecimal resultValue = formulaEngine.evaluate(
                        indicator.getFormulaType(), indicator.getFormula(), variables);

                BigDecimal achievement = null;
                if (indicator.getTargetValue() != null && indicator.getTargetValue().compareTo(BigDecimal.ZERO) != 0) {
                    achievement = resultValue
                            .divide(indicator.getTargetValue(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                }

                upsertResult(tenantId, indicator, year, month, rawData.getContractId(),
                        resultValue, indicator.getTargetValue(), achievement);
                count++;
            } catch (Exception e) {
                log.error("指標 {} 計算失敗: {}", indicator.getIndicatorCode(), e.getMessage(), e);
            }
        }
        return count;
    }

    private void upsertResult(String tenantId, KpiIndicator indicator, int year, int month,
                              Long contractId, BigDecimal resultValue, BigDecimal targetValue,
                              BigDecimal achievement) {
        KpiResult existing;
        if (contractId != null) {
            existing = resultRepository
                    .findByTenantIdAndIndicatorIdAndPeriodYearAndPeriodMonthAndContractId(
                            tenantId, indicator.getId(), year, month, contractId)
                    .orElse(null);
        } else {
            existing = resultRepository.findCityLevel(tenantId, indicator.getId(), year, month)
                    .orElse(null);
        }

        if (existing != null) {
            existing.setResultValue(resultValue);
            existing.setTargetValue(targetValue);
            existing.setAchievement(achievement);
            existing.setCalculatedAt(LocalDateTime.now());
            resultRepository.save(existing);
        } else {
            KpiResult result = KpiResult.builder()
                    .indicator(indicator)
                    .periodYear(year)
                    .periodMonth(month)
                    .contractId(contractId)
                    .resultValue(resultValue)
                    .targetValue(targetValue)
                    .achievement(achievement)
                    .build();
            resultRepository.save(result);
        }
    }

    private void checkNotLocked(String tenantId, int year, int month) {
        periodRepository.findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, month)
                .filter(p -> Boolean.TRUE.equals(p.getLocked()))
                .ifPresent(p -> {
                    throw new BusinessException(ErrorCode.KPI_PERIOD_LOCKED);
                });
    }

    private KpiResultResponse toResponse(KpiResult r) {
        return KpiResultResponse.builder()
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
                .build();
    }
}
