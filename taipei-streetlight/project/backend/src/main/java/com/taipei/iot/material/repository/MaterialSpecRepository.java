package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.MaterialSpec;
import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.material.enums.MaterialStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MaterialSpecRepository extends JpaRepository<MaterialSpec, Long>, TenantScopedRepository {

    @Query("""
        SELECT m FROM MaterialSpec m
        WHERE (:category IS NULL OR m.category = :category)
          AND (:status IS NULL OR m.status = :status)
          AND (CAST(:keyword AS string) IS NULL OR LOWER(m.specCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(m.specName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        ORDER BY m.createdAt DESC
        """)
    Page<MaterialSpec> findByFilters(@Param("category") MaterialCategory category,
                                      @Param("status") MaterialStatus status,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    Optional<MaterialSpec> findByTenantIdAndSpecCode(String tenantId, String specCode);
}
