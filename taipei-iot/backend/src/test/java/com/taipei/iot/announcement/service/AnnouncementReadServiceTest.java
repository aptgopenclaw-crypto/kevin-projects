package com.taipei.iot.announcement.service;

import com.taipei.iot.announcement.dto.UnreadCountResponse;
import com.taipei.iot.announcement.repository.AnnouncementReadRepository;
import com.taipei.iot.announcement.repository.AnnouncementRepository;
import com.taipei.iot.common.dto.UserInfo;
import com.taipei.iot.common.util.JwtClaimKeys;
import com.taipei.iot.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnouncementReadServiceTest {

    @InjectMocks private AnnouncementReadService announcementReadService;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementReadRepository announcementReadRepository;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("TENANT_A");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String userId, Long deptId) {
        Map<String, Object> details = new HashMap<>();
        details.put(JwtClaimKeys.TENANT_ID, "TENANT_A");
        details.put(JwtClaimKeys.DEPT_ID, deptId);
        details.put(JwtClaimKeys.DATA_SCOPE, "DEPT");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ─── getUnreadCount ─────────────────────────────────────────────────────

    @Test
    void getUnreadCount_returnsCountFromRepository() {
        setSecurityContext("user-1", 3L);
        when(announcementRepository.countUnread(eq(3L), eq("user-1"), any()))
                .thenReturn(5L);

        UnreadCountResponse resp = announcementReadService.getUnreadCount();

        assertEquals(5, resp.getCount());
    }

    @Test
    void getUnreadCount_nullDeptId_usesMinusOne() {
        setSecurityContext("user-2", null);
        // UserInfo.getDeptId() returns null → service uses -1L
        when(announcementRepository.countUnread(eq(-1L), eq("user-2"), any()))
                .thenReturn(0L);

        UnreadCountResponse resp = announcementReadService.getUnreadCount();

        assertEquals(0, resp.getCount());
        verify(announcementRepository).countUnread(eq(-1L), eq("user-2"), any());
    }

    // ─── markAsRead ─────────────────────────────────────────────────────────

    @Test
    void markAsRead_callsRepositoryUpsert() {
        setSecurityContext("user-1", 3L);

        announcementReadService.markAsRead(42L);

        verify(announcementReadRepository).markAsRead(42L, "user-1");
    }

    @Test
    void markAsRead_idempotent_noException() {
        setSecurityContext("user-1", 3L);
        doNothing().when(announcementReadRepository).markAsRead(anyLong(), anyString());

        // 呼叫兩次不應拋例外（ON CONFLICT DO NOTHING）
        announcementReadService.markAsRead(1L);
        announcementReadService.markAsRead(1L);

        verify(announcementReadRepository, times(2)).markAsRead(1L, "user-1");
    }

    // ─── markAllAsRead ──────────────────────────────────────────────────────

    @Test
    void markAllAsRead_callsRepositoryWithCorrectParams() {
        setSecurityContext("user-1", 3L);

        announcementReadService.markAllAsRead();

        verify(announcementReadRepository).markAllAsRead("user-1", "TENANT_A", 3L);
    }

    @Test
    void markAllAsRead_nullDeptId_usesMinusOne() {
        setSecurityContext("user-2", null);

        announcementReadService.markAllAsRead();

        verify(announcementReadRepository).markAllAsRead("user-2", "TENANT_A", -1L);
    }
}
