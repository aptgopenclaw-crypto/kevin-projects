package com.taipei.iot.smartiot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.smartiot.dto.DimmingGroupRequest;
import com.taipei.iot.smartiot.dto.DimmingGroupResponse;
import com.taipei.iot.smartiot.dto.DimmingLogResponse;
import com.taipei.iot.smartiot.dto.DimmingScheduleRequest;
import com.taipei.iot.smartiot.dto.DimmingScheduleResponse;
import com.taipei.iot.smartiot.dto.GroupDimRequest;
import com.taipei.iot.smartiot.dto.InstantDimRequest;
import com.taipei.iot.smartiot.enums.DimmingCommandType;
import com.taipei.iot.smartiot.enums.DimmingResult;
import com.taipei.iot.smartiot.enums.DimmingTargetType;
import com.taipei.iot.smartiot.service.DimmingService;
import com.taipei.iot.smartiot.service.DimmingSyncService;
import com.taipei.iot.user.dto.response.PageResponse;
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

import java.time.LocalDateTime;
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

@WebMvcTest(DimmingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DimmingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private DimmingService dimmingService;
    @MockitoBean private DimmingSyncService dimmingSyncService;
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

    // ── 即時調光 ──

    @Test
    void instantDim_withDimmingPermission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_DIMMING"));
        DimmingLogResponse resp = DimmingLogResponse.builder()
                .id(1L).deviceId(10L).commandType(DimmingCommandType.INSTANT)
                .brightnessPct(50).result(DimmingResult.PENDING)
                .sentAt(LocalDateTime.now()).build();
        when(dimmingService.instantDim(any())).thenReturn(resp);

        mockMvc.perform(post("/v1/auth/iot/dimming/instant")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                InstantDimRequest.builder().deviceId(10L).brightness(50).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.deviceId").value(10))
                .andExpect(jsonPath("$.body.result").value("PENDING"));
    }

    @Test
    void instantDim_invalidBrightness_returns400() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_DIMMING"));

        mockMvc.perform(post("/v1/auth/iot/dimming/instant")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":10,\"brightness\":150}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void instantDim_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/v1/auth/iot/dimming/instant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":10,\"brightness\":50}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void instantDim_wrongPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));

        mockMvc.perform(post("/v1/auth/iot/dimming/instant")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":10,\"brightness\":50}"))
                .andExpect(status().isForbidden());
    }

    // ── 群組調光 ──

    @Test
    void groupDim_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_DIMMING"));
        DimmingLogResponse r1 = DimmingLogResponse.builder()
                .id(1L).deviceId(10L).result(DimmingResult.PENDING).build();
        DimmingLogResponse r2 = DimmingLogResponse.builder()
                .id(2L).deviceId(11L).result(DimmingResult.PENDING).build();
        when(dimmingService.groupDim(any())).thenReturn(List.of(r1, r2));

        mockMvc.perform(post("/v1/auth/iot/dimming/group")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                GroupDimRequest.builder().groupId(1L).brightness(80).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.length()").value(2));
    }

    @Test
    void groupDim_groupNotFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_DIMMING"));
        when(dimmingService.groupDim(any()))
                .thenThrow(new BusinessException(ErrorCode.IOT_DIMMING_GROUP_NOT_FOUND));

        mockMvc.perform(post("/v1/auth/iot/dimming/group")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":999,\"brightness\":80}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("88040"));
    }

    // ── 群組 CRUD ──

    @Test
    void listGroups_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        DimmingGroupResponse gr = DimmingGroupResponse.builder()
                .id(1L).groupName("A區").deviceIds(new Long[]{10L, 11L}).build();
        when(dimmingService.listGroups(any()))
                .thenReturn(new PageImpl<>(List.of(gr), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/auth/iot/dimming/groups")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].groupName").value("A區"));
    }

    @Test
    void createGroup_withManagePermission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        DimmingGroupResponse gr = DimmingGroupResponse.builder()
                .id(1L).groupName("B區").deviceIds(new Long[]{10L}).build();
        when(dimmingService.createGroup(any())).thenReturn(gr);

        mockMvc.perform(post("/v1/auth/iot/dimming/groups")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"B區\",\"deviceIds\":[10]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.groupName").value("B區"));
    }

    @Test
    void updateGroup_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        when(dimmingService.updateGroup(eq(999L), any()))
                .thenThrow(new BusinessException(ErrorCode.IOT_DIMMING_GROUP_NOT_FOUND));

        mockMvc.perform(put("/v1/auth/iot/dimming/groups/999")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"X\",\"deviceIds\":[1]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("88040"));
    }

    @Test
    void deleteGroup_withManagePermission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        doNothing().when(dimmingService).deleteGroup(1L);

        mockMvc.perform(delete("/v1/auth/iot/dimming/groups/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk());
    }

    // ── 排程 CRUD ──

    @Test
    void listSchedules_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        DimmingScheduleResponse sr = DimmingScheduleResponse.builder()
                .id(1L).scheduleName("夜間排程").targetType(DimmingTargetType.GROUP)
                .brightnessPct(30).scheduleCron("0 0 22 * * ?").enabled(true).build();
        when(dimmingService.listSchedules(any()))
                .thenReturn(new PageImpl<>(List.of(sr), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/auth/iot/dimming/schedules")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].scheduleName").value("夜間排程"));
    }

    @Test
    void createSchedule_withManagePermission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        DimmingScheduleResponse sr = DimmingScheduleResponse.builder()
                .id(1L).scheduleName("晨間開燈").brightnessPct(100)
                .scheduleCron("0 0 6 * * ?").enabled(true).build();
        when(dimmingService.createSchedule(any())).thenReturn(sr);

        mockMvc.perform(post("/v1/auth/iot/dimming/schedules")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleName\":\"晨間開燈\",\"targetType\":\"DEVICE\"," +
                                "\"targetId\":10,\"brightnessPct\":100," +
                                "\"scheduleCron\":\"0 0 6 * * ?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.scheduleName").value("晨間開燈"));
    }

    @Test
    void deleteSchedule_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_MANAGE"));
        doThrow(new BusinessException(ErrorCode.IOT_DIMMING_SCHEDULE_NOT_FOUND))
                .when(dimmingService).deleteSchedule(999L);

        mockMvc.perform(delete("/v1/auth/iot/dimming/schedules/999")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("88041"));
    }

    // ── 指令歷史 ──

    @Test
    void listLogs_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        DimmingLogResponse lr = DimmingLogResponse.builder()
                .id(1L).deviceId(10L).commandType(DimmingCommandType.INSTANT)
                .brightnessPct(50).result(DimmingResult.SUCCESS).sentAt(LocalDateTime.now()).build();
        when(dimmingService.listLogs(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(lr), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/auth/iot/dimming/logs")
                        .param("deviceId", "10")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content[0].result").value("SUCCESS"));
    }

    @Test
    void listLogs_allDevices_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(dimmingService.listLogs(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/v1/auth/iot/dimming/logs")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.content").isEmpty());
    }
}
