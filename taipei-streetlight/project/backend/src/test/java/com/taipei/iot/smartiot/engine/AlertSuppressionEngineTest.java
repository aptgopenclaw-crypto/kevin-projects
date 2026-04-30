package com.taipei.iot.smartiot.engine;

import com.taipei.iot.smartiot.entity.AlertHistory;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.AlertStatus;
import com.taipei.iot.smartiot.event.AlertTriggeredEvent;
import com.taipei.iot.smartiot.repository.AlertHistoryRepository;
import com.taipei.iot.smartiot.repository.EventRuleRepository;
import com.taipei.iot.smartiot.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertSuppressionEngineTest {

    @Mock private AlertHistoryRepository alertHistoryRepository;
    @Mock private EventRuleRepository eventRuleRepository;
    @Mock private AlertService alertService;

    @InjectMocks private AlertSuppressionEngine engine;

    private EventRule sampleRule;

    @BeforeEach
    void setUp() {
        sampleRule = EventRule.builder()
                .id(1L)
                .ruleName("Low RSSI Alert")
                .severity(AlertSeverity.WARNING)
                .suppressDurationMin(30)
                .autoCreateTicket(false)
                .enabled(true)
                .build();
    }

    private AlertTriggeredEvent makeEvent(Long ruleId, Long deviceId) {
        return new AlertTriggeredEvent(
                this, ruleId, "Low RSSI Alert", deviceId,
                AlertSeverity.WARNING, Map.of("rssi", -105), "TENANT_A");
    }

    // ── TC-1: Rule not found → skip ──
    @Test
    void processAlert_ruleNotFound_skips() {
        AlertTriggeredEvent event = makeEvent(999L, 100L);
        when(eventRuleRepository.findById(999L)).thenReturn(Optional.empty());

        engine.processAlert(event);

        verify(alertService, never()).createAlert(any(), any(), any());
    }

    // ── TC-2: No previous alert → create ──
    @Test
    void processAlert_noHistory_createsAlert() {
        AlertTriggeredEvent event = makeEvent(1L, 100L);
        when(eventRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));
        when(alertHistoryRepository.findLatestByDeviceIdAndRuleId(100L, 1L))
                .thenReturn(Optional.empty());

        engine.processAlert(event);

        verify(alertService).createAlert(eq(event), eq(sampleRule), any(String.class));
    }

    // ── TC-3: Previous alert within suppress window → suppressed ──
    @Test
    void processAlert_withinSuppressWindow_suppressed() {
        AlertTriggeredEvent event = makeEvent(1L, 100L);
        when(eventRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        AlertHistory recent = AlertHistory.builder()
                .id(10L)
                .ruleId(1L)
                .deviceId(100L)
                .triggeredAt(LocalDateTime.now().minusMinutes(10)) // 10 min ago, within 30 min window
                .status(AlertStatus.OPEN)
                .build();
        when(alertHistoryRepository.findLatestByDeviceIdAndRuleId(100L, 1L))
                .thenReturn(Optional.of(recent));

        engine.processAlert(event);

        verify(alertService, never()).createAlert(any(), any(), any());
    }

    // ── TC-4: Previous alert outside suppress window → create ──
    @Test
    void processAlert_outsideSuppressWindow_createsAlert() {
        AlertTriggeredEvent event = makeEvent(1L, 100L);
        when(eventRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        AlertHistory old = AlertHistory.builder()
                .id(5L)
                .ruleId(1L)
                .deviceId(100L)
                .triggeredAt(LocalDateTime.now().minusMinutes(60)) // 60 min ago, outside 30 min window
                .status(AlertStatus.OPEN)
                .build();
        when(alertHistoryRepository.findLatestByDeviceIdAndRuleId(100L, 1L))
                .thenReturn(Optional.of(old));

        engine.processAlert(event);

        verify(alertService).createAlert(eq(event), eq(sampleRule), any(String.class));
    }

    // ── TC-5: Custom suppress duration ──
    @Test
    void processAlert_customSuppressDuration_respected() {
        sampleRule.setSuppressDurationMin(5);
        AlertTriggeredEvent event = makeEvent(1L, 100L);
        when(eventRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        AlertHistory recent = AlertHistory.builder()
                .id(10L)
                .ruleId(1L)
                .deviceId(100L)
                .triggeredAt(LocalDateTime.now().minusMinutes(3)) // 3 min ago, within 5 min window
                .status(AlertStatus.OPEN)
                .build();
        when(alertHistoryRepository.findLatestByDeviceIdAndRuleId(100L, 1L))
                .thenReturn(Optional.of(recent));

        engine.processAlert(event);

        verify(alertService, never()).createAlert(any(), any(), any());
    }

    // ── TC-6: Different device → not suppressed ──
    @Test
    void processAlert_differentDevice_createsAlert() {
        AlertTriggeredEvent event = makeEvent(1L, 200L); // device 200
        when(eventRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));
        when(alertHistoryRepository.findLatestByDeviceIdAndRuleId(200L, 1L))
                .thenReturn(Optional.empty()); // no history for device 200

        engine.processAlert(event);

        verify(alertService).createAlert(eq(event), eq(sampleRule), any(String.class));
    }

    // ── TC-7: Null suppress duration defaults to 30 min ──
    @Test
    void processAlert_nullSuppressDuration_defaultsTo30() {
        sampleRule.setSuppressDurationMin(null);
        AlertTriggeredEvent event = makeEvent(1L, 100L);
        when(eventRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        AlertHistory recent = AlertHistory.builder()
                .id(10L)
                .ruleId(1L)
                .deviceId(100L)
                .triggeredAt(LocalDateTime.now().minusMinutes(20)) // 20 min < 30 min default
                .status(AlertStatus.OPEN)
                .build();
        when(alertHistoryRepository.findLatestByDeviceIdAndRuleId(100L, 1L))
                .thenReturn(Optional.of(recent));

        engine.processAlert(event);

        verify(alertService, never()).createAlert(any(), any(), any());
    }
}
