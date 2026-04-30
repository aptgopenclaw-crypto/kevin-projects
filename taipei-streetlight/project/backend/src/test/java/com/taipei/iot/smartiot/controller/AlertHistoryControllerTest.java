package com.taipei.iot.smartiot.controller;

import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.smartiot.dto.AlertHistoryResponse;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.AlertStatus;
import com.taipei.iot.smartiot.service.AlertService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertHistoryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AlertHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AlertService alertService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private static final String BASE_URL = "/v1/auth/iot/alerts";

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

    private AlertHistoryResponse sampleResponse() {
        return AlertHistoryResponse.builder()
                .id(1L)
                .tenantId("TENANT_A")
                .ruleId(10L)
                .deviceId(100L)
                .severity(AlertSeverity.WARNING)
                .status(AlertStatus.OPEN)
                .message("規則 [Low RSSI Alert] 觸發：{rssi=-105}")
                .triggeredValues(Map.of("rssi", -105))
                .triggeredAt(LocalDateTime.of(2025, 1, 23, 10, 0, 0))
                .notificationSent(true)
                .build();
    }

    // ── GET /alerts — 列表 ──

    @Test
    void list_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        var page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1);
        when(alertService.list(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].id").value(1))
                .andExpect(jsonPath("$.body.content[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.body.content[0].status").value("OPEN"));
    }

    @Test
    void list_withStatusFilter_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        var page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1);
        when(alertService.list(eq(AlertStatus.OPEN), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .param("status", "OPEN")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].status").value("OPEN"));
    }

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /alerts/{id}/acknowledge ──

    @Test
    void acknowledge_withIotManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        AlertHistoryResponse acked = sampleResponse();
        acked.setStatus(AlertStatus.ACKNOWLEDGED);
        acked.setAckBy("user-001");
        acked.setAckAt(LocalDateTime.now());
        when(alertService.acknowledge(1L)).thenReturn(acked);

        mockMvc.perform(put(BASE_URL + "/1/acknowledge")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.body.ackBy").value("user-001"));
    }

    @Test
    void acknowledge_alertNotFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(alertService.acknowledge(999L)).thenThrow(new BusinessException(ErrorCode.IOT_ALERT_NOT_FOUND));

        mockMvc.perform(put(BASE_URL + "/999/acknowledge")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("88030"));
    }

    @Test
    void acknowledge_invalidStatus_returns400() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(alertService.acknowledge(1L)).thenThrow(new BusinessException(ErrorCode.IOT_ALERT_INVALID_STATUS));

        mockMvc.perform(put(BASE_URL + "/1/acknowledge")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("88031"));
    }

    @Test
    void acknowledge_withIotView_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));

        mockMvc.perform(put(BASE_URL + "/1/acknowledge")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── PUT /alerts/{id}/resolve ──

    @Test
    void resolve_withIotManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        AlertHistoryResponse resolved = sampleResponse();
        resolved.setStatus(AlertStatus.RESOLVED);
        resolved.setResolvedAt(LocalDateTime.now());
        resolved.setMttrMinutes(45);
        when(alertService.resolve(1L)).thenReturn(resolved);

        mockMvc.perform(put(BASE_URL + "/1/resolve")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.status").value("RESOLVED"))
                .andExpect(jsonPath("$.body.mttrMinutes").value(45));
    }

    @Test
    void resolve_alertNotFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(alertService.resolve(999L)).thenThrow(new BusinessException(ErrorCode.IOT_ALERT_NOT_FOUND));

        mockMvc.perform(put(BASE_URL + "/999/resolve")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("88030"));
    }

    @Test
    void resolve_invalidStatus_returns400() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(alertService.resolve(1L)).thenThrow(new BusinessException(ErrorCode.IOT_ALERT_INVALID_STATUS));

        mockMvc.perform(put(BASE_URL + "/1/resolve")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("88031"));
    }

    // ── GET /alerts/export ──

    @Test
    void export_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        var page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 1000), 1);
        when(alertService.list(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/export")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].id").value(1));
    }

    @Test
    void export_noAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/export"))
                .andExpect(status().isUnauthorized());
    }
}
