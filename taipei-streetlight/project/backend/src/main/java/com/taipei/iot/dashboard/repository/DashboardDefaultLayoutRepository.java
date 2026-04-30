package com.taipei.iot.dashboard.repository;

import com.taipei.iot.dashboard.entity.DashboardDefaultLayout;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardDefaultLayoutRepository extends JpaRepository<DashboardDefaultLayout, Long>, TenantScopedRepository {

    Optional<DashboardDefaultLayout> findByTenantIdAndRoleType(String tenantId, String roleType);

    Optional<DashboardDefaultLayout> findByTenantIdAndRoleTypeIsNull(String tenantId);
}
