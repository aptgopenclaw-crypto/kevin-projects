package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.OutageAlertResponse;
import com.taipei.iot.dashboard.dto.OutageTrendResponse;
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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WidgetOutageServiceTest {

    @InjectMocks private WidgetOutageService service;
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

    private Query mockQueryList(List<?> results) {
        Query q = mock(Query.class);
        lenient().when(q.setParameter(anyString(), any())).thenReturn(q);
        when(q.getResultList()).thenReturn(results);
        return q;
    }

    // TC-10-006-01: 停電統計
    @Test
    void getCurrentOutages_returnsZones() {
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2025, 3, 15, 10, 30));
        Query q = mockQueryList(List.of(
                new Object[]{"POWER_OUTAGE", 12, ts},
                new Object[]{"POWER_OUTAGE", 5, ts}
        ));
        when(em.createNativeQuery(anyString())).thenReturn(q);

        OutageAlertResponse resp = service.getCurrentOutages();

        assertNotNull(resp);
        assertEquals(2, resp.getCurrentOutageCount());
        assertEquals(2, resp.getOutageZones().size());
        assertEquals(12, resp.getOutageZones().get(0).getAffectedCount());
    }

    // 停電統計 — 無停電
    @Test
    void getCurrentOutages_noOutages_returnsEmpty() {
        Query q = mockQueryList(Collections.emptyList());
        when(em.createNativeQuery(anyString())).thenReturn(q);

        OutageAlertResponse resp = service.getCurrentOutages();

        assertEquals(0, resp.getCurrentOutageCount());
        assertTrue(resp.getOutageZones().isEmpty());
    }

    // TC-10-007-01: 停電趨勢
    @Test
    void getOutageTrend_returnsMonthlyData() {
        Query q = mockQueryList(List.of(
                new Object[]{"2025-01", 3L, 4.5},
                new Object[]{"2025-02", 1L, 2.0}
        ));
        when(em.createNativeQuery(anyString())).thenReturn(q);

        OutageTrendResponse resp = service.getOutageTrend(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 28));

        assertNotNull(resp);
        assertEquals(2, resp.getMonths().size());
        assertEquals(3, resp.getMonths().get(0).getOutageCount());
        assertEquals("2025-01", resp.getMonths().get(0).getMonth());
    }
}
