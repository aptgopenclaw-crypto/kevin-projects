package com.taipei.iot.gis.dto;

import java.math.BigDecimal;

public record BoundsRequest(
        BigDecimal minLng,
        BigDecimal minLat,
        BigDecimal maxLng,
        BigDecimal maxLat
) {}
