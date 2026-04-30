package com.taipei.iot.replacement.repository;

import com.taipei.iot.replacement.entity.LightPoleNumber;
import com.taipei.iot.replacement.enums.PoleNumberStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LightPoleNumberRepository extends JpaRepository<LightPoleNumber, Long>, TenantScopedRepository {

    boolean existsByTenantIdAndPoleNumber(String tenantId, String poleNumber);

    Optional<LightPoleNumber> findByPoleNumber(String poleNumber);

    @Query("""
        SELECT p FROM LightPoleNumber p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:keyword IS NULL OR p.poleNumber LIKE %:keyword%)
        ORDER BY p.createdAt DESC
        """)
    Page<LightPoleNumber> findByFilters(
            @Param("status") PoleNumberStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
