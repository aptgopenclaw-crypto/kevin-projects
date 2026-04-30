package com.taipei.iot.gis.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoordinateService {

    public static final int SRID_WGS84 = 4326;
    public static final int SRID_TWD97 = 3826;
    public static final int SRID_TWD67 = 3828;

    private final EntityManager em;

    /**
     * Transform a coordinate between SRIDs using PostGIS ST_Transform.
     */
    public double[] transform(double x, double y, int fromSrid, int toSrid) {
        if (fromSrid == toSrid) return new double[]{x, y};

        String sql = """
                SELECT ST_X(ST_Transform(ST_SetSRID(ST_MakePoint(:x, :y), :fromSrid), :toSrid)),
                       ST_Y(ST_Transform(ST_SetSRID(ST_MakePoint(:x, :y), :fromSrid), :toSrid))
                """;

        Object[] row = (Object[]) em.createNativeQuery(sql)
                .setParameter("x", x)
                .setParameter("y", y)
                .setParameter("fromSrid", fromSrid)
                .setParameter("toSrid", toSrid)
                .getSingleResult();

        return new double[]{((Number) row[0]).doubleValue(), ((Number) row[1]).doubleValue()};
    }

    /**
     * Auto-fill all three coordinate systems from whatever is available.
     * Priority: WGS84 > TWD97 > TWD67.
     */
    public CoordinateSet autoFill(BigDecimal lng, BigDecimal lat,
                                  BigDecimal twd97X, BigDecimal twd97Y,
                                  BigDecimal twd67X, BigDecimal twd67Y) {

        if (lng != null && lat != null) {
            if (twd97X == null || twd97Y == null) {
                double[] r = transform(lng.doubleValue(), lat.doubleValue(), SRID_WGS84, SRID_TWD97);
                twd97X = BigDecimal.valueOf(r[0]).setScale(3, RoundingMode.HALF_UP);
                twd97Y = BigDecimal.valueOf(r[1]).setScale(3, RoundingMode.HALF_UP);
            }
            if (twd67X == null || twd67Y == null) {
                double[] r = transform(lng.doubleValue(), lat.doubleValue(), SRID_WGS84, SRID_TWD67);
                twd67X = BigDecimal.valueOf(r[0]).setScale(3, RoundingMode.HALF_UP);
                twd67Y = BigDecimal.valueOf(r[1]).setScale(3, RoundingMode.HALF_UP);
            }
        } else if (twd97X != null && twd97Y != null) {
            double[] wgs = transform(twd97X.doubleValue(), twd97Y.doubleValue(), SRID_TWD97, SRID_WGS84);
            lng = BigDecimal.valueOf(wgs[0]).setScale(7, RoundingMode.HALF_UP);
            lat = BigDecimal.valueOf(wgs[1]).setScale(7, RoundingMode.HALF_UP);
            if (twd67X == null || twd67Y == null) {
                double[] r = transform(twd97X.doubleValue(), twd97Y.doubleValue(), SRID_TWD97, SRID_TWD67);
                twd67X = BigDecimal.valueOf(r[0]).setScale(3, RoundingMode.HALF_UP);
                twd67Y = BigDecimal.valueOf(r[1]).setScale(3, RoundingMode.HALF_UP);
            }
        } else if (twd67X != null && twd67Y != null) {
            double[] wgs = transform(twd67X.doubleValue(), twd67Y.doubleValue(), SRID_TWD67, SRID_WGS84);
            lng = BigDecimal.valueOf(wgs[0]).setScale(7, RoundingMode.HALF_UP);
            lat = BigDecimal.valueOf(wgs[1]).setScale(7, RoundingMode.HALF_UP);
            double[] r = transform(twd67X.doubleValue(), twd67Y.doubleValue(), SRID_TWD67, SRID_TWD97);
            twd97X = BigDecimal.valueOf(r[0]).setScale(3, RoundingMode.HALF_UP);
            twd97Y = BigDecimal.valueOf(r[1]).setScale(3, RoundingMode.HALF_UP);
        }

        return new CoordinateSet(lng, lat, twd97X, twd97Y, twd67X, twd67Y);
    }

    public record CoordinateSet(
            BigDecimal lng, BigDecimal lat,
            BigDecimal twd97X, BigDecimal twd97Y,
            BigDecimal twd67X, BigDecimal twd67Y
    ) {}
}
