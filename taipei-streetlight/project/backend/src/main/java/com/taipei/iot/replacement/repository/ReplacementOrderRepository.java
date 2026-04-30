package com.taipei.iot.replacement.repository;

import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderType;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReplacementOrderRepository extends JpaRepository<ReplacementOrder, Long>, TenantScopedRepository {

    @Query("""
        SELECT o FROM ReplacementOrder o
        WHERE (:status IS NULL OR o.status = :status)
          AND (:orderType IS NULL OR o.orderType = :orderType)
          AND (:contractId IS NULL OR o.contractId = :contractId)
          AND (:keyword IS NULL OR o.orderNumber LIKE %:keyword%
               OR o.location LIKE %:keyword%
               OR o.assignedContractor LIKE %:keyword%)
          AND (:visibleDeptIds IS NULL OR o.deptId IN :visibleDeptIds)
          AND (CAST(:dateFrom AS date) IS NULL OR CAST(o.createdAt AS date) >= :dateFrom)
          AND (CAST(:dateTo AS date) IS NULL OR CAST(o.createdAt AS date) <= :dateTo)
        ORDER BY o.createdAt DESC
        """)
    Page<ReplacementOrder> findByFilters(
            @Param("status") ReplacementOrderStatus status,
            @Param("orderType") ReplacementOrderType orderType,
            @Param("contractId") Long contractId,
            @Param("keyword") String keyword,
            @Param("visibleDeptIds") List<Long> visibleDeptIds,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable);
}
