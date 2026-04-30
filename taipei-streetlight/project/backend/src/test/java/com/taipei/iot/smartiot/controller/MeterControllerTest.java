package com.taipei.iot.smartiot.controller;

import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.smartiot.dto.MeterStatusResponse;
import com.taipei.iot.smartiot.service.MeterService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeterController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MeterControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MeterService meterService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private static final String URL = "/v1/auth/iot/meters/status";

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
    void getStatus_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(meterService.getStatus()).thenReturn(
                MeterStatusResponse.builder()
                        .totalMeters(10).onlineMeters(8)
                        .offlineMeters(2).anomalyMeters(1).build());

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.totalMeters").value(10))
                .andExpect(jsonPath("$.body.onlineMeters").value(8));
    }

    @Test
    void getStatus_noAuth_returns401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatus_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }
}
