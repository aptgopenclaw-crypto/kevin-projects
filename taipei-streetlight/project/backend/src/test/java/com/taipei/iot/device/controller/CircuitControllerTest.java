package com.taipei.iot.device.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.device.dto.CircuitRequest;
import com.taipei.iot.device.dto.CircuitResponse;
import com.taipei.iot.device.service.CircuitService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.taipei.iot.common.interceptor.RateLimitInterceptor;
import com.taipei.iot.tenant.TenantInterceptor;
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

@WebMvcTest(CircuitController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CircuitControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CircuitService circuitService;
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
    void list_withCircuitView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("CIRCUIT_VIEW"));
        when(circuitService.list(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        CircuitResponse.builder().id(1L).circuitNumber("CKT-001").build())));

        mockMvc.perform(get("/v1/auth/circuits")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.content[0].circuitNumber").value("CKT-001"));
    }

    @Test
    void list_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/circuits"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("CIRCUIT_VIEW"));
        when(circuitService.getById(1L))
                .thenReturn(CircuitResponse.builder().id(1L).circuitNumber("CKT-001").build());

        mockMvc.perform(get("/v1/auth/circuits/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.circuitNumber").value("CKT-001"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("CIRCUIT_VIEW"));
        when(circuitService.getById(99L))
                .thenThrow(new BusinessException(ErrorCode.CIRCUIT_NOT_FOUND));

        mockMvc.perform(get("/v1/auth/circuits/99")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withCircuitManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("CIRCUIT_MANAGE"));
        CircuitRequest req = CircuitRequest.builder().circuitNumber("CKT-NEW").build();
        when(circuitService.create(any()))
                .thenReturn(CircuitResponse.builder().id(2L).circuitNumber("CKT-NEW").build());

        mockMvc.perform(post("/v1/auth/circuits")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.circuitNumber").value("CKT-NEW"));
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("CIRCUIT_VIEW"));
        CircuitRequest req = CircuitRequest.builder().circuitNumber("CKT-NEW").build();

        mockMvc.perform(post("/v1/auth/circuits")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withCircuitManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("CIRCUIT_MANAGE"));

        mockMvc.perform(delete("/v1/auth/circuits/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(circuitService).delete(1L);
    }

    @Test
    void delete_hasDevices_returnsError() throws Exception {
        mockJwtValid(validToken(), List.of("CIRCUIT_MANAGE"));
        doThrow(new BusinessException(ErrorCode.CIRCUIT_HAS_DEVICES))
                .when(circuitService).delete(1L);

        mockMvc.perform(delete("/v1/auth/circuits/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().is4xxClientError());
    }
}
