package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.ReceivingRecord;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReceivingRecordRepository extends JpaRepository<ReceivingRecord, Long>, TenantScopedRepository {

    @Query("""
        SELECT r FROM ReceivingRecord r
        WHERE (:poId IS NULL OR r.poId = :poId)
        ORDER BY r.createdAt DESC
        """)
    Page<ReceivingRecord> findByFilters(@Param("poId") Long poId, Pageable pageable);

    List<ReceivingRecord> findByPoId(Long poId);
}
