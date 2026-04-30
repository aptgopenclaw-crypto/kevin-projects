package com.taipei.iot.replacement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.replacement.dto.ReplacementItemRequest;
import com.taipei.iot.replacement.dto.ReplacementItemResponse;
import com.taipei.iot.replacement.dto.ReplacementOrderRequest;
import com.taipei.iot.replacement.dto.ReplacementOrderResponse;
import com.taipei.iot.replacement.dto.SelfCheckItemRequest;
import com.taipei.iot.replacement.dto.SelfCheckRequest;
import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderType;
import com.taipei.iot.replacement.service.ReplacementItemService;
import com.taipei.iot.replacement.service.ReplacementOrderService;
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

@WebMvcTest(ReplacementOrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReplacementOrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ReplacementOrderService orderService;
    @MockitoBean private ReplacementItemService itemService;
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

    // ── GET /v1/auth/replacement/orders ──

    @Test
    void list_withPermission_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPLACEMENT_VIEW"));
        ReplacementOrderResponse resp = ReplacementOrderResponse.builder()
                .id(1L).orderNumber("RO-20240101-001")
                .status(ReplacementOrderStatus.DRAFT).build();
        when(orderService.list(any(), any())).thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/v1/auth/replacement/orders")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.content[0].orderNumber").value("RO-20240101-001"));
    }

    @Test
    void list_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of());
        mockMvc.perform(get("/v1/auth/replacement/orders")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── GET /v1/auth/replacement/orders/{id} ──

    @Test
    void getById_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPLACEMENT_VIEW"));
        when(orderService.getById(1L)).thenReturn(
                ReplacementOrderResponse.builder().id(1L).orderNumber("RO-001").build());

        mockMvc.perform(get("/v1/auth/replacement/orders/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.orderNumber").value("RO-001"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("REPLACEMENT_VIEW"));
        when(orderService.getById(99L))
                .thenThrow(new BusinessException(ErrorCode.REPLACEMENT_ORDER_NOT_FOUND));

        mockMvc.perform(get("/v1/auth/replacement/orders/99")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }

    // ── POST /v1/auth/replacement/orders ──

    @Test
    void create_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPLACEMENT_MANAGE"));
        ReplacementOrderRequest request = ReplacementOrderRequest.builder()
                .orderType(ReplacementOrderType.REPLACEMENT).build();
        when(orderService.createDirect(any())).thenReturn(
                ReplacementOrderResponse.builder().id(1L).status(ReplacementOrderStatus.DRAFT).build());

        mockMvc.perform(post("/v1/auth/replacement/orders")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.status").value("DRAFT"));
    }

    // ── POST /v1/auth/replacement/orders/{id}/dispatch ──

    @Test
    void dispatch_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPLACEMENT_MANAGE"));
        ReplacementOrderRequest request = ReplacementOrderRequest.builder()
                .orderType(ReplacementOrderType.REPLACEMENT)
                .assignedContractor("承商A").build();
        doNothing().when(orderService).dispatch(eq(1L), any());

        mockMvc.perform(post("/v1/auth/replacement/orders/1/dispatch")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ── POST /v1/auth/replacement/orders/{id}/self-check ──

    @Test
    void selfCheck_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPLACEMENT_MANAGE"));
        SelfCheckRequest request = SelfCheckRequest.builder()
                .items(List.of(SelfCheckItemRequest.builder()
                        .itemId(10L).deviceCode("LED-001").build()))
                .build();
        doNothing().when(orderService).selfCheck(eq(1L), any());

        mockMvc.perform(post("/v1/auth/replacement/orders/1/self-check")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ── Items ──

    @Test
    void getItems_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPLACEMENT_VIEW"));
        when(itemService.getItems(1L)).thenReturn(List.of(
                ReplacementItemResponse.builder().id(10L).orderId(1L)
                        .status(ReplacementItemStatus.PENDING).build()));

        mockMvc.perform(get("/v1/auth/replacement/orders/1/items")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body[0].id").value(10));
    }

    @Test
    void addItem_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("REPLACEMENT_MANAGE"));
        ReplacementItemRequest request = ReplacementItemRequest.builder()
                .parentDeviceId(100L).oldDeviceId(200L).build();
        when(itemService.addItem(eq(1L), any())).thenReturn(
                ReplacementItemResponse.builder().id(10L).status(ReplacementItemStatus.PENDING).build());

        mockMvc.perform(post("/v1/auth/replacement/orders/1/items")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.id").value(10));
    }
}
