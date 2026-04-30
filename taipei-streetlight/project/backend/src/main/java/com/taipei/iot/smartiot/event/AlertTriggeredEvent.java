package com.taipei.iot.smartiot.event;

import com.taipei.iot.smartiot.enums.AlertSeverity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 事件規則觸發事件。
 * 7e1 EventRuleEngine 在規則匹配成功時發布，7e2 AlertSuppressionEngine 接收處理。
 */
@Getter
public class AlertTriggeredEvent extends ApplicationEvent {

    private final Long ruleId;
    private final String ruleName;
    private final Long deviceId;
    private final AlertSeverity severity;
    private final Map<String, Object> triggeredValues;
    private final String tenantId;

    public AlertTriggeredEvent(Object source, Long ruleId, String ruleName,
                                Long deviceId, AlertSeverity severity,
                                Map<String, Object> triggeredValues, String tenantId) {
        super(source);
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.deviceId = deviceId;
        this.severity = severity;
        this.triggeredValues = triggeredValues;
        this.tenantId = tenantId;
    }
}
