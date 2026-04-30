package com.taipei.iot.kpi.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.kpi.dto.KpiIndicatorRequest;
import com.taipei.iot.kpi.dto.KpiIndicatorResponse;
import com.taipei.iot.kpi.entity.KpiIndicator;
import com.taipei.iot.kpi.enums.FormulaType;
import com.taipei.iot.kpi.enums.KpiCategory;
import com.taipei.iot.kpi.enums.KpiDataSource;
import com.taipei.iot.kpi.enums.KpiIndicatorStatus;
import com.taipei.iot.kpi.engine.FormulaEngine;
import com.taipei.iot.kpi.repository.KpiIndicatorRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiIndicatorServiceTest {

    @InjectMocks private KpiIndicatorService service;
    @Mock private KpiIndicatorRepository repo;
    @Mock private KpiRawDataRepository rawDataRepo;
    @Mock private KpiResultRepository resultRepo;
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

    private KpiIndicator buildIndicator(Long id, String code) {
        return KpiIndicator.builder()
                .id(id)
                .tenantId("T1")
                .indicatorCode(code)
                .indicatorName("Test Indicator " + code)
                .category(KpiCategory.MAINTENANCE)
                .formulaType(FormulaType.SPEL)
                .formula("#value / #target * 100")
                .targetValue(new BigDecimal("100"))
                .weight(new BigDecimal("0.3"))
                .dataSource(KpiDataSource.REPAIR)
                .unit("%")
                .status(KpiIndicatorStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // TC-08-001-01: 查詢 KPI 指標
    @Test
    void list_success() {
        Page<KpiIndicator> page = new PageImpl<>(List.of(buildIndicator(1L, "KPI-001")));
        when(repo.findByFilters(any(), any(), any(), any())).thenReturn(page);

        Page<KpiIndicatorResponse> result = service.list(null, null, null, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
        assertEquals("KPI-001", result.getContent().get(0).getIndicatorCode());
    }

    // TC-08-002-01: 新增指標
    @Test
    void create_success() {
        when(repo.save(any())).thenAnswer(inv -> {
            KpiIndicator e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(LocalDateTime.now());
            e.setUpdatedAt(LocalDateTime.now());
            return e;
        });
        doNothing().when(formulaEngine).validate(any(), any());

        KpiIndicatorRequest req = KpiIndicatorRequest.builder()
                .indicatorCode("KPI-NEW")
                .indicatorName("New Indicator")
                .category("MAINTENANCE")
                .formulaType("SPEL")
                .formula("#value / #target * 100")
                .dataSource("REPAIR")
                .build();

        KpiIndicatorResponse resp = service.create(req);
        assertNotNull(resp.getId());
        assertEquals("KPI-NEW", resp.getIndicatorCode());
        verify(repo).save(any());
    }

    // TC-08-003-01: 更新公式
    @Test
    void update_success() {
        KpiIndicator existing = buildIndicator(1L, "KPI-001");
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenReturn(existing);
        doNothing().when(formulaEngine).validate(any(), any());

        KpiIndicatorRequest req = KpiIndicatorRequest.builder()
                .indicatorCode("KPI-001")
                .indicatorName("Updated Name")
                .category("QUALITY")
                .formulaType("SPEL")
                .formula("#value * 2")
                .dataSource("REPAIR")
                .build();

        KpiIndicatorResponse resp = service.update(1L, req);
        assertEquals("Updated Name", resp.getIndicatorName());
    }

    // TC-08-004-01: 刪除指標 (soft delete — has data)
    @Test
    void delete_success() {
        KpiIndicator existing = buildIndicator(1L, "KPI-001");
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenReturn(existing);
        // Has raw data → soft delete (INACTIVE)
        when(rawDataRepo.findByIndicatorIdAndPeriodYearAndPeriodMonth(eq(1L), isNull(), isNull()))
                .thenReturn(List.of());
        when(resultRepo.findByFilters(isNull(), isNull(), isNull(), eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(new com.taipei.iot.kpi.entity.KpiResult())));

        service.delete(1L);
        assertEquals(KpiIndicatorStatus.INACTIVE, existing.getStatus());
        verify(repo).save(existing);
    }

    // Not found
    @Test
    void update_notFound_throwsException() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        KpiIndicatorRequest req = KpiIndicatorRequest.builder()
                .indicatorCode("X").indicatorName("X")
                .category("MAINTENANCE")
                .formulaType("SPEL").formula("#x")
                .dataSource("REPAIR").build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(999L, req));
        assertEquals(ErrorCode.KPI_INDICATOR_NOT_FOUND, ex.getErrorCode());
    }
}
