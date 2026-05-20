package com.taipei.iot.common.util;

import com.taipei.iot.common.dto.UserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── getCurrentUserId ─────────────────────────────────────────────────────

    @Test
    void getCurrentUserId_whenAuthenticated_returnsUserId() {
        setAuthentication("user-001", null);

        assertEquals("user-001", SecurityContextUtils.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_whenUnauthenticated_returnsNull() {
        SecurityContextHolder.clearContext();

        assertNull(SecurityContextUtils.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_whenAnonymous_returnsNull() {
        setAnonymousAuthentication();

        assertNull(SecurityContextUtils.getCurrentUserId(),
                "anonymousUser 不應被視為有效的已登入使用者");
    }

    // ─── getCurrentUsername ───────────────────────────────────────────────────

    @Test
    void getCurrentUsername_whenAuthenticated_returnsUsername() {
        setAuthentication("user-001", null);

        // UsernamePasswordAuthenticationToken.getName() 回傳 principal.toString() 即 userId
        assertEquals("user-001", SecurityContextUtils.getCurrentUsername());
    }

    @Test
    void getCurrentUsername_whenAnonymous_returnsNull() {
        setAnonymousAuthentication();

        assertNull(SecurityContextUtils.getCurrentUsername());
    }

    // ─── getUserInfo ──────────────────────────────────────────────────────────

    @Test
    void getUserInfo_withFullDetails_returnsCompleteUserInfo() {
        Map<String, Object> details = new HashMap<>();
        details.put(JwtClaimKeys.TENANT_ID, "tenant-99");
        details.put(JwtClaimKeys.DEPT_ID, 5L);
        details.put(JwtClaimKeys.DATA_SCOPE, "DEPT");
        setAuthentication("user-001", details);

        UserInfo info = SecurityContextUtils.getUserInfo();

        assertNotNull(info);
        assertEquals("user-001", info.getUserId());
        assertEquals("user-001", info.getUsername()); // getName() 回傳 principal，與 userId 相同
        assertEquals("tenant-99", info.getTenantId());
        assertEquals(5L, info.getDeptId());
        assertEquals("DEPT", info.getDataScope());
    }

    @Test
    void getUserInfo_deptIdAsString_parsedToLong() {
        Map<String, Object> details = new HashMap<>();
        details.put(JwtClaimKeys.DEPT_ID, "12");
        setAuthentication("user-002", details);

        UserInfo info = SecurityContextUtils.getUserInfo();

        assertNotNull(info);
        assertEquals(12L, info.getDeptId());
    }

    @Test
    void getUserInfo_deptIdInvalidString_deptIdIsNull() {
        Map<String, Object> details = new HashMap<>();
        details.put(JwtClaimKeys.DEPT_ID, "not-a-number");
        setAuthentication("user-003", details);

        UserInfo info = SecurityContextUtils.getUserInfo();

        assertNotNull(info);
        assertNull(info.getDeptId(), "無法解析的 deptId 應回傳 null 而非拋出例外");
    }

    @Test
    void getUserInfo_withoutMapDetails_returnsBasicInfo() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user-004", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setDetails("plain-string-detail");
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserInfo info = SecurityContextUtils.getUserInfo();

        assertNotNull(info);
        assertEquals("user-004", info.getUserId());
        assertEquals("user-004", info.getUsername());
        assertNull(info.getTenantId());
        assertNull(info.getDeptId());
    }

    @Test
    void getUserInfo_whenAnonymous_returnsNull() {
        setAnonymousAuthentication();

        assertNull(SecurityContextUtils.getUserInfo());
    }

    @Test
    void getUserInfo_whenUnauthenticated_returnsNull() {
        SecurityContextHolder.clearContext();

        assertNull(SecurityContextUtils.getUserInfo());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void setAuthentication(String userId, Map<String, Object> details) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        if (details != null) {
            auth.setDetails(details);
        }
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setAnonymousAuthentication() {
        AnonymousAuthenticationToken anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);
    }
}
