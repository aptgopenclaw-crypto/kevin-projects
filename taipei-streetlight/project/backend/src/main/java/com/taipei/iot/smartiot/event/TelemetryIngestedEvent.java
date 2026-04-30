package com.taipei.iot.smartiot.event;

import com.taipei.iot.smartiot.enums.QualityFlag;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * Telemetry 入庫完成事件 (D8 接口)。
 * 7c 在 ingest() 結尾發布此事件，7e1 透過 @EventListener 接入 EventRuleEngine。
 */
@Getter
public class TelemetryIngestedEvent extends ApplicationEvent {

    private final Long deviceId;
    private final Long formatId;
    private final Map<String, Object> payload;
    private final QualityFlag qualityFlag;
    private final String tenantId;

    public TelemetryIngestedEvent(Object source, Long deviceId, Long formatId,
                                   Map<String, Object> payload, QualityFlag qualityFlag,
                                   String tenantId) {
        super(source);
        this.deviceId = deviceId;
        this.formatId = formatId;
        this.payload = payload;
        this.qualityFlag = qualityFlag;
        this.tenantId = tenantId;
    }
}
