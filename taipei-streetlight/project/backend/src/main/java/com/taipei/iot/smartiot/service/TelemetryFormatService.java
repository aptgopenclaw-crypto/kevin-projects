package com.taipei.iot.smartiot.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.smartiot.dto.TelemetryFormatFieldResponse;
import com.taipei.iot.smartiot.dto.TelemetryFormatRequest;
import com.taipei.iot.smartiot.dto.TelemetryFormatResponse;
import com.taipei.iot.smartiot.entity.TelemetryFormat;
import com.taipei.iot.smartiot.repository.EventRuleConditionRepository;
import com.taipei.iot.smartiot.repository.TelemetryFormatRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelemetryFormatService {

    private final TelemetryFormatRepository formatRepository;
    private final EventRuleConditionRepository conditionRepository;

    /**
     * 建立 Telemetry Format (FN-07-044)。
     * 若給 samplePayload 但沒給 fieldDefinitions，自動解析 JSON sample。
     */
    @Transactional
    public TelemetryFormatResponse create(TelemetryFormatRequest request) {
        List<Map<String, Object>> fields = request.getFieldDefinitions();
        if ((fields == null || fields.isEmpty()) && request.getSamplePayload() != null) {
            fields = parseJsonSample(request.getSamplePayload());
        }
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "必須提供 fieldDefinitions 或 samplePayload");
        }

        String tenantId = TenantContext.getCurrentTenantId();
        int version = request.getVersion() != null ? request.getVersion() : 1;

        formatRepository.findByTenantIdAndVendorNameAndDeviceModelAndVersion(
                tenantId, request.getVendorName(), request.getDeviceModel(), version)
                .ifPresent(f -> {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "相同廠商+型號+版本已存在");
                });

        TelemetryFormat format = TelemetryFormat.builder()
                .vendorName(request.getVendorName())
                .deviceModel(request.getDeviceModel())
                .version(version)
                .fieldDefinitions(fields)
                .samplePayload(request.getSamplePayload())
                .description(request.getDescription())
                .build();

        return toResponse(formatRepository.save(format));
    }

    /**
     * Format 清單 (FN-07-045) — 支援廠商篩選 + keyword 搜尋。
     */
    public Page<TelemetryFormatResponse> list(String vendorName, String keyword, Pageable pageable) {
        return formatRepository.findByFilters(vendorName, keyword, pageable)
                .map(this::toResponse);
    }

    /**
     * 更新 Format (FN-07-046)。
     * 新增欄位允許；刪除欄位需確認無 event_rule_conditions 引用。
     */
    @Transactional
    public TelemetryFormatResponse update(Long id, TelemetryFormatRequest request) {
        TelemetryFormat format = formatRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IOT_TELEMETRY_FORMAT_NOT_FOUND));

        List<Map<String, Object>> newFields = request.getFieldDefinitions();
        if ((newFields == null || newFields.isEmpty()) && request.getSamplePayload() != null) {
            newFields = parseJsonSample(request.getSamplePayload());
        }

        if (newFields != null && !newFields.isEmpty()) {
            checkRemovedFieldsNotInUse(format.getFieldDefinitions(), newFields);
            format.setFieldDefinitions(newFields);
        }

        if (request.getSamplePayload() != null) {
            format.setSamplePayload(request.getSamplePayload());
        }
        if (request.getVendorName() != null) {
            format.setVendorName(request.getVendorName());
        }
        if (request.getDeviceModel() != null) {
            format.setDeviceModel(request.getDeviceModel());
        }
        if (request.getDescription() != null) {
            format.setDescription(request.getDescription());
        }

        return toResponse(formatRepository.save(format));
    }

    /**
     * 欄位清單 (FN-07-047) — 廠商欄位 + 系統虛擬欄位 ($idle_minutes)。
     */
    public List<TelemetryFormatFieldResponse> getFields(Long id) {
        TelemetryFormat format = formatRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IOT_TELEMETRY_FORMAT_NOT_FOUND));

        List<TelemetryFormatFieldResponse> result = new ArrayList<>();

        for (Map<String, Object> field : format.getFieldDefinitions()) {
            result.add(TelemetryFormatFieldResponse.builder()
                    .name(String.valueOf(field.get("name")))
                    .type(String.valueOf(field.getOrDefault("type", "STRING")))
                    .unit(field.get("unit") != null ? String.valueOf(field.get("unit")) : null)
                    .virtual(false)
                    .build());
        }

        // 系統虛擬欄位
        result.add(TelemetryFormatFieldResponse.builder()
                .name("$idle_minutes")
                .type("NUMBER")
                .unit("minutes")
                .virtual(true)
                .build());

        return result;
    }

    /**
     * 遞迴解析 JSON sample → [{name, type, unit}] 欄位定義。
     */
    List<Map<String, Object>> parseJsonSample(Map<String, Object> json) {
        List<Map<String, Object>> fields = new ArrayList<>();
        parseJsonRecursive(json, "", fields);
        return fields;
    }

    private void parseJsonRecursive(Map<String, Object> json, String prefix, List<Map<String, Object>> fields) {
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                parseJsonRecursive(nested, key, fields);
            } else {
                String type = inferType(value);
                fields.add(Map.of("name", key, "type", type));
            }
        }
    }

    private String inferType(Object value) {
        if (value instanceof Number) return "NUMBER";
        if (value instanceof Boolean) return "BOOLEAN";
        return "STRING";
    }

    /**
     * 檢查被移除的欄位是否被 event_rule_conditions 引用。
     */
    private void checkRemovedFieldsNotInUse(List<Map<String, Object>> oldFields,
                                             List<Map<String, Object>> newFields) {
        Set<String> newNames = newFields.stream()
                .map(f -> String.valueOf(f.get("name")))
                .collect(Collectors.toSet());

        for (Map<String, Object> oldField : oldFields) {
            String name = String.valueOf(oldField.get("name"));
            if (!newNames.contains(name)) {
                // 欄位被移除，檢查是否被引用
                if (!conditionRepository.findByField(name).isEmpty()) {
                    throw new BusinessException(ErrorCode.IOT_TELEMETRY_FORMAT_FIELD_IN_USE,
                            "欄位 '" + name + "' 已被事件規則引用，無法刪除");
                }
            }
        }
    }

    private TelemetryFormatResponse toResponse(TelemetryFormat f) {
        return TelemetryFormatResponse.builder()
                .id(f.getId())
                .tenantId(f.getTenantId())
                .vendorName(f.getVendorName())
                .deviceModel(f.getDeviceModel())
                .version(f.getVersion())
                .fieldDefinitions(f.getFieldDefinitions())
                .samplePayload(f.getSamplePayload())
                .description(f.getDescription())
                .enabled(f.getEnabled())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
