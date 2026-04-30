package com.taipei.iot.gis.dto;

import java.math.BigDecimal;

public record GmlExportRow(
        long id,
        String deviceCode,
        String deviceName,
        String deviceType,
        String status,
        BigDecimal lng,
        BigDecimal lat,
        BigDecimal twd97X,
        BigDecimal twd97Y,
        String address,
        String installedAt
) {}
