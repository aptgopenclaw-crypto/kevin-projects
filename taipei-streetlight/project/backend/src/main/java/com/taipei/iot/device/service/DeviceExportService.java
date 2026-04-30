package com.taipei.iot.device.service;

import com.taipei.iot.dept.annotation.DataPermission;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceExportService {

    private final DeviceRepository deviceRepository;
    private final DataScopeHelper dataScopeHelper;

    // Fixed columns (order matters)
    private static final String[] FIXED_HEADERS = {
            "ID", "設備類型", "設備編號", "設備名稱",
            "TWD97_X", "TWD97_Y", "經度", "緯度", "海拔", "台電座標",
            "部門", "財產所有人", "狀態",
            "安裝日期", "除役日期",
            "父設備ID", "掛載位置", "連線方式",
            "迴路ID", "契約ID",
            "建立者", "建立時間", "更新時間",
    };

    /**
     * Query devices with current user's DataScope applied.
     */
    @DataPermission
    public List<Device> queryForExport(DeviceType deviceType, DeviceStatus status, String keyword) {
        List<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();
        return deviceRepository.findAllByFilters(
                deviceType, status, keyword,
                visibleDeptIds.isEmpty() ? null : visibleDeptIds);
    }

    // ─── CSV ────────────────────────────────────────────

    public void exportCsv(List<Device> devices, OutputStream out) throws IOException {
        Set<String> attrKeys = collectAttributeKeys(devices);
        List<String> headers = buildHeaders(attrKeys);

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // BOM for Excel UTF-8 recognition
            pw.write('\uFEFF');
            pw.println(headers.stream().map(this::csvEscape).collect(Collectors.joining(",")));
            for (Device d : devices) {
                List<String> row = buildRow(d, attrKeys);
                pw.println(row.stream().map(this::csvEscape).collect(Collectors.joining(",")));
            }
        }
    }

    // ─── XLSX ───────────────────────────────────────────

    public void exportXlsx(List<Device> devices, OutputStream out) throws IOException {
        Set<String> attrKeys = collectAttributeKeys(devices);
        List<String> headers = buildHeaders(attrKeys);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("devices");

            // Header row
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int r = 0; r < devices.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> values = buildRow(devices.get(r), attrKeys);
                for (int c = 0; c < values.size(); c++) {
                    row.createCell(c).setCellValue(values.get(c));
                }
            }

            // Auto-size first N columns (skip for large data)
            int autoSizeCols = Math.min(headers.size(), 30);
            for (int i = 0; i < autoSizeCols; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
        }
    }

    // ─── ODS (via ODFDOM) ───────────────────────────────

    public void exportOds(List<Device> devices, OutputStream out) throws Exception {
        Set<String> attrKeys = collectAttributeKeys(devices);
        List<String> headers = buildHeaders(attrKeys);

        org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument doc =
                org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument.newSpreadsheetDocument();
        org.odftoolkit.odfdom.doc.table.OdfTable table = doc.getTableList().get(0);
        table.setTableName("devices");

        // Header row
        for (int c = 0; c < headers.size(); c++) {
            table.getCellByPosition(c, 0).setStringValue(headers.get(c));
        }

        // Data rows
        for (int r = 0; r < devices.size(); r++) {
            List<String> values = buildRow(devices.get(r), attrKeys);
            for (int c = 0; c < values.size(); c++) {
                table.getCellByPosition(c, r + 1).setStringValue(values.get(c));
            }
        }

        doc.save(out);
        doc.close();
    }

    // ─── Helpers ────────────────────────────────────────

    /**
     * Collect all unique JSONB attribute keys across devices (sorted).
     */
    private Set<String> collectAttributeKeys(List<Device> devices) {
        TreeSet<String> keys = new TreeSet<>();
        for (Device d : devices) {
            if (d.getAttributes() != null) {
                keys.addAll(d.getAttributes().keySet());
            }
        }
        return keys;
    }

    private List<String> buildHeaders(Set<String> attrKeys) {
        List<String> headers = new ArrayList<>(Arrays.asList(FIXED_HEADERS));
        for (String key : attrKeys) {
            headers.add("attr:" + key);
        }
        return headers;
    }

    private List<String> buildRow(Device d, Set<String> attrKeys) {
        List<String> row = new ArrayList<>();
        row.add(str(d.getId()));
        row.add(str(d.getDeviceType()));
        row.add(str(d.getDeviceCode()));
        row.add(str(d.getDeviceName()));
        row.add(str(d.getTwd97X()));
        row.add(str(d.getTwd97Y()));
        row.add(str(d.getLng()));
        row.add(str(d.getLat()));
        row.add(str(d.getElevation()));
        row.add(str(d.getTaipowerCoord()));
        row.add(str(d.getDeptId()));
        row.add(str(d.getPropertyOwner()));
        row.add(str(d.getStatus()));
        row.add(str(d.getInstalledAt()));
        row.add(str(d.getDecommissionedAt()));
        row.add(str(d.getParentDeviceId()));
        row.add(str(d.getMountPosition()));
        row.add(str(d.getConnectivityType()));
        row.add(str(d.getCircuitId()));
        row.add(str(d.getContractId()));
        row.add(str(d.getCreatedBy()));
        row.add(str(d.getCreatedAt()));
        row.add(str(d.getUpdatedAt()));

        // Expand JSONB attributes as extra columns
        Map<String, Object> attrs = d.getAttributes();
        for (String key : attrKeys) {
            row.add(attrs != null ? str(attrs.get(key)) : "");
        }
        return row;
    }

    private String str(Object val) {
        return val == null ? "" : val.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
