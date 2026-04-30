package com.taipei.iot.repair.repository;

import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepairTicketRepository extends JpaRepository<RepairTicket, Long>, TenantScopedRepository {

    Optional<RepairTicket> findByTicketNumberAndReporterPhone(String ticketNumber, String reporterPhone);

    @Query("""
        SELECT t FROM RepairTicket t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:source IS NULL OR t.source = :source)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:deptId IS NULL OR t.deptId = :deptId)
          AND (:keyword IS NULL OR t.ticketNumber LIKE %:keyword%
               OR t.reporterName LIKE %:keyword%
               OR t.reportDescription LIKE %:keyword%)
          AND (:visibleDeptIds IS NULL OR t.deptId IN :visibleDeptIds)
        ORDER BY t.createdAt DESC
        """)
    Page<RepairTicket> findByFilters(
            @Param("status") RepairTicketStatus status,
            @Param("source") RepairTicketSource source,
            @Param("priority") RepairTicketPriority priority,
            @Param("deptId") Long deptId,
            @Param("keyword") String keyword,
            @Param("visibleDeptIds") List<Long> visibleDeptIds,
            Pageable pageable);
}
