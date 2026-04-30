package com.taipei.iot.smartiot.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.smartiot.dto.EventNotificationChannelRequest;
import com.taipei.iot.smartiot.dto.EventNotificationChannelResponse;
import com.taipei.iot.smartiot.dto.EventNotificationTargetRequest;
import com.taipei.iot.smartiot.dto.EventNotificationTargetResponse;
import com.taipei.iot.smartiot.dto.EventRuleConditionRequest;
import com.taipei.iot.smartiot.dto.EventRuleConditionResponse;
import com.taipei.iot.smartiot.dto.EventRuleRequest;
import com.taipei.iot.smartiot.dto.EventRuleResponse;
import com.taipei.iot.smartiot.entity.EventNotificationChannel;
import com.taipei.iot.smartiot.entity.EventNotificationTarget;
import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.smartiot.entity.EventRuleCondition;
import com.taipei.iot.smartiot.enums.ConditionLogic;
import com.taipei.iot.smartiot.repository.EventNotificationChannelRepository;
import com.taipei.iot.smartiot.repository.EventNotificationTargetRepository;
import com.taipei.iot.smartiot.repository.EventRuleConditionRepository;
import com.taipei.iot.smartiot.repository.EventRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventRuleService {

    private final EventRuleRepository ruleRepository;
    private final EventRuleConditionRepository conditionRepository;
    private final EventNotificationTargetRepository targetRepository;
    private final EventNotificationChannelRepository channelRepository;

    /**
     * 建立規則 + 條件 (FN-07-013)。
     */
    @Transactional
    public EventRuleResponse create(EventRuleRequest request) {
        EventRule rule = EventRule.builder()
                .ruleName(request.getRuleName())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .targetScope(request.getTargetScope())
                .formatId(request.getFormatId())
                .conditionLogic(request.getConditionLogic() != null ? request.getConditionLogic() : ConditionLogic.AND)
                .suppressDurationMin(request.getSuppressDurationMin() != null ? request.getSuppressDurationMin() : 30)
                .autoCreateTicket(request.getAutoCreateTicket() != null ? request.getAutoCreateTicket() : false)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        rule = ruleRepository.save(rule);

        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            saveConditions(rule.getId(), request.getConditions());
        }

        return toResponse(rule);
    }

    /**
     * 列表 (FN-07-013)。
     */
    public Page<EventRuleResponse> list(Pageable pageable) {
        return ruleRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * 更新規則 (FN-07-013)。
     */
    @Transactional
    public EventRuleResponse update(Long id, EventRuleRequest request) {
        EventRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND));

        rule.setRuleName(request.getRuleName());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        rule.setSeverity(request.getSeverity());
        if (request.getTargetScope() != null) rule.setTargetScope(request.getTargetScope());
        if (request.getFormatId() != null) rule.setFormatId(request.getFormatId());
        if (request.getConditionLogic() != null) rule.setConditionLogic(request.getConditionLogic());
        if (request.getSuppressDurationMin() != null) rule.setSuppressDurationMin(request.getSuppressDurationMin());
        if (request.getAutoCreateTicket() != null) rule.setAutoCreateTicket(request.getAutoCreateTicket());
        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());

        rule = ruleRepository.save(rule);

        // 若帶 conditions 一併更新
        if (request.getConditions() != null) {
            conditionRepository.deleteByRuleId(id);
            saveConditions(id, request.getConditions());
        }

        return toResponse(rule);
    }

    /**
     * 刪除規則 (CASCADE conditions/targets/channels)。
     */
    @Transactional
    public void delete(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND);
        }
        conditionRepository.deleteByRuleId(id);
        targetRepository.deleteByRuleId(id);
        channelRepository.deleteByRuleId(id);
        ruleRepository.deleteById(id);
    }

    /**
     * 條件群組列表 (FN-07-048)。
     */
    public List<EventRuleConditionResponse> getConditions(Long ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND);
        }
        return conditionRepository.findByRuleIdOrderBySortOrder(ruleId).stream()
                .map(this::toConditionResponse)
                .toList();
    }

    /**
     * 批次更新條件群組 — 全量替換 (FN-07-048)。
     */
    @Transactional
    public List<EventRuleConditionResponse> updateConditions(Long ruleId, List<EventRuleConditionRequest> requests) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND);
        }
        conditionRepository.deleteByRuleId(ruleId);
        saveConditions(ruleId, requests);

        return conditionRepository.findByRuleIdOrderBySortOrder(ruleId).stream()
                .map(this::toConditionResponse)
                .toList();
    }

    private void saveConditions(Long ruleId, List<EventRuleConditionRequest> requests) {
        List<EventRuleCondition> conditions = requests.stream()
                .map(r -> EventRuleCondition.builder()
                        .ruleId(ruleId)
                        .conditionGroup(r.getConditionGroup() != null ? r.getConditionGroup() : 1)
                        .field(r.getField())
                        .operator(r.getOperator())
                        .thresholdValue(r.getThresholdValue())
                        .sortOrder(r.getSortOrder() != null ? r.getSortOrder() : 0)
                        .build())
                .toList();
        conditionRepository.saveAll(conditions);
    }

    private EventRuleResponse toResponse(EventRule rule) {
        List<EventRuleConditionResponse> conditions = conditionRepository
                .findByRuleIdOrderBySortOrder(rule.getId()).stream()
                .map(this::toConditionResponse)
                .toList();

        return EventRuleResponse.builder()
                .id(rule.getId())
                .tenantId(rule.getTenantId())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .severity(rule.getSeverity())
                .targetScope(rule.getTargetScope())
                .formatId(rule.getFormatId())
                .conditionLogic(rule.getConditionLogic())
                .suppressDurationMin(rule.getSuppressDurationMin())
                .autoCreateTicket(rule.getAutoCreateTicket())
                .enabled(rule.getEnabled())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .conditions(conditions)
                .build();
    }

    private EventRuleConditionResponse toConditionResponse(EventRuleCondition c) {
        return EventRuleConditionResponse.builder()
                .id(c.getId())
                .ruleId(c.getRuleId())
                .conditionGroup(c.getConditionGroup())
                .field(c.getField())
                .operator(c.getOperator())
                .thresholdValue(c.getThresholdValue())
                .sortOrder(c.getSortOrder())
                .build();
    }

    // ── 通知對象 (recipients) ──

    public List<EventNotificationTargetResponse> getRecipients(Long ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND);
        }
        return targetRepository.findByRuleId(ruleId).stream()
                .map(this::toTargetResponse)
                .toList();
    }

    @Transactional
    public List<EventNotificationTargetResponse> updateRecipients(Long ruleId, List<EventNotificationTargetRequest> requests) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND);
        }
        targetRepository.deleteByRuleId(ruleId);

        List<EventNotificationTarget> targets = requests.stream()
                .map(r -> EventNotificationTarget.builder()
                        .ruleId(ruleId)
                        .targetType(r.getTargetType())
                        .targetId(r.getTargetId())
                        .build())
                .toList();
        targetRepository.saveAll(targets);

        return targetRepository.findByRuleId(ruleId).stream()
                .map(this::toTargetResponse)
                .toList();
    }

    // ── 通知管道 (channels) ──

    public List<EventNotificationChannelResponse> getChannels(Long ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND);
        }
        return channelRepository.findByRuleId(ruleId).stream()
                .map(this::toChannelResponse)
                .toList();
    }

    @Transactional
    public List<EventNotificationChannelResponse> updateChannels(Long ruleId, List<EventNotificationChannelRequest> requests) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new BusinessException(ErrorCode.IOT_EVENT_RULE_NOT_FOUND);
        }
        channelRepository.deleteByRuleId(ruleId);

        List<EventNotificationChannel> channels = requests.stream()
                .map(r -> EventNotificationChannel.builder()
                        .ruleId(ruleId)
                        .channel(r.getChannel())
                        .config(r.getConfig())
                        .enabled(r.getEnabled() != null ? r.getEnabled() : true)
                        .build())
                .toList();
        channelRepository.saveAll(channels);

        return channelRepository.findByRuleId(ruleId).stream()
                .map(this::toChannelResponse)
                .toList();
    }

    private EventNotificationTargetResponse toTargetResponse(EventNotificationTarget t) {
        return EventNotificationTargetResponse.builder()
                .id(t.getId())
                .ruleId(t.getRuleId())
                .targetType(t.getTargetType())
                .targetId(t.getTargetId())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private EventNotificationChannelResponse toChannelResponse(EventNotificationChannel c) {
        return EventNotificationChannelResponse.builder()
                .id(c.getId())
                .ruleId(c.getRuleId())
                .channel(c.getChannel())
                .config(c.getConfig())
                .enabled(c.getEnabled())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
