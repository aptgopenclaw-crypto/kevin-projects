package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.EventRuleCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRuleConditionRepository extends JpaRepository<EventRuleCondition, Long> {

    List<EventRuleCondition> findByRuleIdOrderBySortOrder(Long ruleId);

    void deleteByRuleId(Long ruleId);

    List<EventRuleCondition> findByField(String field);
}
