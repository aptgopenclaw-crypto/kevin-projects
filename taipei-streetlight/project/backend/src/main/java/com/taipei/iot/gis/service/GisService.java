package com.taipei.iot.gis.service;

import com.taipei.iot.gis.dto.GeoJsonResponse;
import com.taipei.iot.gis.dto.GeoJsonResponse.Feature;
import com.taipei.iot.gis.dto.GeoJsonResponse.Geometry;
import com.taipei.iot.tenant.TenantContext;
import static com.taipei.iot.tenant.TenantContext.getCurrentTenantId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GisService {

    private final EntityManager em;

    /**
     * Get all devices within a bounding box as GeoJSON.
     */
    @Transactional(readOnly = true)
    public GeoJsonResponse findDevicesInBounds(
            BigDecimal minLng, BigDecimal minLat,
            BigDecimal maxLng, BigDecimal maxLat,
            String deviceType, Integer zoom) {

        // zoom < 14: minimal payload (id, deviceType, lng, lat only)
        boolean simplified = zoom != null && zoom < 14;

        String columns = simplified
                ? "d.id, d.device_type, d.lng, d.lat"
                : "d.id, d.device_code, d.device_name, d.device_type, d.status, d.lng, d.lat, d.dept_id";

        String sql = "SELECT " + columns + """
                 FROM taipei_streetlight.devices d
                WHERE d.tenant_id = :tenantId
                  AND d.geom IS NOT NULL
                  AND ST_Intersects(d.geom,
                        ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))
                """;

        if (deviceType != null && !deviceType.isBlank()) {
            sql += " AND d.device_type = :deviceType";
        }

        Query query = em.createNativeQuery(sql)
                .setParameter("tenantId", getCurrentTenantId())
                .setParameter("minLng", minLng.doubleValue())
                .setParameter("minLat", minLat.doubleValue())
                .setParameter("maxLng", maxLng.doubleValue())
                .setParameter("maxLat", maxLat.doubleValue());

        if (deviceType != null && !deviceType.isBlank()) {
            query.setParameter("deviceType", deviceType);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return GeoJsonResponse.of(simplified ? toSimplifiedFeatures(rows) : toFeatures(rows));
    }

    /**
     * Get all devices as GeoJSON (for initial full load / small datasets).
     */
    @Transactional(readOnly = true)
    public GeoJsonResponse findAllDevices(String deviceType) {

        String sql = """
                SELECT d.id, d.device_code, d.device_name, d.device_type,
                       d.status, d.lng, d.lat, d.dept_id
                FROM taipei_streetlight.devices d
                WHERE d.tenant_id = :tenantId
                  AND d.lng IS NOT NULL AND d.lat IS NOT NULL
                """;

        if (deviceType != null && !deviceType.isBlank()) {
            sql += " AND d.device_type = :deviceType";
        }

        Query query = em.createNativeQuery(sql)
                .setParameter("tenantId", getCurrentTenantId());

        if (deviceType != null && !deviceType.isBlank()) {
            query.setParameter("deviceType", deviceType);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return GeoJsonResponse.of(toFeatures(rows));
    }

    /**
     * Find devices within radius (meters) of a point.
     */
    @Transactional(readOnly = true)
    public GeoJsonResponse findDevicesNearby(BigDecimal lng, BigDecimal lat, double radiusMeters) {

        String sql = """
                SELECT d.id, d.device_code, d.device_name, d.device_type,
                       d.status, d.lng, d.lat, d.dept_id
                FROM taipei_streetlight.devices d
                WHERE d.tenant_id = :tenantId
                  AND d.geom IS NOT NULL
                  AND ST_DWithin(d.geom::geography,
                        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                        :radius)
                ORDER BY d.geom <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
                """;

        Query query = em.createNativeQuery(sql)
                .setParameter("tenantId", getCurrentTenantId())
                .setParameter("lng", lng.doubleValue())
                .setParameter("lat", lat.doubleValue())
                .setParameter("radius", radiusMeters);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return GeoJsonResponse.of(toFeatures(rows));
    }

    private List<Feature> toFeatures(List<Object[]> rows) {
        List<Feature> features = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            BigDecimal lng = toBigDecimal(row[5]);
            BigDecimal lat = toBigDecimal(row[6]);
            if (lng == null || lat == null) continue;

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("id", ((Number) row[0]).longValue());
            props.put("deviceCode", row[1]);
            props.put("deviceName", row[2]);
            props.put("deviceType", row[3]);
            props.put("status", row[4]);
            props.put("deptId", row[7] != null ? ((Number) row[7]).longValue() : null);

            features.add(Feature.of(Geometry.point(lng, lat), props));
        }
        return features;
    }

    private List<Feature> toSimplifiedFeatures(List<Object[]> rows) {
        List<Feature> features = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            // columns: id, device_type, lng, lat
            BigDecimal lng = toBigDecimal(row[2]);
            BigDecimal lat = toBigDecimal(row[3]);
            if (lng == null || lat == null) continue;

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("id", ((Number) row[0]).longValue());
            props.put("deviceType", row[1]);

            features.add(Feature.of(Geometry.point(lng, lat), props));
        }
        return features;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }
}
