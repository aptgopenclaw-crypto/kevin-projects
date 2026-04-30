package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.PurchaseOrder;
import com.taipei.iot.material.enums.PurchaseOrderStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, TenantScopedRepository {

    @Query("""
        SELECT po FROM PurchaseOrder po
        LEFT JOIN FETCH po.supplier s
        WHERE (:status IS NULL OR po.status = :status)
          AND (CAST(:keyword AS string) IS NULL
               OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR (s IS NOT NULL AND LOWER(s.supplierName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))))
        ORDER BY po.createdAt DESC
        """)
    Page<PurchaseOrder> findByFilters(@Param("status") PurchaseOrderStatus status,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);
}
