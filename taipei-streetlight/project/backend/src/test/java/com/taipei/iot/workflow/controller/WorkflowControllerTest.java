package com.taipei.iot.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.workflow.dto.WorkflowInstanceResponse;
import com.taipei.iot.workflow.dto.WorkflowStepLogResponse;
import com.taipei.iot.workflow.dto.WorkflowTransitionRequest;
import com.taipei.iot.workflow.enums.WorkflowStatus;
import com.taipei.iot.workflow.service.WorkflowService;
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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WorkflowControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private WorkflowService workflowService;
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
    void getPendingTasks_withWorkflowView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("WORKFLOW_VIEW"));
        WorkflowInstanceResponse resp = WorkflowInstanceResponse.builder()
                .id(1L).workflowType("FAULT_REVIEW").currentStep("REVIEW")
                .status(WorkflowStatus.ACTIVE).build();
        when(workflowService.getMyPendingTasks(eq("user-001"), any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/v1/auth/workflow/pending")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.content[0].workflowType").value("FAULT_REVIEW"));
    }

    @Test
    void getPendingTasks_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/workflow/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStepLogs_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("WORKFLOW_VIEW"));
        WorkflowStepLogResponse log = WorkflowStepLogResponse.builder()
                .id(1L).stepCode("REVIEW").action("APPROVE")
                .actorId("reviewer-001").actedAt(LocalDateTime.now()).build();
        when(workflowService.getStepLogs(1L)).thenReturn(List.of(log));

        mockMvc.perform(get("/v1/auth/workflow/1/logs")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body[0].stepCode").value("REVIEW"));
    }

    @Test
    void transition_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("WORKFLOW_VIEW"));
        WorkflowTransitionRequest req = WorkflowTransitionRequest.builder()
                .targetStep("REVIEW").action("SUBMIT").comment("OK").build();

        mockMvc.perform(post("/v1/auth/workflow/1/transition")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(workflowService).transition(eq(1L), eq("REVIEW"), eq("SUBMIT"),
                eq("user-001"), isNull(), eq("OK"), isNull());
    }

    @Test
    void cancel_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("WORKFLOW_VIEW"));

        mockMvc.perform(post("/v1/auth/workflow/1/cancel")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(workflowService).cancel(1L, "user-001");
    }

    @Test
    void cancel_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());

        mockMvc.perform(post("/v1/auth/workflow/1/cancel")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }
}
