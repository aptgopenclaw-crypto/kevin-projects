package com.taipei.iot.smartiot.engine;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.entity.EventRuleCondition;
import com.taipei.iot.smartiot.enums.ConditionLogic;
import com.taipei.iot.smartiot.enums.ConditionOperator;
import com.taipei.iot.smartiot.event.AlertTriggeredEvent;
import com.taipei.iot.smartiot.event.TelemetryIngestedEvent;
import com.taipei.iot.smartiot.repository.EventRuleConditionRepository;
import com.taipei.iot.smartiot.repository.EventRuleRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config-Driven 事件規則引擎 (FN-07-014)。
 * <p>
 * 監聽 TelemetryIngestedEvent (D8)，遍歷所有啟用規則，
 * 按 condition_group 分組進行複合條件比對，
 * 匹配成功時發布 AlertTriggeredEvent (7e2 接口)。
 * </p>
 * <p>
 * D10: target_scope.areaIds 對應 device.deptId。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventRuleEngine {

    private final EventRuleRepository ruleRepository;
    private final EventRuleConditionRepository conditionRepository;
    private final DeviceRepository deviceRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 監聽 TelemetryIngestedEvent，觸發規則評估。
     */
    @EventListener
    public void onTelemetryIngested(TelemetryIngestedEvent event) {
        try {
            TenantContext.setCurrentTenantId(event.getTenantId());
            evaluate(event.getDeviceId(), event.getPayload(), event.getFormatId(), event.getTenantId());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 主入口: 評估所有啟用規則。
     */
    public void evaluate(Long deviceId, Map<String, Object> payload, Long formatId, String tenantId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            log.warn("[EventRuleEngine] Device {} not found, skipping evaluation", deviceId);
            return;
        }

        // 取得匹配 formatId 的啟用規則
        List<EventRule> rules;
        if (formatId != null) {
            rules = ruleRepository.findByEnabledTrueAndFormatId(formatId);
        } else {
            rules = ruleRepository.findByEnabledTrue();
        }

        for (EventRule rule : rules) {
            if (!matchesTargetScope(rule, device)) {
                continue;
            }

            List<EventRuleCondition> conditions = conditionRepository.findByRuleIdOrderBySortOrder(rule.getId());
            if (conditions.isEmpty()) {
                continue;
            }

            Map<String, Object> triggeredValues = new HashMap<>();
            boolean triggered = evaluateCompoundConditions(conditions, rule.getConditionLogic(), payload, triggeredValues);

            if (triggered) {
                log.info("[EventRuleEngine] Rule '{}' triggered for device={}, values={}",
                        rule.getRuleName(), deviceId, triggeredValues);

                eventPublisher.publishEvent(new AlertTriggeredEvent(
                        this, rule.getId(), rule.getRuleName(),
                        deviceId, rule.getSeverity(), triggeredValues, tenantId));
            }
        }
    }

    /**
     * target_scope 匹配 (D10: areaIds = deptId 清單)。
     */
    boolean matchesTargetScope(EventRule rule, Device device) {
        Map<String, Object> scope = rule.getTargetScope();
        if (scope == null || scope.isEmpty()) {
            return true; // 無限制 → 全部設備適用
        }

        // deviceType 匹配
        Object scopeDeviceType = scope.get("deviceType");
        if (scopeDeviceType != null && device.getDeviceType() != null) {
            if (!device.getDeviceType().name().equals(scopeDeviceType.toString())) {
                return false;
            }
        }

        // areaIds 匹配 (D10: areaIds = deptId 清單)
        Object areaIdsObj = scope.get("areaIds");
        if (areaIdsObj instanceof List<?> areaIds && !areaIds.isEmpty()) {
            if (device.getDeptId() == null) {
                return false;
            }
            boolean matches = areaIds.stream()
                    .anyMatch(id -> {
                        long areaId = (id instanceof Number n) ? n.longValue() : Long.parseLong(id.toString());
                        return areaId == device.getDeptId();
                    });
            if (!matches) {
                return false;
            }
        }

        return true;
    }

    /**
     * 複合條件評估。
     * 群組內 AND，群組間依 conditionLogic (AND/OR)。
     */
    boolean evaluateCompoundConditions(List<EventRuleCondition> conditions,
                                        ConditionLogic logic,
                                        Map<String, Object> payload,
                                        Map<String, Object> triggeredValues) {
        // 按 conditionGroup 分組
        Map<Integer, List<EventRuleCondition>> groups = conditions.stream()
                .collect(Collectors.groupingBy(EventRuleCondition::getConditionGroup));

        List<Boolean> groupResults = groups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> evaluateGroup(entry.getValue(), payload, triggeredValues))
                .toList();

        if (logic == ConditionLogic.OR) {
            return groupResults.stream().anyMatch(Boolean::booleanValue);
        } else {
            // AND (default)
            return groupResults.stream().allMatch(Boolean::booleanValue);
        }
    }

    /**
     * 單一群組內條件評估 (全部 AND)。
     */
    private boolean evaluateGroup(List<EventRuleCondition> conditions,
                                   Map<String, Object> payload,
                                   Map<String, Object> triggeredValues) {
        for (EventRuleCondition condition : conditions) {
            String field = condition.getField();

            // $idle_minutes 虛擬欄位 → 7e3 處理，這裡跳過
            if (field != null && field.startsWith("$")) {
                return false;
            }

            Object rawValue = payload.get(field);
            if (rawValue == null) {
                return false; // 欄位不存在 → 視為不成立
            }

            double value;
            try {
                value = ((Number) rawValue).doubleValue();
            } catch (ClassCastException e) {
                return false; // 非數值 → 視為不成立
            }

            double threshold;
            try {
                threshold = Double.parseDouble(condition.getThresholdValue());
            } catch (NumberFormatException e) {
                log.warn("[EventRuleEngine] Invalid threshold '{}' for field '{}'",
                        condition.getThresholdValue(), field);
                return false;
            }

            if (!evaluateCondition(value, condition.getOperator(), threshold)) {
                return false;
            }

            triggeredValues.put(field, value);
        }
        return true;
    }

    /**
     * 單條件比對。
     */
    boolean evaluateCondition(double value, ConditionOperator operator, double threshold) {
        return switch (operator) {
            case GT -> value > threshold;
            case GTE -> value >= threshold;
            case LT -> value < threshold;
            case LTE -> value <= threshold;
            case EQ -> Double.compare(value, threshold) == 0;
            case NEQ -> Double.compare(value, threshold) != 0;
        };
    }
}
