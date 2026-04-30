package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.Warehouse;
import com.taipei.iot.material.enums.WarehouseStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long>, TenantScopedRepository {

    @Query("""
        SELECT w FROM Warehouse w
        WHERE (:status IS NULL OR w.status = :status)
          AND (CAST(:keyword AS string) IS NULL OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        ORDER BY w.createdAt DESC
        """)
    Page<Warehouse> findByFilters(@Param("status") WarehouseStatus status,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    List<Warehouse> findByStatus(WarehouseStatus status);
}
