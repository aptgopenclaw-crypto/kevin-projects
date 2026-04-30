package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.AlertHistory;
import com.taipei.iot.smartiot.enums.AlertSeverity;
import com.taipei.iot.smartiot.enums.AlertStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long>, TenantScopedRepository {

    Page<AlertHistory> findByStatusOrderByTriggeredAtDesc(AlertStatus status, Pageable pageable);

    @Query("SELECT a FROM AlertHistory a WHERE " +
           "(:status IS NULL OR a.status = :status) " +
           "AND (:severity IS NULL OR a.severity = :severity) " +
           "ORDER BY a.triggeredAt DESC")
    Page<AlertHistory> findByFilters(@Param("status") AlertStatus status,
                                     @Param("severity") AlertSeverity severity,
                                     Pageable pageable);

    @Query("SELECT a FROM AlertHistory a WHERE a.deviceId = :deviceId AND a.ruleId = :ruleId " +
           "ORDER BY a.triggeredAt DESC LIMIT 1")
    Optional<AlertHistory> findLatestByDeviceIdAndRuleId(
            @Param("deviceId") Long deviceId, @Param("ruleId") Long ruleId);

    Page<AlertHistory> findByDeviceIdOrderByTriggeredAtDesc(Long deviceId, Pageable pageable);

    long countByStatusAndTriggeredAtAfter(AlertStatus status, LocalDateTime after);
}
