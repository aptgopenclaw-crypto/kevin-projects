package com.taipei.iot.replacement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.replacement.dto.PoleNumberRequest;
import com.taipei.iot.replacement.dto.PoleNumberResponse;
import com.taipei.iot.replacement.enums.PoleNumberStatus;
import com.taipei.iot.replacement.service.LightPoleNumberService;
import com.taipei.iot.tenant.TenantInterceptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LightPoleNumberController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class LightPoleNumberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private LightPoleNumberService poleNumberService;
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
    void list_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("POLE_NUMBER_MANAGE"));
        when(poleNumberService.list(any(), any())).thenReturn(
                new PageImpl<>(List.of(PoleNumberResponse.builder()
                        .id(1L).poleNumber("PN-001").status(PoleNumberStatus.ACTIVE).build())));

        mockMvc.perform(get("/v1/auth/replacement/pole-numbers")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].poleNumber").value("PN-001"));
    }

    @Test
    void generate_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("POLE_NUMBER_MANAGE"));
        when(poleNumberService.generate(any())).thenReturn(
                PoleNumberResponse.builder().id(1L).poleNumber("PN-002")
                        .status(PoleNumberStatus.ACTIVE).build());

        PoleNumberRequest request = PoleNumberRequest.builder()
                .poleNumber("PN-002").deviceId(100L).build();

        mockMvc.perform(post("/v1/auth/replacement/pole-numbers")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.poleNumber").value("PN-002"));
    }

    @Test
    void list_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());
        mockMvc.perform(get("/v1/auth/replacement/pole-numbers")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }
}
