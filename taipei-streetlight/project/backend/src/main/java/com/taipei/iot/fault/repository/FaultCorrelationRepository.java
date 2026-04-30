package com.taipei.iot.fault.repository;

import com.taipei.iot.fault.entity.FaultCorrelation;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaultCorrelationRepository extends JpaRepository<FaultCorrelation, Long>, TenantScopedRepository {

    Page<FaultCorrelation> findByStatusOrderByDetectedAtDesc(String status, Pageable pageable);
}
