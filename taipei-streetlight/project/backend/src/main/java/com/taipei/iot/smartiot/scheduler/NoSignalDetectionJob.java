package com.taipei.iot.smartiot.scheduler;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.config.NoSignalProperties;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.entity.EventRuleCondition;
import com.taipei.iot.smartiot.event.AlertTriggeredEvent;
import com.taipei.iot.smartiot.repository.EventRuleConditionRepository;
import com.taipei.iot.smartiot.repository.EventRuleRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 無訊號偵測排程 (FN-07-049)。
 * <p>
 * D13: 規則驅動 — 掃描含 $idle_minutes 條件的 EventRule，取閾值。
 * D14: 走 AlertTriggeredEvent → AlertSuppressionEngine → 完整告警流程。
 * D15: 逐租戶設 TenantContext。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iot.no-signal-detection", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NoSignalDetectionJob {

    static final String IDLE_MINUTES_FIELD = "$idle_minutes";

    private final NoSignalProperties noSignalProperties;
    private final EventRuleConditionRepository conditionRepository;
    private final EventRuleRepository ruleRepository;
    private final DeviceRepository deviceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${iot.no-signal-detection.interval-ms:300000}")
    public void detect() {
        if (!noSignalProperties.isEnabled()) {
            return;
        }
        log.debug("[NoSignalDetection] Starting scan...");

        // D13: 查詢所有含 $idle_minutes 條件 → 取 ruleId + threshold
        List<EventRuleCondition> idleConditions = conditionRepository.findByField(IDLE_MINUTES_FIELD);
        if (idleConditions.isEmpty()) {
            log.debug("[NoSignalDetection] No $idle_minutes rules configured, skipping");
            return;
        }

        // 按 ruleId 分組 (一個 rule 可能有多個 $idle_minutes 條件，取最小閾值)
        Map<Long, Integer> ruleThresholds = idleConditions.stream()
                .collect(Collectors.toMap(
                        EventRuleCondition::getRuleId,
                        c -> parseThreshold(c.getThresholdValue()),
                        Math::min
                ));

        for (Map.Entry<Long, Integer> entry : ruleThresholds.entrySet()) {
            Long ruleId = entry.getKey();
            int thresholdMinutes = entry.getValue();
            processRule(ruleId, thresholdMinutes);
        }

        log.debug("[NoSignalDetection] Scan completed");
    }

    void processRule(Long ruleId, int thresholdMinutes) {
        EventRule rule = ruleRepository.findById(ruleId).orElse(null);
        if (rule == null || !Boolean.TRUE.equals(rule.getEnabled())) {
            return;
        }

        // D15: 設定 TenantContext
        try {
            TenantContext.setCurrentTenantId(rule.getTenantId());

            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(thresholdMinutes);

            // 解析 targetScope.areaIds → deptIds
            List<Long> deptIds = extractDeptIds(rule);

            List<Device> staleDevices = deviceRepository.findStaleIoTDevices(
                    rule.getTenantId(), cutoff, deptIds.isEmpty() ? null : deptIds);

            log.info("[NoSignalDetection] Rule '{}' (threshold={}min): found {} stale devices",
                    rule.getRuleName(), thresholdMinutes, staleDevices.size());

            // D14: 對每個逾時設備發布 AlertTriggeredEvent → 走抑制 + 完整告警流程
            for (Device device : staleDevices) {
                long idleMinutes = device.getLastTelemetryAt() != null
                        ? java.time.Duration.between(device.getLastTelemetryAt(), LocalDateTime.now()).toMinutes()
                        : thresholdMinutes; // null → 至少達到閾值

                Map<String, Object> triggeredValues = Map.of(
                        "$idle_minutes", idleMinutes,
                        "threshold", thresholdMinutes
                );

                eventPublisher.publishEvent(new AlertTriggeredEvent(
                        this, rule.getId(), rule.getRuleName(),
                        device.getId(), rule.getSeverity(),
                        triggeredValues, rule.getTenantId()));
            }
        } finally {
            TenantContext.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractDeptIds(EventRule rule) {
        Map<String, Object> scope = rule.getTargetScope();
        if (scope == null) return List.of();

        Object areaIdsObj = scope.get("areaIds");
        if (areaIdsObj instanceof List<?> areaIds && !areaIds.isEmpty()) {
            return areaIds.stream()
                    .map(id -> (id instanceof Number n) ? n.longValue() : Long.parseLong(id.toString()))
                    .toList();
        }
        return List.of();
    }

    private int parseThreshold(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("[NoSignalDetection] Invalid threshold '{}', defaulting to 120", value);
            return 120;
        }
    }
}
