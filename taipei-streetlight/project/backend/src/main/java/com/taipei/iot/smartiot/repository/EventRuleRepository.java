package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.EventRule;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRuleRepository extends JpaRepository<EventRule, Long>, TenantScopedRepository {

    List<EventRule> findByEnabledTrueAndFormatId(Long formatId);

    List<EventRule> findByEnabledTrue();
}
