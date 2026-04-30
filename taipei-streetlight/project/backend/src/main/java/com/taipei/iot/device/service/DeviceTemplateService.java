package com.taipei.iot.device.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.device.entity.DeviceTemplate;
import com.taipei.iot.device.repository.DeviceTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTemplateService {

    private final DeviceTemplateRepository templateRepository;

    /**
     * 取得指定 deviceType 的 schema。
     * 若不存在回傳 null（向後相容：不驗證）。
     */
    public Map<String, Object> getSchema(String deviceType) {
        return templateRepository.findByDeviceType(deviceType)
                .map(DeviceTemplate::getSchema)
                .orElse(null);
    }

    /**
     * 更新指定 deviceType 的 schema（ADMIN only）。
     */
    @Transactional
    public Map<String, Object> updateSchema(String deviceType, Map<String, Object> schema) {
        DeviceTemplate template = templateRepository.findByDeviceType(deviceType)
                .orElseGet(() -> {
                    DeviceTemplate t = new DeviceTemplate();
                    t.setDeviceType(deviceType);
                    t.setCreatedBy(SecurityContextUtils.getCurrentUserId());
                    return t;
                });

        template.setSchema(schema);
        template.setVersion(template.getVersion() + 1);
        return templateRepository.save(template).getSchema();
    }

    /**
     * 驗證 attributes 是否符合 schema 定義。
     * <ul>
     *   <li>Schema 不存在 → 不驗證（向後相容）</li>
     *   <li>Schema 存在 → 驗證 required + type (open schema: 額外 key 保留不報錯)</li>
     * </ul>
     */
    public void validateAttributes(String deviceType, Map<String, Object> attributes) {
        Map<String, Object> schema = getSchema(deviceType);
        if (schema == null) return; // 無 schema → 自由儲存

        Object fieldsObj = schema.get("fields");
        if (!(fieldsObj instanceof List<?> fields)) return;

        for (Object fieldObj : fields) {
            if (!(fieldObj instanceof Map<?, ?> fieldMap)) continue;

            String key = (String) fieldMap.get("key");
            String type = (String) fieldMap.get("type");
            Boolean required = Boolean.TRUE.equals(fieldMap.get("required"));

            if (key == null) continue;

            Object value = attributes != null ? attributes.get(key) : null;

            // required 檢查
            if (required && (value == null || (value instanceof String s && s.isBlank()))) {
                Object title = fieldMap.get("title");
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        String.format("欄位 '%s' 為必填", title != null ? title : key));
            }

            // type 檢查（僅在有值時驗證）
            if (value != null && type != null) {
                validateFieldType(key, type, value, fieldMap);
            }
        }
    }

    private void validateFieldType(String key, String type, Object value, Map<?, ?> fieldMap) {
        switch (type) {
            case "number" -> {
                if (!(value instanceof Number)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            String.format("欄位 '%s' 應為數字", key));
                }
                Number num = (Number) value;
                Object min = fieldMap.get("minimum");
                Object max = fieldMap.get("maximum");
                if (min instanceof Number minVal && num.doubleValue() < minVal.doubleValue()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            String.format("欄位 '%s' 不可小於 %s", key, min));
                }
                if (max instanceof Number maxVal && num.doubleValue() > maxVal.doubleValue()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            String.format("欄位 '%s' 不可大於 %s", key, max));
                }
            }
            case "text", "select" -> {
                if (!(value instanceof String)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            String.format("欄位 '%s' 應為文字", key));
                }
                // select: 驗證 enum 選項
                if ("select".equals(type)) {
                    Object optionsObj = fieldMap.get("options");
                    if (optionsObj instanceof List<?> options && !options.contains(value)) {
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                                String.format("欄位 '%s' 的值 '%s' 不在允許選項中", key, value));
                    }
                }
            }
            case "checkbox" -> {
                if (!(value instanceof Boolean)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            String.format("欄位 '%s' 應為布林值", key));
                }
            }
            case "date" -> {
                if (!(value instanceof String)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            String.format("欄位 '%s' 應為日期字串", key));
                }
            }
            default -> { /* 未知類型不驗證 */ }
        }
    }
}
