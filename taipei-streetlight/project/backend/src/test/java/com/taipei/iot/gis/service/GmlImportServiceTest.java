package com.taipei.iot.gis.service;

import com.taipei.iot.gis.dto.GmlImportDiff;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmlImportServiceTest {

    @Mock private EntityManager em;
    @Mock private Query query;
    @Mock private CoordinateService coordinateService;

    @InjectMocks private GmlImportService service;

    @BeforeEach
    void setUp() { TenantContext.setCurrentTenantId("TAIPEI"); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    private static final String SAMPLE_GML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gml:FeatureCollection xmlns:gml="http://www.opengis.net/gml/3.2"
                                   xmlns:sl="http://taipei.gov.tw/streetlight">
              <gml:featureMember>
                <sl:Device gml:id="device_1">
                  <sl:geometry>
                    <gml:Point srsName="EPSG:3826" gml:id="pt_1">
                      <gml:pos>305500.123 2770100.456</gml:pos>
                    </gml:Point>
                  </sl:geometry>
                  <sl:deviceCode>SL-NEW-001</sl:deviceCode>
                  <sl:deviceName>新路燈001</sl:deviceName>
                  <sl:deviceType>STREETLIGHT</sl:deviceType>
                  <sl:status>ACTIVE</sl:status>
                </sl:Device>
              </gml:featureMember>
              <gml:featureMember>
                <sl:Device gml:id="device_2">
                  <sl:geometry>
                    <gml:Point srsName="EPSG:3826" gml:id="pt_2">
                      <gml:pos>305600.789 2770200.123</gml:pos>
                    </gml:Point>
                  </sl:geometry>
                  <sl:deviceCode>SL-EXIST</sl:deviceCode>
                  <sl:deviceName>已有路燈-改名</sl:deviceName>
                  <sl:deviceType>STREETLIGHT</sl:deviceType>
                  <sl:status>ACTIVE</sl:status>
                </sl:Device>
              </gml:featureMember>
            </gml:FeatureCollection>
            """;

    @Test
    void parseGml_extractsFeatures() {
        var rows = service.parseGml(toStream(SAMPLE_GML));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).deviceCode()).isEqualTo("SL-NEW-001");
        assertThat(rows.get(0).deviceName()).isEqualTo("新路燈001");
        assertThat(rows.get(0).twd97X()).isNotNull();
        assertThat(rows.get(0).twd97Y()).isNotNull();
    }

    @Test
    void parseGml_invalidXml_throwsException() {
        assertThatThrownBy(() -> service.parseGml(toStream("<invalid>")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid GML");
    }

    @Test
    void parseAndDiff_newDevice_addedToList() {
        mockExistingDevices(); // DB has SL-EXIST only

        GmlImportDiff diff = service.parseAndDiff(toStream(SAMPLE_GML));

        assertThat(diff.totalParsed()).isEqualTo(2);
        assertThat(diff.toAdd()).hasSize(1);
        assertThat(diff.toAdd().get(0).deviceCode()).isEqualTo("SL-NEW-001");
    }

    @Test
    void parseAndDiff_existingDeviceChanged_markedForUpdate() {
        mockExistingDevices(); // DB has SL-EXIST with name "已有路燈"

        GmlImportDiff diff = service.parseAndDiff(toStream(SAMPLE_GML));

        assertThat(diff.toUpdate()).hasSize(1);
        assertThat(diff.toUpdate().get(0).deviceCode()).isEqualTo("SL-EXIST");
        assertThat(diff.toUpdate().get(0).diffFields()).contains("deviceName");
    }

    @Test
    void parseAndDiff_deviceInDbNotInGml_markedForDelete() {
        // DB has SL-EXIST and SL-OLD, GML only has SL-NEW-001 and SL-EXIST
        Object[] row1 = {10L, "已有路燈", "STREETLIGHT", "ACTIVE", "SL-EXIST"};
        Object[] row2 = {20L, "舊路燈", "STREETLIGHT", "ACTIVE", "SL-OLD"};
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(row1, row2));

        GmlImportDiff diff = service.parseAndDiff(toStream(SAMPLE_GML));

        assertThat(diff.toDelete()).hasSize(1);
        assertThat(diff.toDelete().get(0).deviceCode()).isEqualTo("SL-OLD");
    }

    @Test
    void parseAndDiff_emptyGml_allExistingMarkedForDelete() {
        String emptyGml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gml:FeatureCollection xmlns:gml="http://www.opengis.net/gml/3.2">
                </gml:FeatureCollection>
                """;
        mockExistingDevices();

        GmlImportDiff diff = service.parseAndDiff(toStream(emptyGml));

        assertThat(diff.totalParsed()).isZero();
        assertThat(diff.toAdd()).isEmpty();
        assertThat(diff.toDelete()).hasSize(1);
    }

    private void mockExistingDevices() {
        Object[] existingRow = {10L, "已有路燈", "STREETLIGHT", "ACTIVE", "SL-EXIST"};
        List<Object[]> results = new ArrayList<>();
        results.add(existingRow);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);
    }

    private ByteArrayInputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
