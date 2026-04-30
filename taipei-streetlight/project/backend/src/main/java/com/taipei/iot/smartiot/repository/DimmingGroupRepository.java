package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.DimmingGroup;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimmingGroupRepository extends JpaRepository<DimmingGroup, Long>, TenantScopedRepository {
}
