package com.taipei.iot.kpi.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

/**
 * 從 IoT 模組收集 KPI 數據。
 * <p>Phase 7 完成前為空實作 (D5: 預留介面，graceful skip)。</p>
 */
@Slf4j
@Component
public class IoTDataCollector implements KpiDataCollector {

    @Override
    public Map<String, BigDecimal> collect(LocalDate date) {
        log.debug("IoTDataCollector: Phase 7 尚未完成，跳過收集");
        return Collections.emptyMap();
    }

    @Override
    public String getSourceName() {
        return "IOT";
    }
}
