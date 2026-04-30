package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.MaintenanceStatsResponse;
import com.taipei.iot.dashboard.dto.MaintenanceTrendResponse;
import com.taipei.iot.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WidgetMaintenanceServiceTest {

    @InjectMocks private WidgetMaintenanceService service;
    @Mock private EntityManager em;

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

    private Query mockQuery(Object result) {
        Query q = mock(Query.class);
        lenient().when(q.setParameter(anyString(), any())).thenReturn(q);
        when(q.getSingleResult()).thenReturn(result);
        return q;
    }

    private Query mockQueryList(List<?> results) {
        Query q = mock(Query.class);
        lenient().when(q.setParameter(anyString(), any())).thenReturn(q);
        when(q.getResultList()).thenReturn(results);
        return q;
    }

    // TC-10-004-01: 養護統計數據
    @Test
    void getStats_returnsCorrectSummary() {
        // 主統計
        Query statsQ = mockQuery(new Object[]{100L, 80L, 20L});
        // 平均修復時數
        Query avgQ = mockQuery(24.5);
        // 來源分布
        Query sourceQ = mockQueryList(List.of(
                new Object[]{"APP", 60L},
                new Object[]{"PHONE", 40L}
        ));
        // 故障分類分布
        Query faultQ = mockQueryList(List.of(
                new Object[]{"燈不亮", 50L},
                new Object[]{"燈閃爍", 30L}
        ));
        // 照明妥善率
        Query illumQ = mockQuery(new Object[]{9500L, 10000L});

        when(em.createNativeQuery(anyString()))
                .thenReturn(statsQ, avgQ, sourceQ, faultQ, illumQ);

        MaintenanceStatsResponse resp = service.getStats(null, null, null);

        assertNotNull(resp);
        assertEquals(100L, resp.getTotalRepairs());
        assertEquals(80L, resp.getCompletedRepairs());
        assertEquals(20L, resp.getPendingRepairs());
        assertEquals(new BigDecimal("80.00"), resp.getCompletionRate());
        assertFalse(resp.getSourceDistribution().isEmpty());
        assertFalse(resp.getFaultCategoryDistribution().isEmpty());
    }

    // TC-10-005-01: 養護趨勢圖
    @Test
    void getTrend_returnsMonthlyPoints() {
        Query q = mockQueryList(List.of(
                new Object[]{"2025-01", 30L, 25L},
                new Object[]{"2025-02", 40L, 35L},
                new Object[]{"2025-03", 35L, 30L}
        ));
        when(em.createNativeQuery(anyString())).thenReturn(q);

        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 3, 31);
        MaintenanceTrendResponse resp = service.getTrend(start, end, null);

        assertNotNull(resp);
        assertEquals(3, resp.getMonths().size());
        assertEquals("2025-01", resp.getMonths().get(0).getMonth());
        assertTrue(resp.getMonths().get(0).getCompletionRate().compareTo(BigDecimal.ZERO) > 0);
    }

    // 帶有合約篩選
    @Test
    void getStats_withContract_callsWithParameter() {
        Query statsQ = mockQuery(new Object[]{50L, 40L, 10L});
        Query avgQ = mockQuery(12.0);
        List<Object[]> sourceList = new ArrayList<>();
        sourceList.add(new Object[]{"APP", 50L});
        Query sourceQ = mockQueryList(sourceList);
        List<Object[]> faultList = new ArrayList<>();
        faultList.add(new Object[]{"燈不亮", 30L});
        Query faultQ = mockQueryList(faultList);
        Query illumQ = mockQuery(new Object[]{4800L, 5000L});

        when(em.createNativeQuery(anyString()))
                .thenReturn(statsQ, avgQ, sourceQ, faultQ, illumQ);

        MaintenanceStatsResponse resp = service.getStats(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31), 1L);

        assertNotNull(resp);
        assertEquals(50L, resp.getTotalRepairs());
    }
}
