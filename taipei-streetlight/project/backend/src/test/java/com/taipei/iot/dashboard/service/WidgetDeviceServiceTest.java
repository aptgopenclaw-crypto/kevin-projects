package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.LampCountResponse;
import com.taipei.iot.dashboard.dto.LampStatusResponse;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WidgetDeviceServiceTest {

    @InjectMocks private WidgetDeviceService service;
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

    private Query mockQuerySingle(Object result) {
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

    // TC-10-012-01: 路燈數量統計
    @Test
    void getLampCount_returnsGroupedCounts() {
        // total
        Query totalQ = mockQuerySingle(16000L);
        // byContractor
        Query contractorQ = mockQueryList(List.of(
                new Object[]{"A公司", 8000L},
                new Object[]{"B公司", 5000L},
                new Object[]{"未指定", 3000L}
        ));
        // byType
        Query typeQ = mockQueryList(List.of(
                new Object[]{"LUMINAIRE", 14000L},
                new Object[]{"POLE", 2000L}
        ));
        // byLightSource
        Query lightQ = mockQueryList(List.of(
                new Object[]{"LED", 12000L},
                new Object[]{"鈉燈", 2000L}
        ));
        // byFacilityType
        Query facilityQ = mockQueryList(List.of(
                new Object[]{"路燈", 10000L},
                new Object[]{"園燈", 4000L}
        ));

        when(em.createNativeQuery(anyString()))
                .thenReturn(totalQ, contractorQ, typeQ, lightQ, facilityQ);

        LampCountResponse resp = service.getLampCount(null, null);

        assertNotNull(resp);
        assertEquals(16000L, resp.getTotal());
        assertEquals(3, resp.getByContractor().size());
        assertEquals(2, resp.getByType().size());
        assertEquals(2, resp.getByLightSource().size());
        assertEquals(2, resp.getByFacilityType().size());
    }

    // TC-10-013-01: 在線/離線統計
    @Test
    void getLampStatus_returnsOnlineOffline() {
        Query q = mockQuerySingle(new Object[]{15000L, 1000L, 16000L});
        when(em.createNativeQuery(anyString())).thenReturn(q);

        LampStatusResponse resp = service.getLampStatus();

        assertNotNull(resp);
        assertEquals(15000L, resp.getOnline());
        assertEquals(1000L, resp.getOffline());
        assertEquals(new BigDecimal("93.75"), resp.getOnlineRate());
        assertNotNull(resp.getUpdatedAt());
    }

    // 路燈狀態 — 零設備
    @Test
    void getLampStatus_noDevices_returnsZeroRate() {
        Query q = mockQuerySingle(new Object[]{0L, 0L, 0L});
        when(em.createNativeQuery(anyString())).thenReturn(q);

        LampStatusResponse resp = service.getLampStatus();

        assertEquals(0L, resp.getOnline());
        assertEquals(BigDecimal.ZERO, resp.getOnlineRate());
    }
}
