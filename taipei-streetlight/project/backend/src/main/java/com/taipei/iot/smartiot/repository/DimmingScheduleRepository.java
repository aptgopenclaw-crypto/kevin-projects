package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.DimmingSchedule;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DimmingScheduleRepository extends JpaRepository<DimmingSchedule, Long>, TenantScopedRepository {

    List<DimmingSchedule> findByEnabledTrue();
}
