package com.taipei.iot.gis.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * GeoJSON FeatureCollection for device points and zone polygons.
 */
public record GeoJsonResponse(
        String type,
        List<Feature> features
) {
    public static GeoJsonResponse of(List<Feature> features) {
        return new GeoJsonResponse("FeatureCollection", features);
    }

    public record Feature(
            String type,
            Object geometry,
            Map<String, Object> properties
    ) {
        public static Feature of(Geometry geometry, Map<String, Object> properties) {
            return new Feature("Feature", geometry, properties);
        }

        /** Create a Feature with raw GeoJSON geometry string (from ST_AsGeoJSON) */
        public static Feature ofRawGeometry(String geoJson, Map<String, Object> properties) {
            return new Feature("Feature", new RawJson(geoJson), properties);
        }
    }

    public record Geometry(
            String type,
            double[] coordinates
    ) {
        public static Geometry point(BigDecimal lng, BigDecimal lat) {
            return new Geometry("Point", new double[]{lng.doubleValue(), lat.doubleValue()});
        }
    }

    /** Wrapper to serialize raw JSON string without escaping */
    public record RawJson(@JsonValue @JsonRawValue String value) {}
}
