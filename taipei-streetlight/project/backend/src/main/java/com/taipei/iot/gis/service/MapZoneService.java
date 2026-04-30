package com.taipei.iot.gis.service;

import com.taipei.iot.gis.dto.GeoJsonResponse;
import com.taipei.iot.gis.dto.GeoJsonResponse.Feature;
import com.taipei.iot.gis.dto.GeoJsonResponse.Geometry;
import com.taipei.iot.gis.entity.ZoneType;
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

import static com.taipei.iot.tenant.TenantContext.getCurrentTenantId;

@Service
@RequiredArgsConstructor
public class MapZoneService {

    private final EntityManager em;

    /**
     * 取得指定類型的所有分區，回傳 GeoJSON FeatureCollection (Polygon)
     */
    @Transactional(readOnly = true)
    public GeoJsonResponse getZonesByType(ZoneType zoneType) {
        String sql = """
                SELECT z.id, z.zone_code, z.zone_name, z.zone_type, z.properties,
                       ST_AsGeoJSON(z.geom) AS geojson_geom,
                       (SELECT COUNT(*) FROM taipei_streetlight.devices d
                        WHERE d.tenant_id = z.tenant_id
                          AND d.geom IS NOT NULL
                          AND ST_Contains(z.geom, d.geom)) AS device_count
                FROM taipei_streetlight.map_zones z
                WHERE z.tenant_id = :tenantId
                  AND z.zone_type = :zoneType
                ORDER BY z.zone_name
                """;

        Query query = em.createNativeQuery(sql)
                .setParameter("tenantId", getCurrentTenantId())
                .setParameter("zoneType", zoneType.name());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return GeoJsonResponse.of(toZoneFeatures(rows));
    }

    /**
     * 取得指定分區內的設備列表
     */
    @Transactional(readOnly = true)
    public GeoJsonResponse findDevicesInZone(Long zoneId) {
        String sql = """
                SELECT d.id, d.device_code, d.device_name, d.device_type,
                       d.status, d.lng, d.lat, d.dept_id
                FROM taipei_streetlight.devices d
                WHERE d.tenant_id = :tenantId
                  AND d.geom IS NOT NULL
                  AND ST_Contains(
                        (SELECT z.geom FROM taipei_streetlight.map_zones z
                         WHERE z.id = :zoneId AND z.tenant_id = :tenantId),
                        d.geom)
                """;

        Query query = em.createNativeQuery(sql)
                .setParameter("tenantId", getCurrentTenantId())
                .setParameter("zoneId", zoneId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return GeoJsonResponse.of(toDeviceFeatures(rows));
    }

    private List<Feature> toZoneFeatures(List<Object[]> rows) {
        List<Feature> features = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String geojsonGeom = (String) row[5];

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("id", ((Number) row[0]).longValue());
            props.put("zoneCode", row[1]);
            props.put("zoneName", row[2]);
            props.put("zoneType", row[3]);
            props.put("properties", row[4]);
            props.put("deviceCount", ((Number) row[6]).longValue());

            // Parse PostGIS ST_AsGeoJSON output as raw geometry
            features.add(Feature.ofRawGeometry(geojsonGeom, props));
        }
        return features;
    }

    private List<Feature> toDeviceFeatures(List<Object[]> rows) {
        List<Feature> features = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            BigDecimal lng = toBigDecimal(row[5]);
            BigDecimal lat = toBigDecimal(row[6]);
            if (lng == null || lat == null) return features;

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

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }
}
