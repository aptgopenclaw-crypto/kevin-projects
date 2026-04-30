package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.EventNotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventNotificationChannelRepository extends JpaRepository<EventNotificationChannel, Long> {

    List<EventNotificationChannel> findByRuleIdAndEnabledTrue(Long ruleId);

    List<EventNotificationChannel> findByRuleId(Long ruleId);

    void deleteByRuleId(Long ruleId);
}
