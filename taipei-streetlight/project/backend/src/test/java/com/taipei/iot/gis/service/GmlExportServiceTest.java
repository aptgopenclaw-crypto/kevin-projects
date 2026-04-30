package com.taipei.iot.gis.service;

import com.taipei.iot.gis.dto.GmlExportRow;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmlExportServiceTest {

    @Mock private EntityManager em;
    @Mock private Query query;

    @InjectMocks private GmlExportService service;

    @BeforeEach
    void setUp() { TenantContext.setCurrentTenantId("TAIPEI"); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    private void mockQuery(Object[]... rows) {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        if (rows.length == 0) {
            when(query.getResultList()).thenReturn(List.of());
        } else {
            List<Object[]> results = new ArrayList<>();
            results.add(rows[0]);
            when(query.getResultList()).thenReturn(results);
        }
    }

    @Test
    void exportAsGml_containsXmlHeaders() {
        Object[] row = {1L, "SL-001", "路燈-001", "STREETLIGHT", "ACTIVE",
                new BigDecimal("121.52"), new BigDecimal("25.034"),
                new BigDecimal("305500.123"), new BigDecimal("2770100.456"),
                "信義區市府路1號", "2020-01-15"};
        mockQuery(row);

        String gml = service.exportAsGml(null, null);

        assertThat(gml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(gml).contains("gml:FeatureCollection");
        assertThat(gml).contains("xmlns:gml=\"http://www.opengis.net/gml/3.2\"");
        assertThat(gml).contains("gml:featureMember");
    }

    @Test
    void exportAsGml_containsDeviceData() {
        Object[] row = {1L, "SL-001", "路燈-001", "STREETLIGHT", "ACTIVE",
                new BigDecimal("121.52"), new BigDecimal("25.034"),
                new BigDecimal("305500.123"), new BigDecimal("2770100.456"),
                null, null};
        mockQuery(row);

        String gml = service.exportAsGml(null, null);

        assertThat(gml).contains("<sl:deviceCode>SL-001</sl:deviceCode>");
        assertThat(gml).contains("<sl:deviceName>路燈-001</sl:deviceName>");
        assertThat(gml).contains("<sl:deviceType>STREETLIGHT</sl:deviceType>");
        assertThat(gml).contains("srsName=\"EPSG:3826\"");
        assertThat(gml).contains("<gml:pos>305500.123 2770100.456</gml:pos>");
    }

    @Test
    void exportAsGml_emptyResult() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        String gml = service.exportAsGml(null, null);

        assertThat(gml).contains("gml:FeatureCollection");
        assertThat(gml).doesNotContain("gml:featureMember");
    }

    @Test
    void exportAsOpenDataCsv_containsHeaders() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        String csv = service.exportAsOpenDataCsv(null);

        assertThat(csv).startsWith("編號,設備編碼,設備名稱,設備類型,狀態,經度,緯度,TWD97_X,TWD97_Y,地址,安裝日期\n");
    }

    @Test
    void exportAsOpenDataCsv_containsData() {
        Object[] row = {1L, "SL-001", "路燈-001", "STREETLIGHT", "ACTIVE",
                new BigDecimal("121.52"), new BigDecimal("25.034"),
                new BigDecimal("305500.123"), new BigDecimal("2770100.456"),
                "信義區", "2020-01-15"};
        mockQuery(row);

        String csv = service.exportAsOpenDataCsv(null);

        assertThat(csv).contains("SL-001");
        assertThat(csv).contains("121.52");
        assertThat(csv).contains("305500.123");
    }

    @Test
    void escapeXml_handlesSpecialChars() {
        assertThat(GmlExportService.escapeXml("<test>&'\"")).isEqualTo("&lt;test&gt;&amp;&apos;&quot;");
        assertThat(GmlExportService.escapeXml(null)).isEmpty();
    }

    @Test
    void exportAsOpenDataCsv_escapesCommasInFields() {
        Object[] row = {2L, "SL-002", "路燈,特殊", "STREETLIGHT", "ACTIVE",
                new BigDecimal("121.53"), new BigDecimal("25.04"),
                null, null, null, null};
        mockQuery(row);

        String csv = service.exportAsOpenDataCsv(null);

        assertThat(csv).contains("\"路燈,特殊\"");
    }
}
