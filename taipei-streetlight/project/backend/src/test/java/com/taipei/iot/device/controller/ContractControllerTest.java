package com.taipei.iot.device.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.device.dto.ContractRequest;
import com.taipei.iot.device.dto.ContractResponse;
import com.taipei.iot.device.enums.ContractStatus;
import com.taipei.iot.device.service.ContractService;
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

@WebMvcTest(ContractController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ContractControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ContractService contractService;
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
    void list_withContractView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("CONTRACT_VIEW"));
        ContractResponse resp = ContractResponse.builder()
                .id(1L).contractCode("C-114-001").status(ContractStatus.ACTIVE).build();
        when(contractService.list(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/v1/auth/contracts")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.content[0].contractCode").value("C-114-001"));
    }

    @Test
    void list_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/auth/contracts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("CONTRACT_VIEW"));
        when(contractService.getById(1L))
                .thenReturn(ContractResponse.builder().id(1L).contractCode("C-114-001").build());

        mockMvc.perform(get("/v1/auth/contracts/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.contractCode").value("C-114-001"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockJwtValid(validToken(), List.of("CONTRACT_VIEW"));
        when(contractService.getById(99L))
                .thenThrow(new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));

        mockMvc.perform(get("/v1/auth/contracts/99")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withContractManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("CONTRACT_MANAGE"));
        ContractRequest req = ContractRequest.builder()
                .contractCode("C-NEW").contractName("New Contract").build();
        when(contractService.create(any()))
                .thenReturn(ContractResponse.builder().id(2L).contractCode("C-NEW").build());

        mockMvc.perform(post("/v1/auth/contracts")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.contractCode").value("C-NEW"));
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("CONTRACT_VIEW"));
        ContractRequest req = ContractRequest.builder()
                .contractCode("C-NEW").contractName("New").build();

        mockMvc.perform(post("/v1/auth/contracts")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withContractManage_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("CONTRACT_MANAGE"));

        mockMvc.perform(delete("/v1/auth/contracts/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));

        verify(contractService).delete(1L);
    }
}
