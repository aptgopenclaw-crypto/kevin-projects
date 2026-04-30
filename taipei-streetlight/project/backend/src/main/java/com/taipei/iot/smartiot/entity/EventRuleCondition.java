package com.taipei.iot.smartiot.entity;

import com.taipei.iot.smartiot.enums.ConditionOperator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_rule_conditions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EventRuleCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "condition_group", nullable = false)
    @Builder.Default
    private Integer conditionGroup = 1;

    @Column(name = "field", nullable = false, length = 100)
    private String field;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 10)
    private ConditionOperator operator;

    @Column(name = "threshold_value", nullable = false, length = 100)
    private String thresholdValue;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
