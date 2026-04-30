package com.taipei.iot.repair.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.common.interceptor.RateLimitInterceptor;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.repair.dto.InspectionRecordRequest;
import com.taipei.iot.repair.dto.InspectionRecordResponse;
import com.taipei.iot.repair.dto.InspectionTaskRequest;
import com.taipei.iot.repair.dto.InspectionTaskResponse;
import com.taipei.iot.repair.enums.InspectionResult;
import com.taipei.iot.repair.enums.InspectionTaskStatus;
import com.taipei.iot.repair.enums.InspectionTaskType;
import com.taipei.iot.repair.service.InspectionService;
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

@WebMvcTest(InspectionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InspectionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private InspectionService inspectionService;
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

    // ── GET /v1/auth/inspection/tasks ──

    @Test
    void listTasks_withPermission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("INSPECTION_VIEW"));
        InspectionTaskResponse resp = InspectionTaskResponse.builder()
                .id(1L).taskName("忠孝東路巡查")
                .status(InspectionTaskStatus.ACTIVE).build();
        when(inspectionService.listTasks(any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/v1/auth/inspection/tasks")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].taskName").value("忠孝東路巡查"));
    }

    @Test
    void listTasks_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());
        mockMvc.perform(get("/v1/auth/inspection/tasks")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── POST /v1/auth/inspection/tasks ──

    @Test
    void createTask_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("INSPECTION_MANAGE"));
        InspectionTaskRequest req = InspectionTaskRequest.builder()
                .taskName("巡查任務A")
                .taskType(InspectionTaskType.ONE_TIME).build();
        when(inspectionService.createTask(any()))
                .thenReturn(InspectionTaskResponse.builder().id(1L)
                        .taskName("巡查任務A").status(InspectionTaskStatus.ACTIVE).build());

        mockMvc.perform(post("/v1/auth/inspection/tasks")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.taskName").value("巡查任務A"));
    }

    // ── DELETE /v1/auth/inspection/tasks/{id} ──

    @Test
    void deactivateTask_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("INSPECTION_MANAGE"));

        mockMvc.perform(delete("/v1/auth/inspection/tasks/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());

        verify(inspectionService).deactivateTask(1L);
    }

    // ── POST /v1/auth/inspection/records ──

    @Test
    void createRecord_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("INSPECTION_MANAGE"));
        InspectionRecordRequest req = InspectionRecordRequest.builder()
                .taskId(1L).deviceId(5L)
                .result(InspectionResult.NORMAL)
                .notes("正常").build();
        when(inspectionService.createRecord(any()))
                .thenReturn(InspectionRecordResponse.builder().id(10L)
                        .result(InspectionResult.NORMAL).build());

        mockMvc.perform(post("/v1/auth/inspection/records")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ── GET /v1/auth/inspection/tasks/{taskId}/records ──

    @Test
    void listRecords_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("INSPECTION_VIEW"));
        InspectionRecordResponse resp = InspectionRecordResponse.builder()
                .id(1L).result(InspectionResult.NORMAL).build();
        when(inspectionService.getRecordsByTask(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/v1/auth/inspection/tasks/1/records")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].result").value("NORMAL"));
    }
}
