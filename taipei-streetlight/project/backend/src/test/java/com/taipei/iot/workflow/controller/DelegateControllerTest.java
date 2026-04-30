package com.taipei.iot.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.workflow.dto.DelegateCandidateDto;
import com.taipei.iot.workflow.dto.DelegateSettingRequest;
import com.taipei.iot.workflow.dto.DelegateSettingResponse;
import com.taipei.iot.workflow.service.DelegateService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.taipei.iot.common.interceptor.RateLimitInterceptor;
import com.taipei.iot.tenant.TenantInterceptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DelegateController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DelegateControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private DelegateService delegateService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private String validToken() { return "valid.jwt.token"; }

    private void mockJwtValid(String token, List<String> permissions) {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("uid", "user-001");
        claimsMap.put("tenantId", "TENANT_A");
        claimsMap.put("roles", List.of("ADMIN"));
        claimsMap.put("permissions", permissions);
        claimsMap.put("deptId", "6");
        claimsMap.put("dataScope", "ALL");
        claimsMap.put("sub", "test@test.com");
        claimsMap.put("exp", new Date(System.currentTimeMillis() + 3600000));
        claimsMap.put("iat", new Date());
        Claims claims = new DefaultClaims(claimsMap);
        when(jwtUtil.parseToken(token)).thenReturn(claims);
    }

    @Test
    void list_withDelegateManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DELEGATE_MANAGE"));
        DelegateSettingResponse resp = DelegateSettingResponse.builder()
                .id(1L).delegatorId("user-001").delegatorName("測試管理員")
                .delegateId("user-002").delegateName("王小明")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(7))
                .isActive(true).build();
        when(delegateService.getMyDelegates()).thenReturn(List.of(resp));

        mockMvc.perform(get("/v1/auth/delegates")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body[0].delegateName").value("王小明"));
    }

    @Test
    void list_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/delegates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());
        mockMvc.perform(get("/v1/auth/delegates")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withDelegateManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DELEGATE_MANAGE"));
        DelegateSettingRequest req = DelegateSettingRequest.builder()
                .delegateId("user-002")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7)).build();
        when(delegateService.create(any()))
                .thenReturn(DelegateSettingResponse.builder()
                        .id(1L).delegateId("user-002").isActive(true).build());

        mockMvc.perform(post("/v1/auth/delegates")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.delegateId").value("user-002"));
    }

    @Test
    void deactivate_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DELEGATE_MANAGE"));

        mockMvc.perform(delete("/v1/auth/delegates/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(delegateService).deactivate(1L);
    }

    @Test
    void deactivate_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());

        mockMvc.perform(delete("/v1/auth/delegates/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidates_withDelegateManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DELEGATE_MANAGE"));
        when(delegateService.getCandidates()).thenReturn(List.of(
                DelegateCandidateDto.builder().userId("user-002").displayName("王小明").deptName("維修部").build()
        ));

        mockMvc.perform(get("/v1/auth/delegates/candidates")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body[0].userId").value("user-002"))
                .andExpect(jsonPath("$.body[0].displayName").value("王小明"));
    }

    @Test
    void candidates_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());
        mockMvc.perform(get("/v1/auth/delegates/candidates")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }
}
