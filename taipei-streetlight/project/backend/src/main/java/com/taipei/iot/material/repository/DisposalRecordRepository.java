package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.DisposalRecord;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisposalRecordRepository extends JpaRepository<DisposalRecord, Long>, TenantScopedRepository {

    Page<DisposalRecord> findAllByOrderByDisposedAtDesc(Pageable pageable);
}
