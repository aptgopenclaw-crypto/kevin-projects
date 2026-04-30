package com.taipei.iot.setting.service;

import com.taipei.iot.setting.dto.SystemSettingDto;
import com.taipei.iot.setting.entity.SystemSettingEntity;
import com.taipei.iot.setting.enums.SettingKey;
import com.taipei.iot.setting.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
        SystemSettingEntity entity = settingRepository.findBySettingKey(key)
                .orElseThrow(() -> new IllegalStateException("Setting not found: " + key));
        entity.setSettingValue(value);
        entity.setUpdatedAt(LocalDateTime.now());
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
                .map(e -> Integer.parseInt(e.getSettingValue()))
                .orElse(Integer.parseInt(SettingKey.IDLE_TIMEOUT_MINUTES.getDefaultValue()));
    }

    @Transactional
    public int updateIdleTimeoutMinutes(int minutes) {
        SystemSettingEntity entity = settingRepository
                .findBySettingKey(SettingKey.IDLE_TIMEOUT_MINUTES.getKey())
                .orElseThrow(() -> new IllegalStateException(
                        "Setting not found: " + SettingKey.IDLE_TIMEOUT_MINUTES.getKey()));
        entity.setSettingValue(String.valueOf(minutes));
        entity.setUpdatedAt(LocalDateTime.now());
        settingRepository.save(entity);
        return minutes;
    }

    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSettingEntity::getSettingValue)
                .orElseThrow(() -> new IllegalStateException("Setting not found: " + key));
    }
}
