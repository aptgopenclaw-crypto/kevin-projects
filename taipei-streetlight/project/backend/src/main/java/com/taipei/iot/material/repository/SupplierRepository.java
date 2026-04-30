package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.Supplier;
import com.taipei.iot.material.enums.SupplierStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long>, TenantScopedRepository {

    @Query("""
        SELECT s FROM Supplier s
        WHERE (:status IS NULL OR s.status = :status)
          AND (CAST(:keyword AS string) IS NULL OR LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(s.supplierName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        ORDER BY s.createdAt DESC
        """)
    Page<Supplier> findByFilters(@Param("status") SupplierStatus status,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    List<Supplier> findByStatus(SupplierStatus status);
}
