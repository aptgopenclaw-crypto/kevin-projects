package com.taipei.iot.dashboard.repository;

import com.taipei.iot.dashboard.entity.DashboardLayout;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardLayoutRepository extends JpaRepository<DashboardLayout, Long>, TenantScopedRepository {

    Optional<DashboardLayout> findByTenantIdAndUserId(String tenantId, String userId);

    void deleteByTenantIdAndUserId(String tenantId, String userId);
}
