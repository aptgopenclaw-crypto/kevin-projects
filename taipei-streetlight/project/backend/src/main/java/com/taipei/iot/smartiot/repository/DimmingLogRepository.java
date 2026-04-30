package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.DimmingLog;
import com.taipei.iot.smartiot.enums.DimmingResult;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DimmingLogRepository extends JpaRepository<DimmingLog, Long>, TenantScopedRepository {

    Page<DimmingLog> findByDeviceIdOrderBySentAtDesc(Long deviceId, Pageable pageable);

    Page<DimmingLog> findAllByOrderBySentAtDesc(Pageable pageable);

    @Query("SELECT d FROM DimmingLog d WHERE d.result = :result AND d.sentAt < :cutoff")
    List<DimmingLog> findPendingBefore(@Param("result") DimmingResult result,
                                       @Param("cutoff") LocalDateTime cutoff);

    Optional<DimmingLog> findFirstByDeviceIdAndResultOrderBySentAtDesc(Long deviceId, DimmingResult result);
}
