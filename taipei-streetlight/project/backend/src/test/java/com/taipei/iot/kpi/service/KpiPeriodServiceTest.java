package com.taipei.iot.kpi.service;

import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.kpi.dto.PeriodResponse;
import com.taipei.iot.kpi.entity.KpiPeriod;
import com.taipei.iot.kpi.repository.KpiPeriodRepository;
import com.taipei.iot.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiPeriodServiceTest {

    @InjectMocks private KpiPeriodService service;
    @Mock private KpiPeriodRepository repo;

    private MockedStatic<TenantContext> tenantMock;
    private MockedStatic<SecurityContextUtils> securityMock;

    @BeforeEach
    void setUp() {
        tenantMock = mockStatic(TenantContext.class);
        tenantMock.when(TenantContext::getCurrentTenantId).thenReturn("T1");
        securityMock = mockStatic(SecurityContextUtils.class);
        securityMock.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        tenantMock.close();
        securityMock.close();
    }

    // TC-08-019-01: 查詢期間狀態
    @Test
    void getPeriods_returnsList() {
        KpiPeriod p = KpiPeriod.builder()
                .id(1L).tenantId("T1").periodYear(2025).periodMonth(3).locked(false).build();
        when(repo.findByTenantIdOrderByPeriodYearDescPeriodMonthDesc("T1"))
                .thenReturn(List.of(p));

        List<PeriodResponse> result = service.getPeriods();
        assertEquals(1, result.size());
        assertFalse(result.get(0).getLocked());
    }

    // TC-08-017-01: 鎖定期間
    @Test
    void lock_success() {
        KpiPeriod p = KpiPeriod.builder()
                .id(1L).tenantId("T1").periodYear(2025).periodMonth(3).locked(false).build();
        when(repo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(Optional.of(p));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PeriodResponse result = service.lock(2025, 3);
        assertTrue(result.getLocked());
        assertNotNull(result.getLockedAt());
        assertEquals("admin", result.getLockedBy());
    }

    // 鎖定已鎖定的期間 → 錯誤
    @Test
    void lock_alreadyLocked_throwsException() {
        KpiPeriod p = KpiPeriod.builder()
                .id(1L).tenantId("T1").periodYear(2025).periodMonth(3).locked(true).build();
        when(repo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(Optional.of(p));

        assertThrows(BusinessException.class, () -> service.lock(2025, 3));
    }

    // TC-08-018-01: 解鎖期間
    @Test
    void unlock_success() {
        KpiPeriod p = KpiPeriod.builder()
                .id(1L).tenantId("T1").periodYear(2025).periodMonth(3).locked(true).build();
        when(repo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(Optional.of(p));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PeriodResponse result = service.unlock(2025, 3, "修正資料");
        assertFalse(result.getLocked());
        assertEquals("修正資料", result.getUnlockReason());
    }

    // 解鎖未鎖定 → 錯誤
    @Test
    void unlock_notLocked_throwsException() {
        when(repo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 3))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.unlock(2025, 3, "test"));
    }

    // 鎖定不存在的期間 → 自動建立
    @Test
    void lock_newPeriod_createsAndLocks() {
        when(repo.findByTenantIdAndPeriodYearAndPeriodMonth("T1", 2025, 6))
                .thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> {
            KpiPeriod saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        PeriodResponse result = service.lock(2025, 6);
        assertTrue(result.getLocked());
        verify(repo).save(any());
    }
}
