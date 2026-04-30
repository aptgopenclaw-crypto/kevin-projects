package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.ApprovedMaterial;
import com.taipei.iot.material.enums.ApprovedMaterialStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovedMaterialRepository extends JpaRepository<ApprovedMaterial, Long>, TenantScopedRepository {

    @Query("""
        SELECT am FROM ApprovedMaterial am
        LEFT JOIN FETCH am.materialSpec ms
        WHERE (:status IS NULL OR am.status = :status)
          AND (:materialSpecId IS NULL OR am.materialSpecId = :materialSpecId)
          AND (CAST(:keyword AS string) IS NULL
               OR LOWER(am.materialNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(am.brand) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(am.model) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        ORDER BY am.createdAt DESC
        """)
    Page<ApprovedMaterial> findByFilters(@Param("status") ApprovedMaterialStatus status,
                                          @Param("materialSpecId") Long materialSpecId,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);

    boolean existsByTenantIdAndMaterialNumber(String tenantId, String materialNumber);
}
