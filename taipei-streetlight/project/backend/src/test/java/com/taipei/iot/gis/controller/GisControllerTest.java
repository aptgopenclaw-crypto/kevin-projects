package com.taipei.iot.gis.controller;

import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.common.interceptor.RateLimitInterceptor;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.gis.dto.GeoJsonResponse;
import com.taipei.iot.gis.dto.GeoJsonResponse.Feature;
import com.taipei.iot.gis.dto.GeoJsonResponse.Geometry;
import com.taipei.iot.gis.dto.GmlImportDiff;
import com.taipei.iot.gis.dto.GmlImportDiff.ImportRow;
import com.taipei.iot.gis.service.GisService;
import com.taipei.iot.gis.service.GmlExportService;
import com.taipei.iot.gis.service.GmlImportService;
import com.taipei.iot.gis.service.MapZoneService;
import com.taipei.iot.tenant.TenantInterceptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GisController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GisControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GisService gisService;
    @MockitoBean private MapZoneService mapZoneService;
    @MockitoBean private GmlExportService gmlExportService;
    @MockitoBean private GmlImportService gmlImportService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private String validToken() { return "valid.jwt.token"; }

    private void mockJwtValid(String token) {
        mockJwtWithPermissions(token, List.of("GIS_VIEW"));
    }

    private void mockJwtWithPermissions(String token, List<String> permissions) {
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
        props.put("deviceName", "路燈-001");
        props.put("deviceType", "STREETLIGHT");
        props.put("status", "ACTIVE");
        props.put("deptId", 6L);
        Feature feature = Feature.of(
                Geometry.point(new BigDecimal("121.5200"), new BigDecimal("25.0338")),
                props);
        return GeoJsonResponse.of(List.of(feature));
    }

    // ── GET /v1/gis/devices ──

    @Test
    void allDevices_authenticated_returns200() throws Exception {
        mockJwtValid(validToken());
        when(gisService.findAllDevices(isNull())).thenReturn(sampleGeoJson());

        mockMvc.perform(get("/v1/gis/devices")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.body.features[0].type").value("Feature"))
                .andExpect(jsonPath("$.body.features[0].geometry.type").value("Point"))
                .andExpect(jsonPath("$.body.features[0].properties.deviceCode").value("SL-001"));
    }

    @Test
    void allDevices_withDeviceTypeFilter_returns200() throws Exception {
        mockJwtValid(validToken());
        when(gisService.findAllDevices(eq("STREETLIGHT"))).thenReturn(sampleGeoJson());

        mockMvc.perform(get("/v1/gis/devices")
                        .param("deviceType", "STREETLIGHT")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.features").isArray());
    }

    @Test
    void allDevices_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/gis/devices"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/gis/devices/bounds ──

    @Test
    void devicesInBounds_authenticated_returns200() throws Exception {
        mockJwtValid(validToken());
        when(gisService.findDevicesInBounds(any(), any(), any(), any(), isNull(), isNull()))
                .thenReturn(sampleGeoJson());

        mockMvc.perform(get("/v1/gis/devices/bounds")
                        .param("minLng", "121.51")
                        .param("minLat", "25.03")
                        .param("maxLng", "121.53")
                        .param("maxLat", "25.04")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.type").value("FeatureCollection"));
    }

    @Test
    void devicesInBounds_withZoom_returns200() throws Exception {
        mockJwtValid(validToken());
        when(gisService.findDevicesInBounds(any(), any(), any(), any(), isNull(), eq(12)))
                .thenReturn(sampleGeoJson());

        mockMvc.perform(get("/v1/gis/devices/bounds")
                        .param("minLng", "121.51")
                        .param("minLat", "25.03")
                        .param("maxLng", "121.53")
                        .param("maxLat", "25.04")
                        .param("zoom", "12")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.type").value("FeatureCollection"));
    }

    @Test
    void devicesInBounds_missingParam_returns400() throws Exception {
        mockJwtValid(validToken());

        mockMvc.perform(get("/v1/gis/devices/bounds")
                        .param("minLng", "121.51")
                        // missing minLat, maxLng, maxLat
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devicesInBounds_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/gis/devices/bounds")
                        .param("minLng", "121.51")
                        .param("minLat", "25.03")
                        .param("maxLng", "121.53")
                        .param("maxLat", "25.04"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/gis/devices/nearby ──

    @Test
    void devicesNearby_authenticated_returns200() throws Exception {
        mockJwtValid(validToken());
        when(gisService.findDevicesNearby(any(), any(), eq(500.0)))
                .thenReturn(sampleGeoJson());

        mockMvc.perform(get("/v1/gis/devices/nearby")
                        .param("lng", "121.52")
                        .param("lat", "25.034")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.features[0].properties.deviceCode").value("SL-001"));
    }

    @Test
    void devicesNearby_customRadius_returns200() throws Exception {
        mockJwtValid(validToken());
        when(gisService.findDevicesNearby(any(), any(), eq(1000.0)))
                .thenReturn(sampleGeoJson());

        mockMvc.perform(get("/v1/gis/devices/nearby")
                        .param("lng", "121.52")
                        .param("lat", "25.034")
                        .param("radius", "1000")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"));
    }

    @Test
    void devicesNearby_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/gis/devices/nearby")
                        .param("lng", "121.52")
                        .param("lat", "25.034"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devicesNearby_missingParams_returns400() throws Exception {
        mockJwtValid(validToken());

        mockMvc.perform(get("/v1/gis/devices/nearby")
                        // missing lng and lat
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isBadRequest());
    }

    // ── 403 — no GIS_VIEW permission ──

    @Test
    void allDevices_noPermission_returns403() throws Exception {
        mockJwtWithPermissions(validToken(), List.of());
        mockMvc.perform(get("/v1/gis/devices")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void devicesInBounds_noPermission_returns403() throws Exception {
        mockJwtWithPermissions(validToken(), List.of());
        mockMvc.perform(get("/v1/gis/devices/bounds")
                        .param("minLng", "121.51")
                        .param("minLat", "25.03")
                        .param("maxLng", "121.53")
                        .param("maxLat", "25.04")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void devicesNearby_noPermission_returns403() throws Exception {
        mockJwtWithPermissions(validToken(), List.of());
        mockMvc.perform(get("/v1/gis/devices/nearby")
                        .param("lng", "121.52")
                        .param("lat", "25.034")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── GET /v1/gis/zones ──

    @Test
    void zones_authenticated_returns200() throws Exception {
        mockJwtValid(validToken());
        when(mapZoneService.getZonesByType(any())).thenReturn(GeoJsonResponse.of(List.of()));

        mockMvc.perform(get("/v1/gis/zones")
                        .param("type", "ADMIN_DISTRICT")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.type").value("FeatureCollection"));
    }

    @Test
    void zones_missingType_returns400() throws Exception {
        mockJwtValid(validToken());

        mockMvc.perform(get("/v1/gis/zones")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zones_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/gis/zones")
                        .param("type", "ADMIN_DISTRICT"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/gis/zones/{id}/devices ──

    @Test
    void devicesInZone_authenticated_returns200() throws Exception {
        mockJwtValid(validToken());
        when(mapZoneService.findDevicesInZone(eq(1L))).thenReturn(sampleGeoJson());

        mockMvc.perform(get("/v1/gis/zones/1/devices")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.features[0].properties.deviceCode").value("SL-001"));
    }

    @Test
    void devicesInZone_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/gis/zones/1/devices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void zones_noPermission_returns403() throws Exception {
        mockJwtWithPermissions(validToken(), List.of());
        mockMvc.perform(get("/v1/gis/zones")
                        .param("type", "ADMIN_DISTRICT")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    // ── GET /v1/gis/devices/export/gml ──

    @Test
    void exportGml_authenticated_returnsXml() throws Exception {
        mockJwtValid(validToken());
        when(gmlExportService.exportAsGml(isNull(), isNull()))
                .thenReturn("<?xml version=\"1.0\"?><gml:FeatureCollection/>");

        mockMvc.perform(get("/v1/gis/devices/export/gml")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=streetlight_export.gml"))
                .andExpect(content().contentType(MediaType.APPLICATION_XML))
                .andExpect(content().string("<?xml version=\"1.0\"?><gml:FeatureCollection/>"));
    }

    @Test
    void exportGml_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/gis/devices/export/gml"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/gis/devices/export/open-data ──

    @Test
    void exportOpenData_authenticated_returnsCsv() throws Exception {
        mockJwtValid(validToken());
        when(gmlExportService.exportAsOpenDataCsv(isNull())).thenReturn("header\ndata\n");

        mockMvc.perform(get("/v1/gis/devices/export/open-data")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=streetlight_opendata.csv"));
    }

    @Test
    void exportOpenData_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/gis/devices/export/open-data"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /v1/gis/devices/import/gml ──

    @Test
    void importGmlPreview_authenticated_returnsDiff() throws Exception {
        mockJwtWithPermissions(validToken(), List.of("GIS_VIEW", "GIS_MANAGE"));
        GmlImportDiff diff = new GmlImportDiff(List.of(), List.of(), List.of(), 5);
        when(gmlImportService.parseAndDiff(any())).thenReturn(diff);

        MockMultipartFile file = new MockMultipartFile("file", "test.gml",
                "application/xml", "<gml/>".getBytes());

        mockMvc.perform(multipart("/v1/gis/devices/import/gml").file(file)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body.totalParsed").value(5));
    }

    @Test
    void importGmlPreview_noManagePermission_returns403() throws Exception {
        mockJwtWithPermissions(validToken(), List.of("GIS_VIEW"));

        MockMultipartFile file = new MockMultipartFile("file", "test.gml",
                "application/xml", "<gml/>".getBytes());

        mockMvc.perform(multipart("/v1/gis/devices/import/gml").file(file)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void importGmlPreview_noToken_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.gml",
                "application/xml", "<gml/>".getBytes());

        mockMvc.perform(multipart("/v1/gis/devices/import/gml").file(file))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /v1/gis/devices/import/gml/confirm ──

    @Test
    void importGmlConfirm_authenticated_returnsCount() throws Exception {
        mockJwtWithPermissions(validToken(), List.of("GIS_VIEW", "GIS_MANAGE"));
        when(gmlImportService.applyImport(any())).thenReturn(3);

        String body = """
                {"toAdd":[],"toUpdate":[],"toDelete":[],"totalParsed":0}
                """;

        mockMvc.perform(post("/v1/gis/devices/import/gml/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCode").value("00000"))
                .andExpect(jsonPath("$.body").value(3));
    }

    @Test
    void importGmlConfirm_noManagePermission_returns403() throws Exception {
        mockJwtWithPermissions(validToken(), List.of("GIS_VIEW"));

        mockMvc.perform(post("/v1/gis/devices/import/gml/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isForbidden());
    }
}
