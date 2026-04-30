package com.taipei.iot.kpi.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.kpi.dto.FormulaTestRequest;
import com.taipei.iot.kpi.dto.FormulaTestResponse;
import com.taipei.iot.kpi.dto.KpiIndicatorRequest;
import com.taipei.iot.kpi.dto.KpiIndicatorResponse;
import com.taipei.iot.kpi.engine.FormulaEngine;
import com.taipei.iot.kpi.entity.KpiIndicator;
import com.taipei.iot.kpi.enums.FormulaType;
import com.taipei.iot.kpi.enums.KpiCategory;
import com.taipei.iot.kpi.enums.KpiDataSource;
import com.taipei.iot.kpi.enums.KpiIndicatorStatus;
import com.taipei.iot.kpi.repository.KpiIndicatorRepository;
import com.taipei.iot.kpi.repository.KpiRawDataRepository;
import com.taipei.iot.kpi.repository.KpiResultRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiIndicatorService {

    private final KpiIndicatorRepository indicatorRepository;
    private final KpiRawDataRepository rawDataRepository;
    private final KpiResultRepository resultRepository;
    private final FormulaEngine formulaEngine;

    public Page<KpiIndicatorResponse> list(KpiCategory category, KpiIndicatorStatus status,
                                           String keyword, Pageable pageable) {
        return indicatorRepository.findByFilters(category, status, keyword, pageable)
                .map(this::toResponse);
    }

    public KpiIndicatorResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public KpiIndicatorResponse create(KpiIndicatorRequest request) {
        String tenantId = TenantContext.getCurrentTenantId();

        // 檢查 code 重複
        indicatorRepository.findByTenantIdAndIndicatorCode(tenantId, request.getIndicatorCode())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.KPI_INDICATOR_CODE_DUPLICATE);
                });

        FormulaType formulaType = parseFormulaType(request.getFormulaType());

        // 驗證公式語法
        try {
            formulaEngine.validate(formulaType, request.getFormula());
        } catch (UnsupportedOperationException e) {
            throw new BusinessException(ErrorCode.KPI_FORMULA_TYPE_UNSUPPORTED);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KPI_FORMULA_INVALID);
        }

        KpiIndicator entity = KpiIndicator.builder()
                .indicatorCode(request.getIndicatorCode())
                .indicatorName(request.getIndicatorName())
                .category(KpiCategory.valueOf(request.getCategory()))
                .formulaType(formulaType)
                .formula(request.getFormula())
                .targetValue(request.getTargetValue())
                .weight(request.getWeight())
                .dataSource(request.getDataSource() != null ? KpiDataSource.valueOf(request.getDataSource()) : null)
                .unit(request.getUnit())
                .description(request.getDescription())
                .build();

        return toResponse(indicatorRepository.save(entity));
    }

    @Transactional
    public KpiIndicatorResponse update(Long id, KpiIndicatorRequest request) {
        KpiIndicator entity = findOrThrow(id);

        // 檢查 code 重複 (排除自身)
        indicatorRepository.findByTenantIdAndIndicatorCode(entity.getTenantId(), request.getIndicatorCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.KPI_INDICATOR_CODE_DUPLICATE);
                });

        FormulaType formulaType = parseFormulaType(request.getFormulaType());

        // 驗證公式語法
        try {
            formulaEngine.validate(formulaType, request.getFormula());
        } catch (UnsupportedOperationException e) {
            throw new BusinessException(ErrorCode.KPI_FORMULA_TYPE_UNSUPPORTED);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KPI_FORMULA_INVALID);
        }

        entity.setIndicatorCode(request.getIndicatorCode());
        entity.setIndicatorName(request.getIndicatorName());
        entity.setCategory(KpiCategory.valueOf(request.getCategory()));
        entity.setFormulaType(formulaType);
        entity.setFormula(request.getFormula());
        entity.setTargetValue(request.getTargetValue());
        entity.setWeight(request.getWeight());
        entity.setDataSource(request.getDataSource() != null ? KpiDataSource.valueOf(request.getDataSource()) : null);
        entity.setUnit(request.getUnit());
        entity.setDescription(request.getDescription());

        return toResponse(indicatorRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        KpiIndicator entity = findOrThrow(id);

        // 有歷史數據時僅停用
        boolean hasData = !rawDataRepository.findByIndicatorIdAndPeriodYearAndPeriodMonth(
                id, null, null).isEmpty()
                || resultRepository.findByFilters(null, null, null, id,
                Pageable.ofSize(1)).hasContent();

        if (hasData) {
            entity.setStatus(KpiIndicatorStatus.INACTIVE);
            indicatorRepository.save(entity);
        } else {
            indicatorRepository.delete(entity);
        }
    }

    public FormulaTestResponse testFormula(FormulaTestRequest request) {
        FormulaType formulaType = parseFormulaType(request.getFormulaType());
        try {
            var result = formulaEngine.evaluate(formulaType, request.getFormula(), request.getTestData());
            return FormulaTestResponse.builder()
                    .result(result)
                    .success(true)
                    .build();
        } catch (UnsupportedOperationException e) {
            return FormulaTestResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            return FormulaTestResponse.builder()
                    .success(false)
                    .errorMessage("公式執行錯誤: " + e.getMessage())
                    .build();
        }
    }

    // ── helpers ─────────────────────────────────────────────

    KpiIndicator findOrThrow(Long id) {
        return indicatorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.KPI_INDICATOR_NOT_FOUND));
    }

    private FormulaType parseFormulaType(String type) {
        if (type == null || type.isBlank()) return FormulaType.SPEL;
        try {
            return FormulaType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.KPI_FORMULA_TYPE_UNSUPPORTED);
        }
    }

    private KpiIndicatorResponse toResponse(KpiIndicator e) {
        return KpiIndicatorResponse.builder()
                .id(e.getId())
                .indicatorCode(e.getIndicatorCode())
                .indicatorName(e.getIndicatorName())
                .category(e.getCategory().name())
                .formulaType(e.getFormulaType().name())
                .formula(e.getFormula())
                .targetValue(e.getTargetValue())
                .weight(e.getWeight())
                .dataSource(e.getDataSource() != null ? e.getDataSource().name() : null)
                .unit(e.getUnit())
                .description(e.getDescription())
                .status(e.getStatus().name())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
