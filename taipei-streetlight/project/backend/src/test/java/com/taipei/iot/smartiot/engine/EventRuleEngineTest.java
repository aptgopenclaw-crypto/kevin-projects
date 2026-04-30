package com.taipei.iot.smartiot.engine;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.entity.EventRuleCondition;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.ConditionLogic;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRuleEngineTest {

    @Mock private EventRuleRepository ruleRepository;
    @Mock private EventRuleConditionRepository conditionRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private EventRuleEngine engine;

    private Device device;

    @BeforeEach
    void setUp() {
        device = Device.builder()
                .id(100L)
                .tenantId("TENANT_A")
                .deviceType(DeviceType.LUMINAIRE)
                .deviceCode("L-001")
                .deptId(1L)
                .build();
    }

    private EventRule buildRule(Long id, String name, ConditionLogic logic) {
        return EventRule.builder()
                .id(id)
                .tenantId("TENANT_A")
                .ruleName(name)
                .severity(AlertSeverity.WARNING)
                .conditionLogic(logic)
                .formatId(1L)
                .enabled(true)
                .build();
    }

    private EventRuleCondition buildCondition(Long ruleId, int group, String field,
                                               ConditionOperator op, String threshold) {
        return EventRuleCondition.builder()
                .id(1L)
                .ruleId(ruleId)
                .conditionGroup(group)
                .field(field)
                .operator(op)
                .thresholdValue(threshold)
                .sortOrder(0)
                .build();
    }

    // ── TC1: 單一條件 (rssi <= -100) → 觸發 ──

    @Test
    void evaluate_singleConditionMet_triggersAlert() {
        EventRule rule = buildRule(1L, "Low RSSI", ConditionLogic.AND);
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(ruleRepository.findByEnabledTrueAndFormatId(1L)).thenReturn(List.of(rule));
        when(conditionRepository.findByRuleIdOrderBySortOrder(1L)).thenReturn(List.of(
                buildCondition(1L, 1, "rssi", ConditionOperator.LTE, "-100")
        ));

        Map<String, Object> payload = Map.of("rssi", -105);

        engine.evaluate(100L, payload, 1L, "TENANT_A");

        ArgumentCaptor<AlertTriggeredEvent> captor = ArgumentCaptor.forClass(AlertTriggeredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        AlertTriggeredEvent event = captor.getValue();
        assertThat(event.getRuleId()).isEqualTo(1L);
        assertThat(event.getDeviceId()).isEqualTo(100L);
        assertThat(event.getTriggeredValues()).containsEntry("rssi", -105.0);
    }

    // ── TC2: 單一條件未滿足 → 不觸發 ──

    @Test
    void evaluate_singleConditionNotMet_noAlert() {
        EventRule rule = buildRule(1L, "Low RSSI", ConditionLogic.AND);
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(ruleRepository.findByEnabledTrueAndFormatId(1L)).thenReturn(List.of(rule));
        when(conditionRepository.findByRuleIdOrderBySortOrder(1L)).thenReturn(List.of(
                buildCondition(1L, 1, "rssi", ConditionOperator.LTE, "-100")
        ));

        Map<String, Object> payload = Map.of("rssi", -50);

        engine.evaluate(100L, payload, 1L, "TENANT_A");

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── TC3: AND 群組全部成立 → 觸發 ──

    @Test
    void evaluate_andGroupAllMet_triggersAlert() {
        EventRule rule = buildRule(1L, "Low RSSI + Low Voltage", ConditionLogic.AND);
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(ruleRepository.findByEnabledTrueAndFormatId(1L)).thenReturn(List.of(rule));
        when(conditionRepository.findByRuleIdOrderBySortOrder(1L)).thenReturn(List.of(
                buildCondition(1L, 1, "rssi", ConditionOperator.LTE, "-100"),
                buildCondition(1L, 1, "voltage", ConditionOperator.LT, "180")
        ));

        Map<String, Object> payload = Map.of("rssi", -105, "voltage", 150);

        engine.evaluate(100L, payload, 1L, "TENANT_A");

        verify(eventPublisher).publishEvent(any(AlertTriggeredEvent.class));
    }

    // ── TC4: AND 群組部分成立 → 不觸發 ──

    @Test
    void evaluate_andGroupPartialMet_noAlert() {
        EventRule rule = buildRule(1L, "Low RSSI + Low Voltage", ConditionLogic.AND);
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(ruleRepository.findByEnabledTrueAndFormatId(1L)).thenReturn(List.of(rule));
        when(conditionRepository.findByRuleIdOrderBySortOrder(1L)).thenReturn(List.of(
                buildCondition(1L, 1, "rssi", ConditionOperator.LTE, "-100"),
                buildCondition(1L, 1, "voltage", ConditionOperator.LT, "180")
        ));

        // rssi matches but voltage does not
        Map<String, Object> payload = Map.of("rssi", -105, "voltage", 220);

        engine.evaluate(100L, payload, 1L, "TENANT_A");

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── TC5: OR 多群組任一成立 → 觸發 ──

    @Test
    void evaluate_orMultipleGroupsOneMet_triggersAlert() {
        EventRule rule = buildRule(1L, "Low RSSI OR High Temp", ConditionLogic.OR);
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(ruleRepository.findByEnabledTrueAndFormatId(1L)).thenReturn(List.of(rule));
        when(conditionRepository.findByRuleIdOrderBySortOrder(1L)).thenReturn(List.of(
                buildCondition(1L, 1, "rssi", ConditionOperator.LTE, "-100"),  // group 1 - NOT met
                buildCondition(1L, 2, "temperature", ConditionOperator.GT, "80") // group 2 - met
        ));

        // rssi does not match (-50 > -100), but temperature matches (85 > 80)
        Map<String, Object> payload = Map.of("rssi", -50, "temperature", 85);

        engine.evaluate(100L, payload, 1L, "TENANT_A");

        verify(eventPublisher).publishEvent(any(AlertTriggeredEvent.class));
    }

    // ── TC6: disabled 規則 → 跳過 ──

    @Test
    void evaluate_disabledRule_skipped() {
        // findByEnabledTrueAndFormatId won't return disabled rules
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(ruleRepository.findByEnabledTrueAndFormatId(1L)).thenReturn(List.of());

        engine.evaluate(100L, Map.of("rssi", -105), 1L, "TENANT_A");

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── TC7: JSONB 欄位不存在 → 視為不成立 (不拋錯) ──

    @Test
    void evaluate_missingField_treatedAsNotMet() {
        EventRule rule = buildRule(1L, "Low RSSI", ConditionLogic.AND);
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(ruleRepository.findByEnabledTrueAndFormatId(1L)).thenReturn(List.of(rule));
        when(conditionRepository.findByRuleIdOrderBySortOrder(1L)).thenReturn(List.of(
                buildCondition(1L, 1, "rssi", ConditionOperator.LTE, "-100")
        ));

        // payload does not contain "rssi"
        Map<String, Object> payload = Map.of("voltage", 220);

        engine.evaluate(100L, payload, 1L, "TENANT_A");

        verify(eventPublisher, never()).publishEvent(any());
    }
}
