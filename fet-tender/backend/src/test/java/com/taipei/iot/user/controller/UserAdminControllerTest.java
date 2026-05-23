package com.taipei.iot.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtAuthenticationFilter;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.common.dto.PageResponse;
import com.taipei.iot.user.dto.response.UserListItemDto;
import com.taipei.iot.user.service.UserAdminService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.taipei.iot.common.interceptor.RateLimitInterceptor;
import com.taipei.iot.tenant.TenantInterceptor;
import com.taipei.iot.tenant.TenantEnabledCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAdminController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserAdminService userAdminService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;
    @MockitoBean private TenantEnabledCache tenantEnabledCache;

    private String validToken() {
        return "valid.jwt.token";
    }

    private void mockJwtValid(String token, String userId, String tenantId, List<String> roles) {
        mockJwtValid(token, userId, tenantId, roles, List.of());
    }

    private void mockJwtValid(String token, String userId, String tenantId,
                               List<String> roles, List<String> permissions) {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("uid", userId);
        claimsMap.put("tenantId", tenantId);
        claimsMap.put("roles", roles);
        claimsMap.put("permissions", permissions);
        claimsMap.put("sub", "test@test.com");
        claimsMap.put("exp", new Date(System.currentTimeMillis() + 3600000));
        claimsMap.put("iat", new Date());
        Claims claims = new DefaultClaims(claimsMap);
        when(jwtUtil.parseToken(token)).thenReturn(claims);
    }

    @Test
    void listUsers_adminRole_shouldReturn200() throws Exception {
        mockJwtValid(validToken(), "user-admin-001", "TENANT_A", List.of("ADMIN"),
                List.of("USER_LIST", "USER_CREATE", "USER_UPDATE", "USER_DISABLE", "USER_DELETE"));

        PageResponse<UserListItemDto> pageResponse = PageResponse.<UserListItemDto>builder()
                .content(List.of())
                .totalElements(0)
                .totalPages(0)
                .page(0)
                .size(20)
                .build();
        when(userAdminService.listUsers(0, 20, null)).thenReturn(pageResponse);

        mockMvc.perform(get("/v1/auth/users")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));
    }

    @Test
    void listUsers_viewerRole_shouldReturn403() throws Exception {
        mockJwtValid(validToken(), "user-viewer-001", "TENANT_A", List.of("VIEWER"));

        mockMvc.perform(get("/v1/auth/users")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/v1/auth/users"))
                .andExpect(status().isUnauthorized());
    }
}
