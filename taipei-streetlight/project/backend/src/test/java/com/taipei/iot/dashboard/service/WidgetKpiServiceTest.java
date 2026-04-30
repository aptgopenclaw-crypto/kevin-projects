package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.KpiSummaryResponse;
import com.taipei.iot.dashboard.dto.KpiTrendResponse;
import com.taipei.iot.kpi.entity.KpiIndicator;
import com.taipei.iot.kpi.entity.KpiResult;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WidgetKpiServiceTest {

    @InjectMocks private WidgetKpiService service;
    @Mock private KpiResultRepository resultRepository;

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

    private KpiResult buildResult(String code, String name, BigDecimal value, BigDecimal target) {
        KpiIndicator indicator = KpiIndicator.builder()
                .indicatorCode(code).indicatorName(name).build();
        return KpiResult.builder()
                .indicator(indicator)
                .resultValue(value)
                .targetValue(target)
                .achievement(target.compareTo(BigDecimal.ZERO) != 0
                        ? value.multiply(BigDecimal.valueOf(100)).divide(target, 2, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .build();
    }

    // TC-10-010-01: KPI 摘要卡片
    @Test
    void getSummary_returnsIndicators() {
        when(resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(List.of(
                        buildResult("KPI-001", "路燈可用率", new BigDecimal("95"), new BigDecimal("100")),
                        buildResult("KPI-002", "維修及時率", new BigDecimal("88"), new BigDecimal("100"))
                ));

        KpiSummaryResponse resp = service.getSummary(2025, 3);

        assertNotNull(resp);
        assertEquals(2, resp.getIndicators().size());
        assertEquals("KPI-001", resp.getIndicators().get(0).getCode());
        assertEquals("A", resp.getIndicators().get(0).getGrade()); // 95 >= 90
        assertEquals("B", resp.getIndicators().get(1).getGrade()); // 88 >= 80
    }

    // KPI 摘要 — 無數據
    @Test
    void getSummary_noData_returnsEmpty() {
        when(resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(Collections.emptyList());

        KpiSummaryResponse resp = service.getSummary(2025, 3);

        assertNotNull(resp);
        assertTrue(resp.getIndicators().isEmpty());
    }

    // TC-10-011-01: KPI 趨勢
    @Test
    void getTrend_returnsMultiMonths() {
        when(resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 1))
                .thenReturn(List.of(
                        buildResult("KPI-001", "路燈可用率", new BigDecimal("92"), new BigDecimal("100"))
                ));
        when(resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 2))
                .thenReturn(List.of(
                        buildResult("KPI-001", "路燈可用率", new BigDecimal("94"), new BigDecimal("100"))
                ));
        when(resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(List.of(
                        buildResult("KPI-001", "路燈可用率", new BigDecimal("95"), new BigDecimal("100"))
                ));

        KpiTrendResponse resp = service.getTrend(2025, 3, 3);

        assertNotNull(resp);
        assertEquals(3, resp.getMonths().size());
        assertEquals("2025-01", resp.getMonths().get(0).getMonth());
        assertEquals("2025-03", resp.getMonths().get(2).getMonth());
        assertEquals(1, resp.getMonths().get(0).getIndicators().size());
    }

    // KPI 趨勢 — 跨年 (month=1, lookback=3 → 11月, 12月, 1月)
    @Test
    void getTrend_crossYear_handlesCorrectly() {
        when(resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth(eq("T1"), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        KpiTrendResponse resp = service.getTrend(2025, 1, 3);

        assertNotNull(resp);
        assertEquals(3, resp.getMonths().size());
        assertEquals("2024-11", resp.getMonths().get(0).getMonth());
        assertEquals("2025-01", resp.getMonths().get(2).getMonth());
    }
}
