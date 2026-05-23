package com.taipei.iot.setting.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.setting.dto.SystemSettingDto;
import com.taipei.iot.setting.entity.SystemSettingEntity;
import com.taipei.iot.setting.enums.SettingKey;
import com.taipei.iot.setting.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public List<SystemSettingDto> findAllSettings() {
        return settingRepository.findAll().stream()
                .map(e -> SystemSettingDto.builder()
                        .settingKey(e.getSettingKey())
                        .settingValue(e.getSettingValue())
                        .description(e.getDescription())
                        .build())
                .toList();
    }

    @Transactional
    public SystemSettingDto updateSetting(String key, String value) {
        validateSettingValue(key, value);
        SystemSettingEntity entity = settingRepository.findBySettingKey(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTING_NOT_FOUND));
        entity.setSettingValue(value);
        settingRepository.save(entity);
        return SystemSettingDto.builder()
                .settingKey(entity.getSettingKey())
                .settingValue(entity.getSettingValue())
                .description(entity.getDescription())
                .build();
    }

    @Transactional(readOnly = true)
    public int getIdleTimeoutMinutes() {
        return settingRepository.findBySettingKey(SettingKey.IDLE_TIMEOUT_MINUTES.getKey())
                .map(e -> parseIntOrDefault(e.getSettingValue(),
                        Integer.parseInt(SettingKey.IDLE_TIMEOUT_MINUTES.getDefaultValue())))
                .orElse(Integer.parseInt(SettingKey.IDLE_TIMEOUT_MINUTES.getDefaultValue()));
    }

    @Transactional
    public int updateIdleTimeoutMinutes(int minutes) {
        SystemSettingEntity entity = settingRepository
                .findBySettingKey(SettingKey.IDLE_TIMEOUT_MINUTES.getKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTING_NOT_FOUND));
        entity.setSettingValue(String.valueOf(minutes));
        settingRepository.save(entity);
        return minutes;
    }

    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSettingEntity::getSettingValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTING_NOT_FOUND));
    }

    private void validateSettingValue(String key, String value) {
        if (SettingKey.IDLE_TIMEOUT_MINUTES.getKey().equals(key)) {
            try {
                int minutes = Integer.parseInt(value);
                if (minutes < 1 || minutes > 480) {
                    throw new BusinessException(ErrorCode.SETTING_INVALID_VALUE,
                            "idle_timeout_minutes must be between 1 and 480");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.SETTING_INVALID_VALUE,
                        "idle_timeout_minutes must be a valid integer");
            }
        }
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer setting value '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }
}
