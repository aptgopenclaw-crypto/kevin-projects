package com.taipei.iot.repair.repository;

import com.taipei.iot.repair.entity.InspectionTask;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionTaskRepository extends JpaRepository<InspectionTask, Long>, TenantScopedRepository {

    Page<InspectionTask> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
