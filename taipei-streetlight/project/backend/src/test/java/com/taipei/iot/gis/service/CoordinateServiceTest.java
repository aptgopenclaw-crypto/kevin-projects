package com.taipei.iot.gis.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoordinateServiceTest {

    @InjectMocks private CoordinateService coordinateService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private void mockTransform(double expectedX, double expectedY) {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new Object[]{expectedX, expectedY});
    }

    @Test
    void transform_sameSrid_returnsOriginal() {
        double[] result = coordinateService.transform(121.5654, 25.0330, 4326, 4326);
        assertEquals(121.5654, result[0]);
        assertEquals(25.0330, result[1]);
        verifyNoInteractions(em);
    }

    @Test
    void transform_wgs84ToTwd97_callsPostGIS() {
        mockTransform(306700.0, 2770300.0);

        double[] result = coordinateService.transform(121.5654, 25.0330, 4326, 3826);

        assertEquals(306700.0, result[0]);
        assertEquals(2770300.0, result[1]);
        verify(em).createNativeQuery(contains("ST_Transform"));
    }

    @Test
    void autoFill_allNull_returnsAllNull() {
        var result = coordinateService.autoFill(null, null, null, null, null, null);

        assertNull(result.lng());
        assertNull(result.lat());
        assertNull(result.twd97X());
        assertNull(result.twd97Y());
        assertNull(result.twd67X());
        assertNull(result.twd67Y());
    }

    @Test
    void autoFill_fromWgs84_fillsTwd97AndTwd67() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(new Object[]{306700.0, 2770300.0})   // TWD97
                .thenReturn(new Object[]{306500.0, 2770100.0});  // TWD67

        var result = coordinateService.autoFill(
                new BigDecimal("121.5654000"), new BigDecimal("25.0330000"),
                null, null, null, null);

        assertEquals(new BigDecimal("121.5654000"), result.lng());
        assertEquals(new BigDecimal("25.0330000"), result.lat());
        assertEquals(0, result.twd97X().compareTo(new BigDecimal("306700.000")));
        assertEquals(0, result.twd97Y().compareTo(new BigDecimal("2770300.000")));
        assertEquals(0, result.twd67X().compareTo(new BigDecimal("306500.000")));
        assertEquals(0, result.twd67Y().compareTo(new BigDecimal("2770100.000")));
    }

    @Test
    void autoFill_fromTwd97_fillsWgs84AndTwd67() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(new Object[]{121.5654, 25.033})      // WGS84
                .thenReturn(new Object[]{306500.0, 2770100.0});  // TWD67

        var result = coordinateService.autoFill(
                null, null,
                new BigDecimal("306700.000"), new BigDecimal("2770300.000"),
                null, null);

        assertNotNull(result.lng());
        assertNotNull(result.lat());
        assertNotNull(result.twd67X());
        assertNotNull(result.twd67Y());
        assertEquals(new BigDecimal("306700.000"), result.twd97X());
    }

    @Test
    void autoFill_fromTwd67_fillsWgs84AndTwd97() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(new Object[]{121.5654, 25.033})      // WGS84
                .thenReturn(new Object[]{306700.0, 2770300.0});  // TWD97

        var result = coordinateService.autoFill(
                null, null, null, null,
                new BigDecimal("306500.000"), new BigDecimal("2770100.000"));

        assertNotNull(result.lng());
        assertNotNull(result.lat());
        assertNotNull(result.twd97X());
        assertNotNull(result.twd97Y());
    }

    @Test
    void autoFill_wgs84PlusTwd97_onlyFillsTwd67() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(new Object[]{306500.0, 2770100.0});  // TWD67

        var result = coordinateService.autoFill(
                new BigDecimal("121.5654000"), new BigDecimal("25.0330000"),
                new BigDecimal("306700.000"), new BigDecimal("2770300.000"),
                null, null);

        assertEquals(new BigDecimal("121.5654000"), result.lng());
        assertEquals(new BigDecimal("306700.000"), result.twd97X());
        assertNotNull(result.twd67X());
        assertNotNull(result.twd67Y());
    }

    @Test
    void autoFill_allProvided_noTransformCalled() {
        var result = coordinateService.autoFill(
                new BigDecimal("121.5654000"), new BigDecimal("25.0330000"),
                new BigDecimal("306700.000"), new BigDecimal("2770300.000"),
                new BigDecimal("306500.000"), new BigDecimal("2770100.000"));

        assertEquals(new BigDecimal("121.5654000"), result.lng());
        assertEquals(new BigDecimal("306700.000"), result.twd97X());
        assertEquals(new BigDecimal("306500.000"), result.twd67X());
        verifyNoInteractions(em);
    }
}
