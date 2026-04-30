package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.InventoryAdjustment;
import com.taipei.iot.material.enums.AdjustmentType;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long>, TenantScopedRepository {

    @Query("""
        SELECT ia FROM InventoryAdjustment ia
        WHERE (:type IS NULL OR ia.adjustmentType = :type)
        ORDER BY ia.adjustedAt DESC
        """)
    Page<InventoryAdjustment> findByFilters(@Param("type") AdjustmentType type, Pageable pageable);
}
