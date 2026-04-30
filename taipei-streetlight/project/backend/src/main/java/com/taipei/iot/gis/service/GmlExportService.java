package com.taipei.iot.gis.service;

import com.taipei.iot.gis.dto.GmlExportRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.taipei.iot.tenant.TenantContext.getCurrentTenantId;

@Service
@RequiredArgsConstructor
public class GmlExportService {

    private final EntityManager em;

    /**
     * Export devices as OGC GML 3.2 XML string.
     * Coordinates in EPSG:3826 (TWD97) — government standard.
     */
    @Transactional(readOnly = true)
    public String exportAsGml(String deviceType, String district) {
        List<GmlExportRow> rows = queryDevices(deviceType, district);
        return buildGml(rows);
    }

    /**
     * Export devices as CSV in Taipei Open Data Platform format.
     */
    @Transactional(readOnly = true)
    public String exportAsOpenDataCsv(String deviceType) {
        List<GmlExportRow> rows = queryDevices(deviceType, null);
        return buildCsv(rows);
    }

    private List<GmlExportRow> queryDevices(String deviceType, String district) {
        String sql = """
                SELECT d.id, d.device_code, d.device_name, d.device_type,
                       d.status, d.lng, d.lat,
                       d.twd97_x, d.twd97_y,
                       d.address, d.installed_at
                FROM taipei_streetlight.devices d
                WHERE d.tenant_id = :tenantId
                  AND d.lng IS NOT NULL AND d.lat IS NOT NULL
                """;

        if (deviceType != null && !deviceType.isBlank()) {
            sql += " AND d.device_type = :deviceType";
        }
        if (district != null && !district.isBlank()) {
            sql += " AND d.address LIKE :district";
        }
        sql += " ORDER BY d.device_code";

        Query query = em.createNativeQuery(sql)
                .setParameter("tenantId", getCurrentTenantId());

        if (deviceType != null && !deviceType.isBlank()) {
            query.setParameter("deviceType", deviceType);
        }
        if (district != null && !district.isBlank()) {
            query.setParameter("district", "%" + district + "%");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<GmlExportRow> rows = new ArrayList<>(results.size());
        for (Object[] r : results) {
            rows.add(new GmlExportRow(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    (String) r[4],
                    toBigDecimal(r[5]),
                    toBigDecimal(r[6]),
                    toBigDecimal(r[7]),
                    toBigDecimal(r[8]),
                    (String) r[9],
                    r[10] != null ? r[10].toString() : null
            ));
        }
        return rows;
    }

    private String buildGml(List<GmlExportRow> rows) {
        StringBuilder sb = new StringBuilder(rows.size() * 500 + 500);

        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gml:FeatureCollection
                    xmlns:gml="http://www.opengis.net/gml/3.2"
                    xmlns:sl="http://taipei.gov.tw/streetlight"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd"
                    gml:id="fc_streetlight_export"
                    timeStamp="%s">
                """.formatted(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        for (GmlExportRow row : rows) {
            sb.append("  <gml:featureMember>\n");
            sb.append("    <sl:Device gml:id=\"device_").append(row.id()).append("\">\n");

            // Geometry — TWD97 coordinates (EPSG:3826)
            if (row.twd97X() != null && row.twd97Y() != null) {
                sb.append("      <sl:geometry>\n");
                sb.append("        <gml:Point srsName=\"EPSG:3826\" gml:id=\"pt_")
                        .append(row.id()).append("\">\n");
                sb.append("          <gml:pos>")
                        .append(row.twd97X().toPlainString()).append(" ")
                        .append(row.twd97Y().toPlainString())
                        .append("</gml:pos>\n");
                sb.append("        </gml:Point>\n");
                sb.append("      </sl:geometry>\n");
            }

            appendElement(sb, "sl:deviceCode", row.deviceCode());
            appendElement(sb, "sl:deviceName", row.deviceName());
            appendElement(sb, "sl:deviceType", row.deviceType());
            appendElement(sb, "sl:status", row.status());
            appendElement(sb, "sl:lng", row.lng() != null ? row.lng().toPlainString() : null);
            appendElement(sb, "sl:lat", row.lat() != null ? row.lat().toPlainString() : null);
            appendElement(sb, "sl:address", row.address());
            appendElement(sb, "sl:installedAt", row.installedAt());

            sb.append("    </sl:Device>\n");
            sb.append("  </gml:featureMember>\n");
        }

        sb.append("</gml:FeatureCollection>\n");
        return sb.toString();
    }

    private void appendElement(StringBuilder sb, String tag, String value) {
        if (value == null) return;
        sb.append("      <").append(tag).append(">")
                .append(escapeXml(value))
                .append("</").append(tag).append(">\n");
    }

    static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String buildCsv(List<GmlExportRow> rows) {
        StringBuilder sb = new StringBuilder(rows.size() * 200 + 100);
        // Header — Taipei Open Data Platform format
        sb.append("編號,設備編碼,設備名稱,設備類型,狀態,經度,緯度,TWD97_X,TWD97_Y,地址,安裝日期\n");

        for (GmlExportRow row : rows) {
            sb.append(row.id()).append(",");
            sb.append(csvField(row.deviceCode())).append(",");
            sb.append(csvField(row.deviceName())).append(",");
            sb.append(csvField(row.deviceType())).append(",");
            sb.append(csvField(row.status())).append(",");
            sb.append(row.lng() != null ? row.lng().toPlainString() : "").append(",");
            sb.append(row.lat() != null ? row.lat().toPlainString() : "").append(",");
            sb.append(row.twd97X() != null ? row.twd97X().toPlainString() : "").append(",");
            sb.append(row.twd97Y() != null ? row.twd97Y().toPlainString() : "").append(",");
            sb.append(csvField(row.address())).append(",");
            sb.append(row.installedAt() != null ? row.installedAt() : "");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String csvField(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }
}
