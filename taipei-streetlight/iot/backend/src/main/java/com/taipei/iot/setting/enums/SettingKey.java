package com.taipei.iot.setting.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettingKey {

    IDLE_TIMEOUT_MINUTES("idle_timeout_minutes", "15"),
    FRONTEND_BASE_URL("frontend_base_url", "http://localhost:5173");

    private final String key;
    private final String defaultValue;
}
