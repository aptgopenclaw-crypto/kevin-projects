package com.taipei.iot.smartiot.scheduler;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.config.NoSignalProperties;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.entity.EventRuleCondition;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.ConditionOperator;
import com.taipei.iot.smartiot.event.AlertTriggeredEvent;
import com.taipei.iot.smartiot.repository.EventRuleConditionRepository;
import com.taipei.iot.smartiot.repository.EventRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoSignalDetectionJobTest {

    @Mock private NoSignalProperties noSignalProperties;
    @Mock private EventRuleConditionRepository conditionRepository;
    @Mock private EventRuleRepository ruleRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private NoSignalDetectionJob job;

    private EventRule sampleRule;

    @BeforeEach
    void setUp() {
        sampleRule = EventRule.builder()
                .id(1L)
                .tenantId("TENANT_A")
                .ruleName("No Signal 120min")
                .severity(AlertSeverity.WARNING)
                .enabled(true)
                .targetScope(Map.of("areaIds", List.of(6, 7)))
                .build();
    }

    private EventRuleCondition idleCondition(Long ruleId, String threshold) {
        return EventRuleCondition.builder()
                .id(100L)
                .ruleId(ruleId)
                .field("$idle_minutes")
                .operator(ConditionOperator.GT)
                .thresholdValue(threshold)
                .build();
    }

    private Device staleDevice(Long id, LocalDateTime lastTelemetry) {
        Device d = new Device();
        d.setId(id);
        d.setTenantId("TENANT_A");
        d.setDeviceToken("token-" + id);
        d.setLastTelemetryAt(lastTelemetry);
        d.setDeptId(6L);
        return d;
    }

    // ── TC-1: disabled → skip ──
    @Test
    void detect_disabled_skips() {
        when(noSignalProperties.isEnabled()).thenReturn(false);

        job.detect();

        verify(conditionRepository, never()).findByField(any());
    }

    // ── TC-2: no $idle_minutes rules → skip ──
    @Test
    void detect_noIdleRules_skips() {
        when(noSignalProperties.isEnabled()).thenReturn(true);
        when(conditionRepository.findByField("$idle_minutes")).thenReturn(List.of());

        job.detect();

        verify(ruleRepository, never()).findById(any());
    }

    // ── TC-3: stale device → publishes AlertTriggeredEvent ──
    @Test
    void detect_staleDevice_publishesEvent() {
        when(noSignalProperties.isEnabled()).thenReturn(true);
        when(conditionRepository.findByField("$idle_minutes"))
                .thenReturn(List.of(idleCondition(1L, "120")));
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        Device stale = staleDevice(100L, LocalDateTime.now().minusMinutes(180));
        when(deviceRepository.findStaleIoTDevices(eq("TENANT_A"), any(), eq(List.of(6L, 7L))))
                .thenReturn(List.of(stale));

        job.detect();

        ArgumentCaptor<AlertTriggeredEvent> captor = ArgumentCaptor.forClass(AlertTriggeredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        AlertTriggeredEvent event = captor.getValue();
        assertThat(event.getRuleId()).isEqualTo(1L);
        assertThat(event.getDeviceId()).isEqualTo(100L);
        assertThat(event.getSeverity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(event.getTenantId()).isEqualTo("TENANT_A");
        assertThat(event.getTriggeredValues()).containsKey("$idle_minutes");
    }

    // ── TC-4: no stale devices → no events ──
    @Test
    void detect_noStaleDevices_noEvents() {
        when(noSignalProperties.isEnabled()).thenReturn(true);
        when(conditionRepository.findByField("$idle_minutes"))
                .thenReturn(List.of(idleCondition(1L, "120")));
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));
        when(deviceRepository.findStaleIoTDevices(eq("TENANT_A"), any(), eq(List.of(6L, 7L))))
                .thenReturn(List.of());

        job.detect();

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── TC-5: disabled rule → skip ──
    @Test
    void detect_disabledRule_skips() {
        when(noSignalProperties.isEnabled()).thenReturn(true);
        when(conditionRepository.findByField("$idle_minutes"))
                .thenReturn(List.of(idleCondition(1L, "120")));
        sampleRule.setEnabled(false);
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        job.detect();

        verify(deviceRepository, never()).findStaleIoTDevices(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── TC-6: multiple stale devices → multiple events ──
    @Test
    void detect_multipleStaleDevices_publishesMultipleEvents() {
        when(noSignalProperties.isEnabled()).thenReturn(true);
        when(conditionRepository.findByField("$idle_minutes"))
                .thenReturn(List.of(idleCondition(1L, "60")));
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        Device d1 = staleDevice(100L, LocalDateTime.now().minusMinutes(90));
        Device d2 = staleDevice(200L, LocalDateTime.now().minusMinutes(120));
        when(deviceRepository.findStaleIoTDevices(eq("TENANT_A"), any(), eq(List.of(6L, 7L))))
                .thenReturn(List.of(d1, d2));

        job.detect();

        verify(eventPublisher, times(2)).publishEvent(any(AlertTriggeredEvent.class));
    }

    // ── TC-7: null lastTelemetryAt → treated as stale ──
    @Test
    void detect_nullLastTelemetryAt_usesThresholdAsIdle() {
        when(noSignalProperties.isEnabled()).thenReturn(true);
        when(conditionRepository.findByField("$idle_minutes"))
                .thenReturn(List.of(idleCondition(1L, "120")));
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        Device d = staleDevice(100L, null);
        when(deviceRepository.findStaleIoTDevices(eq("TENANT_A"), any(), eq(List.of(6L, 7L))))
                .thenReturn(List.of(d));

        job.detect();

        ArgumentCaptor<AlertTriggeredEvent> captor = ArgumentCaptor.forClass(AlertTriggeredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        // null lastTelemetryAt → idleMinutes defaults to threshold
        assertThat((long) captor.getValue().getTriggeredValues().get("$idle_minutes")).isEqualTo(120L);
    }

    // ── TC-8: rule without targetScope → queries all tenanted devices ──
    @Test
    void detect_noTargetScope_queriesAllDevices() {
        when(noSignalProperties.isEnabled()).thenReturn(true);
        when(conditionRepository.findByField("$idle_minutes"))
                .thenReturn(List.of(idleCondition(1L, "120")));
        sampleRule.setTargetScope(null);
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));
        when(deviceRepository.findStaleIoTDevices(eq("TENANT_A"), any(), eq(null)))
                .thenReturn(List.of());

        job.detect();

        verify(deviceRepository).findStaleIoTDevices(eq("TENANT_A"), any(), eq(null));
    }
}
