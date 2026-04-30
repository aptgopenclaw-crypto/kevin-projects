package com.taipei.iot.kpi.repository;

import com.taipei.iot.kpi.entity.KpiRawData;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KpiRawDataRepository extends JpaRepository<KpiRawData, Long>, TenantScopedRepository {

    @Query("""
        SELECT d FROM KpiRawData d
        WHERE (:indicatorId IS NULL OR d.indicator.id = :indicatorId)
          AND (:periodYear IS NULL OR d.periodYear = :periodYear)
          AND (:periodMonth IS NULL OR d.periodMonth = :periodMonth)
          AND (:contractId IS NULL OR d.contractId = :contractId)
        """)
    Page<KpiRawData> findByFilters(
            @Param("indicatorId") Long indicatorId,
            @Param("periodYear") Integer periodYear,
            @Param("periodMonth") Integer periodMonth,
            @Param("contractId") Long contractId,
            Pageable pageable);

    List<KpiRawData> findByIndicatorIdAndPeriodYearAndPeriodMonth(
            Long indicatorId, Integer periodYear, Integer periodMonth);

    Optional<KpiRawData> findByTenantIdAndIndicatorIdAndPeriodYearAndPeriodMonthAndContractId(
            String tenantId, Long indicatorId, Integer periodYear, Integer periodMonth, Long contractId);

    /** 全市層級 (contract_id IS NULL) 的 raw data */
    @Query("""
        SELECT d FROM KpiRawData d
        WHERE d.tenantId = :tenantId
          AND d.indicator.id = :indicatorId
          AND d.periodYear = :periodYear
          AND d.periodMonth = :periodMonth
          AND d.contractId IS NULL
        """)
    Optional<KpiRawData> findCityLevel(
            @Param("tenantId") String tenantId,
            @Param("indicatorId") Long indicatorId,
            @Param("periodYear") Integer periodYear,
            @Param("periodMonth") Integer periodMonth);
}
