package com.taipei.iot.dashboard.service;

import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dashboard.dto.DefaultLayoutRequest;
import com.taipei.iot.dashboard.dto.DefaultLayoutResponse;
import com.taipei.iot.dashboard.dto.LayoutRequest;
import com.taipei.iot.dashboard.dto.LayoutResponse;
import com.taipei.iot.dashboard.entity.DashboardDefaultLayout;
import com.taipei.iot.dashboard.entity.DashboardLayout;
import com.taipei.iot.dashboard.repository.DashboardDefaultLayoutRepository;
import com.taipei.iot.dashboard.repository.DashboardLayoutRepository;
import com.taipei.iot.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardLayoutServiceTest {

    @InjectMocks private DashboardLayoutService service;
    @Mock private DashboardLayoutRepository layoutRepository;
    @Mock private DashboardDefaultLayoutRepository defaultLayoutRepository;

    private MockedStatic<TenantContext> tenantMock;
    private MockedStatic<SecurityContextUtils> securityMock;

    @BeforeEach
    void setUp() {
        tenantMock = mockStatic(TenantContext.class);
        tenantMock.when(TenantContext::getCurrentTenantId).thenReturn("T1");
        securityMock = mockStatic(SecurityContextUtils.class);
        securityMock.when(SecurityContextUtils::getCurrentUserId).thenReturn("user1");
    }

    @AfterEach
    void tearDown() {
        tenantMock.close();
        securityMock.close();
    }

    private DashboardLayout buildPersonalLayout() {
        return DashboardLayout.builder()
                .id(1L).tenantId("T1").userId("user1")
                .layoutJson("[{\"i\":\"w0\",\"x\":0,\"y\":0,\"w\":6,\"h\":4,\"type\":\"maintenance-stats\"}]")
                .isDefault(false)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private DashboardDefaultLayout buildDefaultLayout() {
        return DashboardDefaultLayout.builder()
                .id(10L).tenantId("T1").roleType(null)
                .layoutJson("[{\"i\":\"w0\",\"x\":0,\"y\":0,\"w\":6,\"h\":4,\"type\":\"lamp-count\"}]")
                .build();
    }

    // TC-10-001-01: 載入用戶版面 — 有個人版面
    @Test
    void getLayout_personalExists_returnsPersonal() {
        when(layoutRepository.findByTenantIdAndUserId("T1", "user1"))
                .thenReturn(Optional.of(buildPersonalLayout()));

        LayoutResponse resp = service.getLayout();

        assertNotNull(resp);
        assertEquals(1L, resp.getId());
        assertFalse(resp.getIsDefault());
        assertTrue(resp.getLayoutJson().contains("maintenance-stats"));
    }

    // TC-10-001-02: 首次使用 → 回傳預設版面
    @Test
    void getLayout_noPersonal_defaultExists_returnsDefault() {
        when(layoutRepository.findByTenantIdAndUserId("T1", "user1"))
                .thenReturn(Optional.empty());
        when(defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull("T1"))
                .thenReturn(Optional.of(buildDefaultLayout()));

        LayoutResponse resp = service.getLayout();

        assertTrue(resp.getIsDefault());
        assertEquals(10L, resp.getId());
        assertTrue(resp.getLayoutJson().contains("lamp-count"));
    }

    // 首次使用 → 無預設版面 → 回傳空配置
    @Test
    void getLayout_noPersonal_noDefault_returnsEmpty() {
        when(layoutRepository.findByTenantIdAndUserId("T1", "user1"))
                .thenReturn(Optional.empty());
        when(defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull("T1"))
                .thenReturn(Optional.empty());

        LayoutResponse resp = service.getLayout();

        assertTrue(resp.getIsDefault());
        assertEquals("[]", resp.getLayoutJson());
        assertNull(resp.getId());
    }

    // TC-10-002-01: 儲存版面 — 新建
    @Test
    void saveLayout_noExisting_createsNew() {
        when(layoutRepository.findByTenantIdAndUserId("T1", "user1"))
                .thenReturn(Optional.empty());
        when(layoutRepository.save(any())).thenAnswer(inv -> {
            DashboardLayout l = inv.getArgument(0);
            l.setId(2L);
            l.setUpdatedAt(LocalDateTime.now());
            return l;
        });

        LayoutRequest req = LayoutRequest.builder().layoutJson("[{\"test\":true}]").build();
        LayoutResponse resp = service.saveLayout(req);

        assertNotNull(resp);
        assertEquals(2L, resp.getId());
        assertFalse(resp.getIsDefault());
        assertEquals("[{\"test\":true}]", resp.getLayoutJson());
        verify(layoutRepository).save(any(DashboardLayout.class));
    }

    // TC-10-002-01 variant: 儲存版面 — 更新既有
    @Test
    void saveLayout_existing_updates() {
        DashboardLayout existing = buildPersonalLayout();
        when(layoutRepository.findByTenantIdAndUserId("T1", "user1"))
                .thenReturn(Optional.of(existing));
        when(layoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LayoutRequest req = LayoutRequest.builder().layoutJson("[{\"updated\":true}]").build();
        LayoutResponse resp = service.saveLayout(req);

        assertEquals(1L, resp.getId());
        assertFalse(resp.getIsDefault());
        assertEquals("[{\"updated\":true}]", resp.getLayoutJson());
    }

    // TC-10-003-01: 重置為預設 — 有預設版面
    @Test
    void resetLayout_defaultExists_returnsDefault() {
        when(defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull("T1"))
                .thenReturn(Optional.of(buildDefaultLayout()));

        LayoutResponse resp = service.resetLayout();

        verify(layoutRepository).deleteByTenantIdAndUserId("T1", "user1");
        assertTrue(resp.getIsDefault());
        assertTrue(resp.getLayoutJson().contains("lamp-count"));
    }

    // TC-10-003-01 variant: 重置 — 無預設版面
    @Test
    void resetLayout_noDefault_returnsEmpty() {
        when(defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull("T1"))
                .thenReturn(Optional.empty());

        LayoutResponse resp = service.resetLayout();

        verify(layoutRepository).deleteByTenantIdAndUserId("T1", "user1");
        assertTrue(resp.getIsDefault());
        assertEquals("[]", resp.getLayoutJson());
    }

    // ── Default Layout Management ──

    @Test
    void getDefaultLayout_exists_returnsIt() {
        when(defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull("T1"))
                .thenReturn(Optional.of(buildDefaultLayout()));

        DefaultLayoutResponse resp = service.getDefaultLayout();

        assertNotNull(resp);
        assertEquals(10L, resp.getId());
        assertNull(resp.getRoleType());
        assertTrue(resp.getLayoutJson().contains("lamp-count"));
    }

    @Test
    void getDefaultLayout_notExists_returnsNull() {
        when(defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull("T1"))
                .thenReturn(Optional.empty());

        DefaultLayoutResponse resp = service.getDefaultLayout();

        assertNull(resp);
    }

    @Test
    void saveDefaultLayout_newGlobal_createsNew() {
        when(defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull("T1"))
                .thenReturn(Optional.empty());
        when(defaultLayoutRepository.save(any())).thenAnswer(inv -> {
            DashboardDefaultLayout dl = inv.getArgument(0);
            dl.setId(20L);
            return dl;
        });

        DefaultLayoutRequest req = DefaultLayoutRequest.builder()
                .layoutJson("[{\"i\":\"w0\",\"x\":0,\"y\":0,\"w\":12,\"h\":6,\"type\":\"maintenance-stats\"}]")
                .roleType(null)
                .build();

        DefaultLayoutResponse resp = service.saveDefaultLayout(req);

        assertNotNull(resp);
        assertEquals(20L, resp.getId());
        assertNull(resp.getRoleType());
        assertTrue(resp.getLayoutJson().contains("maintenance-stats"));
        verify(defaultLayoutRepository).save(any(DashboardDefaultLayout.class));
    }

    @Test
    void saveDefaultLayout_existingGlobal_updates() {
        DashboardDefaultLayout existing = buildDefaultLayout();
        when(defaultLayoutRepository.findByTenantIdAndRoleTypeIsNull("T1"))
                .thenReturn(Optional.of(existing));
        when(defaultLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefaultLayoutRequest req = DefaultLayoutRequest.builder()
                .layoutJson("[{\"updated\":true}]")
                .roleType(null)
                .build();

        DefaultLayoutResponse resp = service.saveDefaultLayout(req);

        assertEquals(10L, resp.getId());
        assertEquals("[{\"updated\":true}]", resp.getLayoutJson());
    }
}
