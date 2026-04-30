package com.taipei.iot.repair.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.common.interceptor.RateLimitInterceptor;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.repair.dto.CompletionReportRequest;
import com.taipei.iot.repair.dto.DispatchRequest;
import com.taipei.iot.repair.dto.DispatchResponse;
import com.taipei.iot.repair.dto.RepairTicketRequest;
import com.taipei.iot.repair.dto.RepairTicketResponse;
import com.taipei.iot.repair.enums.RepairDispatchStatus;
import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.service.RepairDispatchService;
import com.taipei.iot.repair.service.RepairTicketService;
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

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepairTicketController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RepairTicketControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private RepairTicketService repairTicketService;
    @MockitoBean private RepairDispatchService repairDispatchService;
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

    // ── GET /v1/auth/repair/tickets ──

    @Test
    void list_withPermission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_VIEW"));
        RepairTicketResponse resp = RepairTicketResponse.builder()
                .id(1L).ticketNumber("RT-20260422-001")
                .status(RepairTicketStatus.PENDING).build();
        when(repairTicketService.list(any(), any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/v1/auth/repair/tickets")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.content[0].ticketNumber").value("RT-20260422-001"));
    }

    @Test
    void list_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());
        mockMvc.perform(get("/v1/auth/repair/tickets")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── GET /v1/auth/repair/tickets/{id} ──

    @Test
    void getById_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_VIEW"));
        when(repairTicketService.getById(1L))
                .thenReturn(RepairTicketResponse.builder().id(1L).ticketNumber("RT-001").build());

        mockMvc.perform(get("/v1/auth/repair/tickets/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.ticketNumber").value("RT-001"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_VIEW"));
        when(repairTicketService.getById(99L))
                .thenThrow(new BusinessException(ErrorCode.REPAIR_TICKET_NOT_FOUND));

        mockMvc.perform(get("/v1/auth/repair/tickets/99")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }

    // ── POST /v1/auth/repair/tickets ──

    @Test
    void create_withRepairManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_MANAGE"));
        RepairTicketRequest req = RepairTicketRequest.builder()
                .source(RepairTicketSource.PHONE)
                .reporterName("王先生")
                .build();
        when(repairTicketService.createDirect(any()))
                .thenReturn(RepairTicketResponse.builder().id(1L).build());

        mockMvc.perform(post("/v1/auth/repair/tickets")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_VIEW"));
        RepairTicketRequest req = RepairTicketRequest.builder()
                .source(RepairTicketSource.PHONE).build();

        mockMvc.perform(post("/v1/auth/repair/tickets")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── POST /v1/auth/repair/tickets/{id}/accept ──

    @Test
    void accept_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_MANAGE"));
        when(repairTicketService.accept(1L))
                .thenReturn(RepairTicketResponse.builder().id(1L)
                        .status(RepairTicketStatus.ACCEPTED).build());

        mockMvc.perform(post("/v1/auth/repair/tickets/1/accept")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());
    }

    // ── POST /v1/auth/repair/tickets/{id}/dispatch ──

    @Test
    void dispatch_withRepairDispatch_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_DISPATCH"));
        DispatchRequest req = DispatchRequest.builder()
                .assignedOrg("光輝公司").contractId(5L)
                .dueDate(LocalDate.now().plusDays(7)).build();
        when(repairDispatchService.dispatch(eq(1L), any()))
                .thenReturn(DispatchResponse.builder().id(10L)
                        .status(RepairDispatchStatus.DISPATCHED).build());

        mockMvc.perform(post("/v1/auth/repair/tickets/1/dispatch")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ── POST /v1/auth/repair/tickets/{id}/complete ──

    @Test
    void complete_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_MANAGE"));
        CompletionReportRequest req = CompletionReportRequest.builder()
                .repairDescription("已更換燈泡").faultCause("老化").build();

        mockMvc.perform(post("/v1/auth/repair/tickets/1/complete")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ── POST /v1/auth/repair/tickets/{id}/transfer ──

    @Test
    void transfer_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPAIR_MANAGE"));
        when(repairTicketService.transfer(1L))
                .thenReturn(RepairTicketResponse.builder().id(1L)
                        .status(RepairTicketStatus.TRANSFERRED).build());

        mockMvc.perform(post("/v1/auth/repair/tickets/1/transfer")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());
    }
}
