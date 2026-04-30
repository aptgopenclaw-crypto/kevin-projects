package com.taipei.iot.kpi.repository;

import com.taipei.iot.kpi.entity.KpiResult;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KpiResultRepository extends JpaRepository<KpiResult, Long>, TenantScopedRepository {

    @Query("""
        SELECT r FROM KpiResult r
        WHERE (:periodYear IS NULL OR r.periodYear = :periodYear)
          AND (:periodMonth IS NULL OR r.periodMonth = :periodMonth)
          AND (:contractId IS NULL OR r.contractId = :contractId)
          AND (:indicatorId IS NULL OR r.indicator.id = :indicatorId)
        """)
    Page<KpiResult> findByFilters(
            @Param("periodYear") Integer periodYear,
            @Param("periodMonth") Integer periodMonth,
            @Param("contractId") Long contractId,
            @Param("indicatorId") Long indicatorId,
            Pageable pageable);

    List<KpiResult> findByTenantIdAndPeriodYearAndPeriodMonth(
            String tenantId, Integer periodYear, Integer periodMonth);

    List<KpiResult> findByTenantIdAndPeriodYearAndPeriodMonthAndContractId(
            String tenantId, Integer periodYear, Integer periodMonth, Long contractId);

    List<KpiResult> findByTenantIdAndPeriodYearAndContractId(
            String tenantId, Integer periodYear, Long contractId);

    Optional<KpiResult> findByTenantIdAndIndicatorIdAndPeriodYearAndPeriodMonthAndContractId(
            String tenantId, Long indicatorId, Integer periodYear, Integer periodMonth, Long contractId);

    /** 全市層級結果 */
    @Query("""
        SELECT r FROM KpiResult r
        WHERE r.tenantId = :tenantId
          AND r.indicator.id = :indicatorId
          AND r.periodYear = :periodYear
          AND r.periodMonth = :periodMonth
          AND r.contractId IS NULL
        """)
    Optional<KpiResult> findCityLevel(
            @Param("tenantId") String tenantId,
            @Param("indicatorId") Long indicatorId,
            @Param("periodYear") Integer periodYear,
            @Param("periodMonth") Integer periodMonth);

    /** 廠商查詢: 依 contractId 列出多期結果 */
    List<KpiResult> findByContractIdAndPeriodYearOrderByPeriodMonthAsc(
            Long contractId, Integer periodYear);
}
