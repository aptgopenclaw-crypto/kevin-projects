package com.taipei.iot.smartiot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.smartiot.dto.TelemetryFormatFieldResponse;
import com.taipei.iot.smartiot.dto.TelemetryFormatRequest;
import com.taipei.iot.smartiot.dto.TelemetryFormatResponse;
import com.taipei.iot.smartiot.service.TelemetryFormatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryFormatController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TelemetryFormatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TelemetryFormatService formatService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private static final String BASE_URL = "/v1/auth/iot/telemetry-formats";

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

    private TelemetryFormatResponse sampleResponse() {
        return TelemetryFormatResponse.builder()
                .id(1L)
                .tenantId("TENANT_A")
                .vendorName("Philips")
                .deviceModel("CityTouch-100")
                .version(1)
                .fieldDefinitions(List.of(Map.of("name", "rssi", "type", "NUMBER", "unit", "dBm")))
                .enabled(true)
                .build();
    }

    // ── POST / (create) ──

    @Test
    void create_withIotManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(formatService.create(any())).thenReturn(sampleResponse());

        TelemetryFormatRequest req = TelemetryFormatRequest.builder()
                .vendorName("Philips")
                .deviceModel("CityTouch-100")
                .fieldDefinitions(List.of(Map.of("name", "rssi", "type", "NUMBER", "unit", "dBm")))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.vendorName").value("Philips"))
                .andExpect(jsonPath("$.body.deviceModel").value("CityTouch-100"));
    }

    @Test
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));

        TelemetryFormatRequest req = TelemetryFormatRequest.builder()
                .vendorName("Philips").deviceModel("X").build();

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── GET / (list) ──

    @Test
    void list_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        var page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1);
        when(formatService.list(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].vendorName").value("Philips"))
                .andExpect(jsonPath("$.body.totalElements").value(1));
    }

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /{id} (update) ──

    @Test
    void update_withIotManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(formatService.update(eq(1L), any())).thenReturn(sampleResponse());

        TelemetryFormatRequest req = TelemetryFormatRequest.builder()
                .vendorName("Philips").deviceModel("CityTouch-200")
                .fieldDefinitions(List.of(Map.of("name", "rssi", "type", "NUMBER")))
                .build();

        mockMvc.perform(put(BASE_URL + "/1")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void update_fieldInUse_returns400() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(formatService.update(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.IOT_TELEMETRY_FORMAT_FIELD_IN_USE));

        TelemetryFormatRequest req = TelemetryFormatRequest.builder()
                .vendorName("Philips").deviceModel("CityTouch-200")
                .fieldDefinitions(List.of(Map.of("name", "voltage", "type", "NUMBER")))
                .build();

        mockMvc.perform(put(BASE_URL + "/1")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(formatService.update(eq(999L), any()))
                .thenThrow(new BusinessException(ErrorCode.IOT_TELEMETRY_FORMAT_NOT_FOUND));

        TelemetryFormatRequest req = TelemetryFormatRequest.builder()
                .vendorName("X").deviceModel("Y").build();

        mockMvc.perform(put(BASE_URL + "/999")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── GET /{id}/fields ──

    @Test
    void getFields_withIotView_returns200WithVirtualField() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(formatService.getFields(1L)).thenReturn(List.of(
                TelemetryFormatFieldResponse.builder()
                        .name("rssi").type("NUMBER").unit("dBm").virtual(false).build(),
                TelemetryFormatFieldResponse.builder()
                        .name("$idle_minutes").type("NUMBER").unit("minutes").virtual(true).build()
        ));

        mockMvc.perform(get(BASE_URL + "/1/fields")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body[0].name").value("rssi"))
                .andExpect(jsonPath("$.body[0].virtual").value(false))
                .andExpect(jsonPath("$.body[1].name").value("$idle_minutes"))
                .andExpect(jsonPath("$.body[1].virtual").value(true));
    }

    @Test
    void getFields_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(formatService.getFields(999L))
                .thenThrow(new BusinessException(ErrorCode.IOT_TELEMETRY_FORMAT_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/999/fields")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }
}
