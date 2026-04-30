package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.EventNotificationTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventNotificationTargetRepository extends JpaRepository<EventNotificationTarget, Long> {

    List<EventNotificationTarget> findByRuleId(Long ruleId);

    void deleteByRuleId(Long ruleId);
}
