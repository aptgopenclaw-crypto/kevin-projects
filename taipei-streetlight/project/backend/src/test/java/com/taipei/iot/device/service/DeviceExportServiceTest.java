package com.taipei.iot.device.service;

import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceExportServiceTest {

    @InjectMocks private DeviceExportService exportService;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DataScopeHelper dataScopeHelper;

    private List<Device> sampleDevices() {
        Device d1 = Device.builder().id(1L).deviceType(DeviceType.POLE)
                .deviceCode("SL-001").deviceName("路燈1")
                .status(DeviceStatus.ACTIVE).deptId(6L)
                .attributes(Map.of("height", 8, "material", "鋼"))
                .build();
        Device d2 = Device.builder().id(2L).deviceType(DeviceType.LUMINAIRE)
                .deviceCode("LM-001").status(DeviceStatus.ACTIVE)
                .attributes(Map.of("wattage", 150, "brand", "Philips"))
                .build();
        return List.of(d1, d2);
    }

    @Test
    void exportCsv_containsHeadersAndData() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportCsv(sampleDevices(), out);
        String csv = out.toString("UTF-8");

        assertTrue(csv.contains("設備類型"));
        assertTrue(csv.contains("設備編號"));
        assertTrue(csv.contains("SL-001"));
        assertTrue(csv.contains("LM-001"));
        // JSONB attrs expanded as columns
        assertTrue(csv.contains("attr:height") || csv.contains("attr:brand"));
    }

    @Test
    void exportCsv_emptyList_headerOnly() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportCsv(List.of(), out);
        String csv = out.toString("UTF-8");

        assertTrue(csv.contains("ID"));
        // Only header row (+ BOM)
        long lineCount = csv.lines().count();
        assertEquals(1, lineCount);
    }

    @Test
    void exportXlsx_doesNotThrow() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> exportService.exportXlsx(sampleDevices(), out));
        assertTrue(out.size() > 0);
    }

    @Test
    void exportOds_doesNotThrow() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> exportService.exportOds(sampleDevices(), out));
        assertTrue(out.size() > 0);
    }

    @Test
    void exportCsv_escapesCommasAndQuotes() throws Exception {
        Device d = Device.builder().id(1L).deviceType(DeviceType.POLE)
                .deviceCode("SL-001").deviceName("路燈,含逗號\"引號")
                .status(DeviceStatus.ACTIVE).build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportCsv(List.of(d), out);
        String csv = out.toString("UTF-8");

        // Escaped value should be quoted
        assertTrue(csv.contains("\"路燈,含逗號\"\"引號\""));
    }

    @Test
    void exportCsv_jsonbAttributeKeysAreSorted() throws Exception {
        Device d = Device.builder().id(1L).deviceType(DeviceType.POLE)
                .deviceCode("SL-001").status(DeviceStatus.ACTIVE)
                .attributes(Map.of("zzz", 1, "aaa", 2, "mmm", 3))
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportCsv(List.of(d), out);
        String csv = out.toString("UTF-8");

        int posA = csv.indexOf("attr:aaa");
        int posM = csv.indexOf("attr:mmm");
        int posZ = csv.indexOf("attr:zzz");
        assertTrue(posA < posM && posM < posZ, "Attribute keys should be sorted");
    }
}
