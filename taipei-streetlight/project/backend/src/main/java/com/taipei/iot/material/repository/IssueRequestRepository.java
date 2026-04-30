package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.IssueRequest;
import com.taipei.iot.material.enums.IssueRequestStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRequestRepository extends JpaRepository<IssueRequest, Long>, TenantScopedRepository {

    @Query("""
        SELECT ir FROM IssueRequest ir
        WHERE (:status IS NULL OR ir.status = :status)
          AND (CAST(:keyword AS string) IS NULL
               OR LOWER(ir.requestNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        ORDER BY ir.createdAt DESC
        """)
    Page<IssueRequest> findByFilters(@Param("status") IssueRequestStatus status,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    java.util.List<IssueRequest> findByReplacementOrderId(Long replacementOrderId);
}
