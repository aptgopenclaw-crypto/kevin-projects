package com.taipei.iot.repair.repository;

import com.taipei.iot.repair.entity.InspectionRecord;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, Long>, TenantScopedRepository {

    Page<InspectionRecord> findByTaskIdOrderByInspectionDateDesc(Long taskId, Pageable pageable);
}
