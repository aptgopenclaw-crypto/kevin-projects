package com.taipei.iot.kpi.repository;

import com.taipei.iot.kpi.entity.KpiIndicator;
import com.taipei.iot.kpi.enums.KpiCategory;
import com.taipei.iot.kpi.enums.KpiIndicatorStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KpiIndicatorRepository extends JpaRepository<KpiIndicator, Long>, TenantScopedRepository {

    Optional<KpiIndicator> findByTenantIdAndIndicatorCode(String tenantId, String indicatorCode);

    @Query("""
        SELECT k FROM KpiIndicator k
        WHERE (:category IS NULL OR k.category = :category)
          AND (:status IS NULL OR k.status = :status)
          AND (:keyword IS NULL OR LOWER(k.indicatorCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(k.indicatorName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        """)
    Page<KpiIndicator> findByFilters(
            @Param("category") KpiCategory category,
            @Param("status") KpiIndicatorStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    List<KpiIndicator> findByTenantIdAndStatus(String tenantId, KpiIndicatorStatus status);
}
