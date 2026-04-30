package com.taipei.iot.smartiot.dto;

import com.taipei.iot.smartiot.enums.ConditionOperator;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventRuleConditionResponse {

    private Long id;
    private Long ruleId;
    private Integer conditionGroup;
    private String field;
    private ConditionOperator operator;
    private String thresholdValue;
    private Integer sortOrder;
}
