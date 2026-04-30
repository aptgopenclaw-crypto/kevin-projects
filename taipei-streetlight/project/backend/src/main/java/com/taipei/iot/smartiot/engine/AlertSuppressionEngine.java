package com.taipei.iot.smartiot.engine;

import com.taipei.iot.smartiot.entity.AlertHistory;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.event.AlertTriggeredEvent;
import com.taipei.iot.smartiot.repository.AlertHistoryRepository;
import com.taipei.iot.smartiot.repository.EventRuleRepository;
import com.taipei.iot.smartiot.service.AlertService;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 告警抑制引擎 (FN-07-015)。
 * <p>
 * 監聽 AlertTriggeredEvent (7e1→7e2 接口)，
 * 判斷同 device + rule 是否在 suppressDurationMin 內已有告警，
 * 若未抑制則委派 AlertService 建立告警。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertSuppressionEngine {

    private final AlertHistoryRepository alertHistoryRepository;
    private final EventRuleRepository eventRuleRepository;
    private final AlertService alertService;

    @EventListener
    public void onAlertTriggered(AlertTriggeredEvent event) {
        try {
            TenantContext.setCurrentTenantId(event.getTenantId());
            processAlert(event);
        } finally {
            TenantContext.clear();
        }
    }

    void processAlert(AlertTriggeredEvent event) {
        EventRule rule = eventRuleRepository.findById(event.getRuleId()).orElse(null);
        if (rule == null) {
            log.warn("[AlertSuppression] Rule {} not found, skipping", event.getRuleId());
            return;
        }

        // 查詢同 device+rule 的最近告警
        Optional<AlertHistory> latestOpt = alertHistoryRepository
                .findLatestByDeviceIdAndRuleId(event.getDeviceId(), event.getRuleId());

        if (latestOpt.isPresent()) {
            AlertHistory latest = latestOpt.get();
            int suppressMin = rule.getSuppressDurationMin() != null ? rule.getSuppressDurationMin() : 30;
            LocalDateTime suppressUntil = latest.getTriggeredAt().plusMinutes(suppressMin);

            if (LocalDateTime.now().isBefore(suppressUntil)) {
                log.info("[AlertSuppression] Alert suppressed for device={}, rule={}, lastTriggered={}, suppressUntil={}",
                        event.getDeviceId(), event.getRuleId(), latest.getTriggeredAt(), suppressUntil);
                return;
            }
        }

        // 未被抑制 → 建立告警
        String message = String.format("規則 [%s] 觸發：%s", event.getRuleName(), event.getTriggeredValues());
        alertService.createAlert(event, rule, message);
    }
}
