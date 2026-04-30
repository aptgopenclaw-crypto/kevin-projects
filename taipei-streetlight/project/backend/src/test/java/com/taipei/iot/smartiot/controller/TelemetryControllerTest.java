package com.taipei.iot.smartiot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.smartiot.dto.TelemetryResponse;
import com.taipei.iot.smartiot.enums.QualityFlag;
import com.taipei.iot.smartiot.service.TelemetryService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TelemetryController 測試 — JWT auth endpoints (/v1/auth/iot/**)。
 * IoT device-token endpoints (/v1/iot/**) 走 IoTSecurityConfig，需整合測試。
 */
@WebMvcTest(TelemetryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TelemetryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TelemetryService telemetryService;
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

    private TelemetryResponse sampleResponse() {
        return TelemetryResponse.builder()
                .id(1L)
                .time(LocalDateTime.of(2026, 4, 26, 10, 30, 0))
                .deviceId(100L)
                .formatId(1L)
                .payload(Map.of("rssi", -85, "voltage", 220))
                .qualityFlag(QualityFlag.OK)
                .build();
    }

    // ── GET /v1/auth/iot/devices/{id}/telemetry/latest ──

    @Test
    void getLatest_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(telemetryService.getLatest(100L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/v1/auth/iot/devices/100/telemetry/latest")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.deviceId").value(100))
                .andExpect(jsonPath("$.body.qualityFlag").value("OK"))
                .andExpect(jsonPath("$.body.payload.rssi").value(-85));
    }

    @Test
    void getLatest_noData_returns200WithNullBody() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(telemetryService.getLatest(999L)).thenReturn(null);

        mockMvc.perform(get("/v1/auth/iot/devices/999/telemetry/latest")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").doesNotExist());
    }

    @Test
    void getLatest_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/iot/devices/100/telemetry/latest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getLatest_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));

        mockMvc.perform(get("/v1/auth/iot/devices/100/telemetry/latest")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── GET /v1/auth/iot/devices/{id}/telemetry/history ──

    @Test
    void getHistory_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        var page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 50), 1);
        when(telemetryService.getHistory(eq(100L), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/auth/iot/devices/100/telemetry/history")
                        .header("Authorization", "Bearer " + validToken())
                        .param("from", "2026-04-01T00:00:00")
                        .param("to", "2026-04-26T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].deviceId").value(100))
                .andExpect(jsonPath("$.body.totalElements").value(1));
    }

    @Test
    void getHistory_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/iot/devices/100/telemetry/history")
                        .param("from", "2026-04-01T00:00:00")
                        .param("to", "2026-04-26T23:59:59"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getHistory_missingParams_returns400() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));

        mockMvc.perform(get("/v1/auth/iot/devices/100/telemetry/history")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));

        mockMvc.perform(get("/v1/auth/iot/devices/100/telemetry/history")
                        .header("Authorization", "Bearer " + validToken())
                        .param("from", "2026-04-01T00:00:00")
                        .param("to", "2026-04-26T23:59:59"))
                .andExpect(status().isForbidden());
    }
}
