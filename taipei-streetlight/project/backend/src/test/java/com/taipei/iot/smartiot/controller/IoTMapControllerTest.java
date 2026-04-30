package com.taipei.iot.smartiot.controller;

import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.gis.dto.GeoJsonResponse;
import com.taipei.iot.gis.dto.GeoJsonResponse.Feature;
import com.taipei.iot.gis.dto.GeoJsonResponse.Geometry;
import com.taipei.iot.smartiot.service.IoTMapService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IoTMapController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class IoTMapControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IoTMapService iotMapService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private static final String URL = "/v1/auth/iot/map/status";

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

    private GeoJsonResponse sampleGeoJson() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", 1L);
        props.put("deviceCode", "SL-001");
        props.put("displayStatus", "ONLINE");

        Feature feature = Feature.of(
                Geometry.point(BigDecimal.valueOf(121.5654), BigDecimal.valueOf(25.0330)),
                props);
        return GeoJsonResponse.of(List.of(feature));
    }

    // ── TC-1: 正常查詢 ──
    @Test
    void getMapStatus_withIotView_returns200() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(iotMapService.getMapStatus()).thenReturn(sampleGeoJson());

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.body.features[0].type").value("Feature"))
                .andExpect(jsonPath("$.body.features[0].properties.deviceCode").value("SL-001"))
                .andExpect(jsonPath("$.body.features[0].properties.displayStatus").value("ONLINE"));
    }

    // ── TC-2: 無設備回傳空 FeatureCollection ──
    @Test
    void getMapStatus_noDevices_returnsEmpty() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(iotMapService.getMapStatus()).thenReturn(GeoJsonResponse.of(List.of()));

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.body.features").isEmpty());
    }

    // ── TC-3: 無 token → 401 ──
    @Test
    void getMapStatus_noAuth_returns401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    // ── TC-4: 無權限 → 403 ──
    @Test
    void getMapStatus_noPermission_returns403() throws Exception {
        mockJwtValid(validToken(), List.of("DEVICE_VIEW")); // wrong permission

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── TC-5: GeoJSON geometry 座標驗證 ──
    @Test
    void getMapStatus_hasCorrectGeometry() throws Exception {
        mockJwtValid(validToken(), List.of("IOT_VIEW"));
        when(iotMapService.getMapStatus()).thenReturn(sampleGeoJson());

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.features[0].geometry.type").value("Point"))
                .andExpect(jsonPath("$.body.features[0].geometry.coordinates[0]").value(121.5654))
                .andExpect(jsonPath("$.body.features[0].geometry.coordinates[1]").value(25.033));
    }
}
