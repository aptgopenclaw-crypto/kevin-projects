package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.ConditionLogic;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class EventRuleResponse {

    private Long id;
    private String tenantId;
    private String ruleName;
    private String description;
    private AlertSeverity severity;
    private Map<String, Object> targetScope;
    private Long formatId;
    private ConditionLogic conditionLogic;
    private Integer suppressDurationMin;
    private Boolean autoCreateTicket;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EventRuleConditionResponse> conditions;
}
