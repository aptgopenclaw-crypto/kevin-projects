package com.taipei.iot.smartiot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "iot.no-signal-detection")
public class NoSignalProperties {
    private boolean enabled = true;
    private long intervalMs = 300_000; // 5 minutes
}
