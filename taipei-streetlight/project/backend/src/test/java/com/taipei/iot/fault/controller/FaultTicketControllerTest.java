package com.taipei.iot.fault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.fault.dto.FaultTicketRequest;
import com.taipei.iot.fault.dto.FaultTicketResponse;
import com.taipei.iot.fault.enums.FaultTicketSource;
import com.taipei.iot.fault.enums.FaultTicketStatus;
import com.taipei.iot.fault.service.FaultTicketService;
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

@WebMvcTest(FaultTicketController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FaultTicketControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private FaultTicketService faultTicketService;
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
    void list_withFaultView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("FAULT_VIEW"));
        FaultTicketResponse resp = FaultTicketResponse.builder()
                .id(1L).status(FaultTicketStatus.OPEN)
                .source(FaultTicketSource.CITIZEN_REPORT).build();
        when(faultTicketService.list(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/v1/auth/faults")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.content[0].status").value("OPEN"));
    }

    @Test
    void list_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/faults"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("FAULT_VIEW"));
        when(faultTicketService.getById(1L))
                .thenReturn(FaultTicketResponse.builder().id(1L)
                        .status(FaultTicketStatus.OPEN).build());

        mockMvc.perform(get("/v1/auth/faults/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.id").value(1));
    }

    @Test
    void create_withFaultManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("FAULT_MANAGE"));
        FaultTicketRequest req = FaultTicketRequest.builder()
                .deviceId(10L).source(FaultTicketSource.PATROL)
                .description("燈不亮").build();
        when(faultTicketService.create(any()))
                .thenReturn(FaultTicketResponse.builder().id(2L)
                        .status(FaultTicketStatus.OPEN).build());

        mockMvc.perform(post("/v1/auth/faults")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.id").value(2));
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("FAULT_VIEW"));
        FaultTicketRequest req = FaultTicketRequest.builder()
                .deviceId(10L).source(FaultTicketSource.PATROL).build();

        mockMvc.perform(post("/v1/auth/faults")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void resolve_withFaultManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("FAULT_MANAGE"));
        when(faultTicketService.resolve(eq(1L), any()))
                .thenReturn(FaultTicketResponse.builder().id(1L)
                        .status(FaultTicketStatus.RESOLVED).build());

        mockMvc.perform(post("/v1/auth/faults/1/resolve")
                        .header("Authorization", "Bearer " + validToken())
                        .param("resolutionNote", "已修復"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.status").value("RESOLVED"));
    }

    @Test
    void resolve_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("FAULT_VIEW"));

        mockMvc.perform(post("/v1/auth/faults/1/resolve")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }
}
