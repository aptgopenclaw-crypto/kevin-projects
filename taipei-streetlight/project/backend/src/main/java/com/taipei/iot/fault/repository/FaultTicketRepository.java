package com.taipei.iot.fault.repository;

import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.enums.FaultTicketStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface FaultTicketRepository extends JpaRepository<FaultTicket, Long>, TenantScopedRepository {

    @Query("""
        SELECT f FROM FaultTicket f
        WHERE (:status IS NULL OR f.status = :status)
          AND (:keyword IS NULL OR LOWER(CAST(f.description AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        ORDER BY f.reportedAt DESC
        """)
    Page<FaultTicket> findByFilters(
            @Param("status") FaultTicketStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
        SELECT COUNT(f) FROM FaultTicket f
        WHERE f.circuitId = :circuitId
          AND f.reportedAt >= :since
          AND f.status != com.taipei.iot.fault.enums.FaultTicketStatus.MERGED
        """)
    long countRecentByCircuit(@Param("circuitId") Long circuitId,
                               @Param("since") LocalDateTime since);

    long countByDeviceIdAndStatusIn(Long deviceId, java.util.Collection<FaultTicketStatus> statuses);
}
