package com.taipei.iot.kpi.repository;

import com.taipei.iot.kpi.entity.KpiPeriod;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KpiPeriodRepository extends JpaRepository<KpiPeriod, Long>, TenantScopedRepository {

    Optional<KpiPeriod> findByTenantIdAndPeriodYearAndPeriodMonth(
            String tenantId, Integer periodYear, Integer periodMonth);

    List<KpiPeriod> findByTenantIdOrderByPeriodYearDescPeriodMonthDesc(String tenantId);
}
