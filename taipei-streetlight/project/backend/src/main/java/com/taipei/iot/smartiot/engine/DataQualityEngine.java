package com.taipei.iot.smartiot.engine;

import com.taipei.iot.smartiot.entity.TelemetryFormat;
import com.taipei.iot.smartiot.enums.QualityFlag;
import com.taipei.iot.smartiot.repository.TelemetryFormatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 資料品質引擎 (FN-07-007)。
 * 依 format 的 field_definitions 檢查 payload 值範圍。
 * D9 決策: 有 min/max 就檢查，沒有就跳過（容錯）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataQualityEngine {

    private final TelemetryFormatRepository formatRepository;

    /**
     * 檢查 payload 品質。
     *
     * @param formatId 格式定義 ID (可為 null → 跳過檢查)
     * @param payload  遙測資料 JSONB
     * @return QualityFlag
     */
    public QualityFlag check(Long formatId, Map<String, Object> payload) {
        if (formatId == null) {
            return QualityFlag.OK;
        }

        Optional<TelemetryFormat> formatOpt = formatRepository.findById(formatId);
        if (formatOpt.isEmpty()) {
            log.warn("[DataQuality] Format {} not found, skipping check", formatId);
            return QualityFlag.OK;
        }

        TelemetryFormat format = formatOpt.get();
        List<Map<String, Object>> fieldDefs = format.getFieldDefinitions();
        if (fieldDefs == null || fieldDefs.isEmpty()) {
            return QualityFlag.OK;
        }

        boolean hasMissing = false;
        boolean hasSuspect = false;

        for (Map<String, Object> fieldDef : fieldDefs) {
            String fieldName = (String) fieldDef.get("name");
            String fieldType = (String) fieldDef.get("type");
            if (fieldName == null) continue;

            Object value = payload.get(fieldName);

            // 缺失偵測: NUMBER 欄位必須有值
            if (value == null && "NUMBER".equals(fieldType)) {
                log.debug("[DataQuality] Missing field: {} in payload", fieldName);
                hasMissing = true;
                continue;
            }

            // 值範圍檢查 (D9: 有 min/max 就檢查，沒有就跳過)
            if (value != null && "NUMBER".equals(fieldType)) {
                double numericValue;
                try {
                    numericValue = ((Number) value).doubleValue();
                } catch (ClassCastException e) {
                    log.debug("[DataQuality] Field {} is not numeric: {}", fieldName, value);
                    hasSuspect = true;
                    continue;
                }

                Object minObj = fieldDef.get("min");
                Object maxObj = fieldDef.get("max");

                if (minObj != null) {
                    double min = ((Number) minObj).doubleValue();
                    if (numericValue < min) {
                        log.debug("[DataQuality] Field {} value {} below min {}", fieldName, numericValue, min);
                        hasSuspect = true;
                    }
                }
                if (maxObj != null) {
                    double max = ((Number) maxObj).doubleValue();
                    if (numericValue > max) {
                        log.debug("[DataQuality] Field {} value {} above max {}", fieldName, numericValue, max);
                        hasSuspect = true;
                    }
                }
            }
        }

        if (hasMissing) return QualityFlag.MISSING;
        if (hasSuspect) return QualityFlag.SUSPECT;
        return QualityFlag.OK;
    }
}
