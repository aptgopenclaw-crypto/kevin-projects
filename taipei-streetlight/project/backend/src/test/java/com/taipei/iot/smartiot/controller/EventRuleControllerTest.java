package com.taipei.iot.smartiot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.smartiot.dto.EventRuleConditionRequest;
import com.taipei.iot.smartiot.dto.EventRuleConditionResponse;
import com.taipei.iot.smartiot.dto.EventRuleRequest;
import com.taipei.iot.smartiot.dto.EventRuleResponse;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.ConditionLogic;
import com.taipei.iot.smartiot.enums.ConditionOperator;
import com.taipei.iot.smartiot.service.EventRuleService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventRuleController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EventRuleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private EventRuleService eventRuleService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private static final String BASE_URL = "/v1/auth/iot/event-rules";

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

    private EventRuleResponse sampleResponse() {
        return EventRuleResponse.builder()
                .id(1L)
                .tenantId("TENANT_A")
                .ruleName("Low RSSI Alert")
                .severity(AlertSeverity.WARNING)
                .conditionLogic(ConditionLogic.AND)
                .suppressDurationMin(30)
                .autoCreateTicket(false)
                .enabled(true)
                .conditions(List.of(
                        EventRuleConditionResponse.builder()
                                .id(1L).ruleId(1L).conditionGroup(1)
                                .field("rssi").operator(ConditionOperator.LTE)
                                .thresholdValue("-100").sortOrder(0).build()
                ))
                .build();
    }

    // ── POST / (create) ──

    @Test
    void create_withIotManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(eventRuleService.create(any())).thenReturn(sampleResponse());

        EventRuleRequest req = EventRuleRequest.builder()
                .ruleName("Low RSSI Alert")
                .severity(AlertSeverity.WARNING)
                .conditions(List.of(
                        EventRuleConditionRequest.builder()
                                .field("rssi").operator(ConditionOperator.LTE)
                                .thresholdValue("-100").build()
                ))
                .build();

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.ruleName").value("Low RSSI Alert"))
                .andExpect(jsonPath("$.body.conditions[0].field").value("rssi"));
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

        EventRuleRequest req = EventRuleRequest.builder()
                .ruleName("Test").severity(AlertSeverity.INFO).build();

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
        when(eventRuleService.list(any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].ruleName").value("Low RSSI Alert"))
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
        when(eventRuleService.update(eq(1L), any())).thenReturn(sampleResponse());

        EventRuleRequest req = EventRuleRequest.builder()
                .ruleName("Low RSSI Alert").severity(AlertSeverity.WARNING).build();

        mockMvc.perform(put(BASE_URL + "/1")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(eventRuleService.update(eq(999L), any()))
                .thenThrow(new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND));

        EventRuleRequest req = EventRuleRequest.builder()
                .ruleName("X").severity(AlertSeverity.INFO).build();

        mockMvc.perform(put(BASE_URL + "/999")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /{id} ──

    @Test
    void delete_withIotManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        doNothing().when(eventRuleService).delete(1L);

        mockMvc.perform(delete(BASE_URL + "/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        doThrow(new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND))
                .when(eventRuleService).delete(999L);

        mockMvc.perform(delete(BASE_URL + "/999")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));

        mockMvc.perform(delete(BASE_URL + "/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── GET /{id}/conditions ──

    @Test
    void getConditions_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(eventRuleService.getConditions(1L)).thenReturn(List.of(
                EventRuleConditionResponse.builder()
                        .id(1L).ruleId(1L).conditionGroup(1)
                        .field("rssi").operator(ConditionOperator.LTE)
                        .thresholdValue("-100").sortOrder(0).build()
        ));

        mockMvc.perform(get(BASE_URL + "/1/conditions")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body[0].field").value("rssi"))
                .andExpect(jsonPath("$.body[0].operator").value("LTE"));
    }

    @Test
    void getConditions_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(eventRuleService.getConditions(999L))
                .thenThrow(new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/999/conditions")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }

    // ── PUT /{id}/conditions ──

    @Test
    void updateConditions_withIotManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(eventRuleService.updateConditions(eq(1L), any())).thenReturn(List.of(
                EventRuleConditionResponse.builder()
                        .id(2L).ruleId(1L).conditionGroup(1)
                        .field("voltage").operator(ConditionOperator.LT)
                        .thresholdValue("180").sortOrder(0).build()
        ));

        List<EventRuleConditionRequest> reqs = List.of(
                EventRuleConditionRequest.builder()
                        .field("voltage").operator(ConditionOperator.LT)
                        .thresholdValue("180").build()
        );

        mockMvc.perform(put(BASE_URL + "/1/conditions")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body[0].field").value("voltage"));
    }
}
