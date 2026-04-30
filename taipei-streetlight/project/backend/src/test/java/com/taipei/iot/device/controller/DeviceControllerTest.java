package com.taipei.iot.device.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.device.dto.DeviceRequest;
import com.taipei.iot.device.dto.DeviceResponse;
import com.taipei.iot.device.entity.DeviceEvent;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.device.service.DeviceExportService;
import com.taipei.iot.device.service.DeviceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

@WebMvcTest(DeviceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DeviceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private DeviceService deviceService;
    @MockitoBean private DeviceEventService deviceEventService;
    @MockitoBean private DeviceExportService deviceExportService;
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

    // ── GET / (list) ──

    @Test
    void list_withDeviceViewPermission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));
        DeviceResponse resp = DeviceResponse.builder().id(1L).deviceCode("SL-001")
                .deviceType(DeviceType.POLE).status(DeviceStatus.ACTIVE).build();
        when(deviceService.listDevices(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/v1/auth/devices")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.content[0].deviceCode").value("SL-001"));
    }

    @Test
    void list_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/devices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());
        mockMvc.perform(get("/v1/auth/devices")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── GET /{id} ──

    @Test
    void getById_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));
        when(deviceService.getByIdWithComponents(1L))
                .thenReturn(DeviceResponse.builder().id(1L).deviceCode("SL-001").build());

        mockMvc.perform(get("/v1/auth/devices/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.deviceCode").value("SL-001"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));
        when(deviceService.getByIdWithComponents(99L))
                .thenThrow(new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        mockMvc.perform(get("/v1/auth/devices/99")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }

    // ── POST / (create) ──

    @Test
    void create_withDeviceManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_MANAGE"));
        DeviceRequest req = DeviceRequest.builder()
                .deviceType(DeviceType.POLE).deviceCode("SL-NEW").build();
        when(deviceService.create(any())).thenReturn(
                DeviceResponse.builder().id(2L).deviceCode("SL-NEW").build());

        mockMvc.perform(post("/v1/auth/devices")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.deviceCode").value("SL-NEW"));
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));
        DeviceRequest req = DeviceRequest.builder()
                .deviceType(DeviceType.POLE).deviceCode("SL-NEW").build();

        mockMvc.perform(post("/v1/auth/devices")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /{id} ──

    @Test
    void delete_withDeviceManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_MANAGE"));

        mockMvc.perform(delete("/v1/auth/devices/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(deviceService).delete(1L);
    }

    // ── POST /{id}/decommission ──

    @Test
    void decommission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_MANAGE"));

        mockMvc.perform(post("/v1/auth/devices/1/decommission")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());

        verify(deviceService).decommission(1L);
    }

    // ── GET /{id}/components ──

    @Test
    void getComponents_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));
        when(deviceService.getActiveComponents(1L)).thenReturn(List.of(
                DeviceResponse.builder().id(2L).deviceCode("LM-001").build()));

        mockMvc.perform(get("/v1/auth/devices/1/components")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body[0].deviceCode").value("LM-001"));
    }

    // ── GET /export ──

    @Test
    void export_withDeviceExport_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_EXPORT"));
        when(deviceExportService.queryForExport(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/v1/auth/devices/export")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());
    }

    @Test
    void export_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW"));

        mockMvc.perform(get("/v1/auth/devices/export")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }
}
