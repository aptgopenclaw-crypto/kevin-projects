package com.taipei.iot.gis.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * GML 匯入差異比對結果
 */
public record GmlImportDiff(
        List<ImportRow> toAdd,
        List<ImportRow> toUpdate,
        List<ImportRow> toDelete,
        int totalParsed
) {
    public record ImportRow(
            Long existingId,        // null for new records
            String deviceCode,
            String deviceName,
            String deviceType,
            String status,
            BigDecimal twd97X,
            BigDecimal twd97Y,
            BigDecimal lng,
            BigDecimal lat,
            String diffFields       // comma-separated changed field names (for updates)
    ) {}
}
