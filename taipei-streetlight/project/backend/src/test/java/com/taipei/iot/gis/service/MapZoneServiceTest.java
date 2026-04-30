package com.taipei.iot.gis.service;

import com.taipei.iot.gis.dto.GeoJsonResponse;
import com.taipei.iot.gis.entity.ZoneType;
import com.taipei.iot.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapZoneServiceTest {

    @Mock private EntityManager em;
    @Mock private Query query;

    @InjectMocks private MapZoneService mapZoneService;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("TAIPEI");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getZonesByType_returnsFeatureCollection() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        Object[] row = {1L, "songshan", "松山區", "ADMIN_DISTRICT",
                "{\"population\": 204000}",
                "{\"type\":\"Polygon\",\"coordinates\":[[[121.55,25.05],[121.57,25.05],[121.57,25.062],[121.55,25.062],[121.55,25.05]]]}",
                42L};
        List<Object[]> results = new ArrayList<>();
        results.add(row);
        when(query.getResultList()).thenReturn(results);

        GeoJsonResponse result = mapZoneService.getZonesByType(ZoneType.ADMIN_DISTRICT);

        assertThat(result.type()).isEqualTo("FeatureCollection");
        assertThat(result.features()).hasSize(1);
        assertThat(result.features().get(0).properties().get("zoneName")).isEqualTo("松山區");
        assertThat(result.features().get(0).properties().get("deviceCount")).isEqualTo(42L);
    }

    @Test
    void getZonesByType_emptyResult() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        GeoJsonResponse result = mapZoneService.getZonesByType(ZoneType.SQUAD);

        assertThat(result.features()).isEmpty();
    }

    @Test
    void findDevicesInZone_returnsDeviceFeatures() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        Object[] row = {1L, "SL-001", "路燈-001", "STREETLIGHT", "ACTIVE",
                new BigDecimal("121.52"), new BigDecimal("25.034"), 6L};
        List<Object[]> results = new ArrayList<>();
        results.add(row);
        when(query.getResultList()).thenReturn(results);

        GeoJsonResponse result = mapZoneService.findDevicesInZone(1L);

        assertThat(result.features()).hasSize(1);
        assertThat(result.features().get(0).properties().get("deviceCode")).isEqualTo("SL-001");
    }

    @Test
    void findDevicesInZone_emptyResult() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        GeoJsonResponse result = mapZoneService.findDevicesInZone(999L);

        assertThat(result.features()).isEmpty();
    }
}
