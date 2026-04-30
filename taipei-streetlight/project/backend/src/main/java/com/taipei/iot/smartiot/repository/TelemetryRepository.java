package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.Telemetry;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long>, TenantScopedRepository {

    Page<Telemetry> findByDeviceIdAndTimeBetweenOrderByTimeDesc(
            Long deviceId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT t FROM Telemetry t WHERE t.deviceId = :deviceId ORDER BY t.time DESC LIMIT 1")
    Optional<Telemetry> findLatestByDeviceId(@Param("deviceId") Long deviceId);

    List<Telemetry> findByDeviceIdInAndTimeAfter(List<Long> deviceIds, LocalDateTime after);
}
