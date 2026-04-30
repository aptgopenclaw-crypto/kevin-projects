package com.taipei.iot.repair.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.auth.service.CaptchaService;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.common.interceptor.RateLimitInterceptor;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.repair.dto.PublicRepairRequest;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.dto.PublicRepairStatusResponse;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.service.RepairTicketService;
import com.taipei.iot.tenant.TenantInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicRepairController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PublicRepairControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private RepairTicketService repairTicketService;
    @MockitoBean private CaptchaService captchaService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private PublicRepairRequest validRequest() {
        PublicRepairRequest req = new PublicRepairRequest();
        req.setReporterName("張三");
        req.setReporterPhone("0912345678");
        req.setReportAddress("台北市信義區");
        req.setReportDescription("路燈不亮");
        req.setCaptchaKey("key-123");
        req.setCaptchaValue("abcd");
        req.setPrivacyAgreed(true);
        return req;
    }

    @Test
    void submitRepair_success_returns200() throws Exception {
        PublicRepairRequest req = validRequest();
        when(captchaService.verify("key-123", "abcd")).thenReturn(true);

        RepairTicket ticket = RepairTicket.builder().ticketNumber("RT-20260424-001").build();
        when(repairTicketService.createPublicTicket(any())).thenReturn(ticket);

        mockMvc.perform(post("/v1/noauth/public/repair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.ticketNumber").value("RT-20260424-001"));
    }

    @Test
    void submitRepair_captchaInvalid_returns400() throws Exception {
        PublicRepairRequest req = validRequest();
        when(captchaService.verify("key-123", "abcd")).thenReturn(false);

        mockMvc.perform(post("/v1/noauth/public/repair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitRepair_privacyNotAgreed_returns400() throws Exception {
        PublicRepairRequest req = validRequest();
        req.setPrivacyAgreed(false);

        mockMvc.perform(post("/v1/noauth/public/repair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitRepair_missingRequiredFields_returns400() throws Exception {
        PublicRepairRequest req = new PublicRepairRequest();

        mockMvc.perform(post("/v1/noauth/public/repair")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryStatus_success_returns200() throws Exception {
        PublicRepairStatusResponse resp = PublicRepairStatusResponse.builder()
                .ticketNumber("RT-20260424-001")
                .status(RepairTicketStatus.PENDING)
                .statusLabel("待派工")
                .createdAt(LocalDateTime.of(2026, 4, 24, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 24, 10, 0))
                .build();
        when(repairTicketService.getPublicStatus("RT-20260424-001", "0912345678")).thenReturn(resp);

        mockMvc.perform(get("/v1/noauth/public/repair/RT-20260424-001/status")
                        .param("phone", "0912345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.ticketNumber").value("RT-20260424-001"))
                .andExpect(jsonPath("$.body.statusLabel").value("待派工"));
    }
}
