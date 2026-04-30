package com.taipei.iot.kpi.service;

import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.kpi.engine.FormulaEngine;
import com.taipei.iot.kpi.entity.KpiIndicator;
import com.taipei.iot.kpi.entity.KpiPeriod;
import com.taipei.iot.kpi.entity.KpiRawData;
import com.taipei.iot.kpi.entity.KpiResult;
import com.taipei.iot.kpi.enums.FormulaType;
import com.taipei.iot.kpi.enums.KpiCategory;
import com.taipei.iot.kpi.enums.KpiDataSource;
import com.taipei.iot.kpi.enums.KpiIndicatorStatus;
import com.taipei.iot.kpi.repository.KpiIndicatorRepository;
import com.taipei.iot.kpi.repository.KpiPeriodRepository;
import com.taipei.iot.kpi.repository.KpiRawDataRepository;
import com.taipei.iot.kpi.repository.KpiResultRepository;
import com.taipei.iot.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiCalculationServiceTest {

    @InjectMocks private KpiCalculationService service;
    @Mock private KpiIndicatorRepository indicatorRepo;
    @Mock private KpiRawDataRepository rawDataRepo;
    @Mock private KpiResultRepository resultRepo;
    @Mock private KpiPeriodRepository periodRepo;
    @Mock private FormulaEngine formulaEngine;

    private MockedStatic<TenantContext> tenantMock;

    @BeforeEach
    void setUp() {
        tenantMock = mockStatic(TenantContext.class);
        tenantMock.when(TenantContext::getCurrentTenantId).thenReturn("T1");
    }

    @AfterEach
    void tearDown() {
        tenantMock.close();
    }

    private KpiIndicator buildIndicator() {
        return KpiIndicator.builder()
                .id(1L).tenantId("T1").indicatorCode("KPI-001")
                .indicatorName("路燈可用率")
                .category(KpiCategory.MAINTENANCE)
                .formulaType(FormulaType.SPEL)
                .formula("#value / #target * 100")
                .targetValue(new BigDecimal("100"))
                .weight(new BigDecimal("0.3"))
                .dataSource(KpiDataSource.REPAIR)
                .status(KpiIndicatorStatus.ACTIVE)
                .build();
    }

    // TC-08-010-01: 手動計算
    @Test
    void calculate_success() {
        KpiIndicator indicator = buildIndicator();
        when(periodRepo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(Optional.empty());
        when(indicatorRepo.findByTenantIdAndStatus("T1", KpiIndicatorStatus.ACTIVE))
                .thenReturn(List.of(indicator));

        KpiRawData rawData = KpiRawData.builder()
                .id(1L).indicator(indicator).periodYear(2025).periodMonth(3)
                .rawValue(new BigDecimal("95")).build();
        when(rawDataRepo.findByIndicatorIdAndPeriodYearAndPeriodMonth(1L, 2025, 3))
                .thenReturn(List.of(rawData));

        when(formulaEngine.evaluate(eq(FormulaType.SPEL), any(), any()))
                .thenReturn(new BigDecimal("95.0000"));

        when(resultRepo.findCityLevel("T1", 1L, 2025, 3))
                .thenReturn(Optional.empty());
        when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.calculate(2025, 3, null);
        assertEquals(1, count);
        verify(resultRepo).save(any(KpiResult.class));
    }

    // TC-08-010-02: 已鎖定期間
    @Test
    void calculate_lockedPeriod_throwsException() {
        KpiPeriod locked = KpiPeriod.builder()
                .id(1L).tenantId("T1").periodYear(2025).periodMonth(3).locked(true).build();
        when(periodRepo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(Optional.of(locked));

        assertThrows(BusinessException.class, () -> service.calculate(2025, 3, null));
    }

    // TC-08-009-01: 月度排程計算（鎖定跳過）
    @Test
    void calculateMonthly_locked_skips() {
        KpiPeriod locked = KpiPeriod.builder()
                .id(1L).tenantId("T1").periodYear(2025).periodMonth(2).locked(true).build();
        when(periodRepo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 2))
                .thenReturn(Optional.of(locked));

        int count = service.calculateMonthly(2025, 2, "T1");
        assertEquals(0, count);
        verifyNoInteractions(indicatorRepo);
    }

    // 計算單一指標
    @Test
    void calculate_singleIndicator() {
        KpiIndicator indicator = buildIndicator();
        when(periodRepo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 4))
                .thenReturn(Optional.empty());
        when(indicatorRepo.findById(1L)).thenReturn(Optional.of(indicator));

        KpiRawData rawData = KpiRawData.builder()
                .id(1L).indicator(indicator).periodYear(2025).periodMonth(4)
                .rawValue(new BigDecimal("88")).build();
        when(rawDataRepo.findByIndicatorIdAndPeriodYearAndPeriodMonth(1L, 2025, 4))
                .thenReturn(List.of(rawData));

        when(formulaEngine.evaluate(eq(FormulaType.SPEL), any(), any()))
                .thenReturn(new BigDecimal("88.0000"));
        when(resultRepo.findCityLevel("T1", 1L, 2025, 4))
                .thenReturn(Optional.empty());
        when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.calculate(2025, 4, 1L);
        assertEquals(1, count);
    }
}
