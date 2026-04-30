package com.taipei.iot.gis.service;

import com.taipei.iot.gis.dto.GmlImportDiff;
import com.taipei.iot.gis.dto.GmlImportDiff.ImportRow;
import com.taipei.iot.gis.service.CoordinateService.CoordinateSet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import static com.taipei.iot.tenant.TenantContext.getCurrentTenantId;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmlImportService {

    private final EntityManager em;
    private final CoordinateService coordinateService;

    /**
     * Parse GML file and compare with existing DB records.
     * Returns diff result (add/update/delete).
     */
    @Transactional(readOnly = true)
    public GmlImportDiff parseAndDiff(InputStream gmlInput) {
        List<ImportRow> parsed = parseGml(gmlInput);
        return compareDiff(parsed);
    }

    /**
     * Apply confirmed import — insert new, update changed, skip deletes (manual).
     */
    @Transactional
    public int applyImport(GmlImportDiff diff) {
        int count = 0;

        for (ImportRow row : diff.toAdd()) {
            // Auto-fill coordinates
            CoordinateSet cs = coordinateService.autoFill(
                    row.lng(), row.lat(), row.twd97X(), row.twd97Y(), null, null);

            String sql = """
                    INSERT INTO taipei_streetlight.devices
                    (tenant_id, device_code, device_name, device_type, status,
                     lng, lat, twd97_x, twd97_y, created_at)
                    VALUES (:tenantId, :code, :name, :type, :status,
                            :lng, :lat, :twd97X, :twd97Y, NOW())
                    """;
            em.createNativeQuery(sql)
                    .setParameter("tenantId", getCurrentTenantId())
                    .setParameter("code", row.deviceCode())
                    .setParameter("name", row.deviceName())
                    .setParameter("type", row.deviceType() != null ? row.deviceType() : "STREETLIGHT")
                    .setParameter("status", row.status() != null ? row.status() : "ACTIVE")
                    .setParameter("lng", cs.lng())
                    .setParameter("lat", cs.lat())
                    .setParameter("twd97X", cs.twd97X())
                    .setParameter("twd97Y", cs.twd97Y())
                    .executeUpdate();
            count++;
        }

        for (ImportRow row : diff.toUpdate()) {
            if (row.existingId() == null) continue;

            CoordinateSet cs = coordinateService.autoFill(
                    row.lng(), row.lat(), row.twd97X(), row.twd97Y(), null, null);

            String sql = """
                    UPDATE taipei_streetlight.devices SET
                        device_name = :name,
                        device_type = :type,
                        status = :status,
                        lng = :lng, lat = :lat,
                        twd97_x = :twd97X, twd97_y = :twd97Y,
                        updated_at = NOW()
                    WHERE id = :id AND tenant_id = :tenantId
                    """;
            em.createNativeQuery(sql)
                    .setParameter("id", row.existingId())
                    .setParameter("tenantId", getCurrentTenantId())
                    .setParameter("name", row.deviceName())
                    .setParameter("type", row.deviceType())
                    .setParameter("status", row.status())
                    .setParameter("lng", cs.lng())
                    .setParameter("lat", cs.lat())
                    .setParameter("twd97X", cs.twd97X())
                    .setParameter("twd97Y", cs.twd97Y())
                    .executeUpdate();
            count++;
        }

        return count;
    }

    // ── GML Parsing ──

    List<ImportRow> parseGml(InputStream input) {
        List<ImportRow> rows = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Security: disable external entities
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(input));

            NodeList members = doc.getElementsByTagNameNS("*", "featureMember");
            if (members.getLength() == 0) {
                members = doc.getElementsByTagNameNS("*", "member");
            }

            for (int i = 0; i < members.getLength(); i++) {
                Element member = (Element) members.item(i);
                ImportRow row = parseFeatureMember(member);
                if (row != null) rows.add(row);
            }
        } catch (Exception e) {
            log.error("Failed to parse GML", e);
            throw new IllegalArgumentException("Invalid GML file: " + e.getMessage());
        }
        return rows;
    }

    private ImportRow parseFeatureMember(Element member) {
        String deviceCode = getElementText(member, "deviceCode");
        if (deviceCode == null || deviceCode.isBlank()) return null;

        String deviceName = getElementText(member, "deviceName");
        String deviceType = getElementText(member, "deviceType");
        String status = getElementText(member, "status");

        // Parse coordinates from gml:pos
        BigDecimal twd97X = null, twd97Y = null;
        BigDecimal lng = null, lat = null;

        NodeList posList = member.getElementsByTagNameNS("*", "pos");
        if (posList.getLength() > 0) {
            String posText = posList.item(0).getTextContent().trim();
            String[] parts = posText.split("\\s+");
            if (parts.length >= 2) {
                try {
                    BigDecimal x = new BigDecimal(parts[0]);
                    BigDecimal y = new BigDecimal(parts[1]);

                    // Determine if coordinates are TWD97 or WGS84
                    // TWD97 X values are typically > 200000
                    if (x.doubleValue() > 1000) {
                        twd97X = x;
                        twd97Y = y;
                    } else {
                        lng = x;
                        lat = y;
                    }
                } catch (NumberFormatException ignored) {
                    // skip invalid coordinates
                }
            }
        }

        // Also check for explicit lng/lat elements
        String lngStr = getElementText(member, "lng");
        String latStr = getElementText(member, "lat");
        if (lngStr != null && latStr != null) {
            try {
                lng = new BigDecimal(lngStr);
                lat = new BigDecimal(latStr);
            } catch (NumberFormatException ignored) {}
        }

        return new ImportRow(null, deviceCode, deviceName, deviceType, status,
                twd97X, twd97Y, lng, lat, null);
    }

    private String getElementText(Element parent, String localName) {
        // Try namespace-aware first, then without namespace
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName("sl:" + localName);
        }
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    // ── Diff Comparison ──

    private GmlImportDiff compareDiff(List<ImportRow> parsed) {
        // Load existing devices by deviceCode for the tenant
        Map<String, Object[]> existing = loadExistingDevicesByCode();

        List<ImportRow> toAdd = new ArrayList<>();
        List<ImportRow> toUpdate = new ArrayList<>();
        Set<String> parsedCodes = new HashSet<>();

        for (ImportRow row : parsed) {
            parsedCodes.add(row.deviceCode());
            Object[] existingRow = existing.get(row.deviceCode());

            if (existingRow == null) {
                toAdd.add(row);
            } else {
                // Compare fields
                List<String> changedFields = new ArrayList<>();
                Long existingId = ((Number) existingRow[0]).longValue();
                String existName = (String) existingRow[1];
                String existType = (String) existingRow[2];
                String existStatus = (String) existingRow[3];

                if (row.deviceName() != null && !row.deviceName().equals(existName)) {
                    changedFields.add("deviceName");
                }
                if (row.deviceType() != null && !row.deviceType().equals(existType)) {
                    changedFields.add("deviceType");
                }
                if (row.status() != null && !row.status().equals(existStatus)) {
                    changedFields.add("status");
                }
                if (row.twd97X() != null || row.lng() != null) {
                    changedFields.add("coordinates");
                }

                if (!changedFields.isEmpty()) {
                    toUpdate.add(new ImportRow(
                            existingId, row.deviceCode(), row.deviceName(),
                            row.deviceType(), row.status(),
                            row.twd97X(), row.twd97Y(), row.lng(), row.lat(),
                            String.join(",", changedFields)));
                }
            }
        }

        // Devices in DB but not in GML → candidates for deletion
        List<ImportRow> toDelete = new ArrayList<>();
        for (Map.Entry<String, Object[]> entry : existing.entrySet()) {
            if (!parsedCodes.contains(entry.getKey())) {
                Object[] row = entry.getValue();
                toDelete.add(new ImportRow(
                        ((Number) row[0]).longValue(), entry.getKey(),
                        (String) row[1], (String) row[2], (String) row[3],
                        null, null, null, null, null));
            }
        }

        return new GmlImportDiff(toAdd, toUpdate, toDelete, parsed.size());
    }

    private Map<String, Object[]> loadExistingDevicesByCode() {
        String sql = """
                SELECT d.id, d.device_name, d.device_type, d.status, d.device_code
                FROM taipei_streetlight.devices d
                WHERE d.tenant_id = :tenantId
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("tenantId", getCurrentTenantId())
                .getResultList();

        Map<String, Object[]> map = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            String code = (String) row[4];
            if (code != null) map.put(code, row);
        }
        return map;
    }
}
