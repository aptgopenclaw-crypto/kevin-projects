package com.taipei.iot.workflow.repository;

import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.enums.WorkflowStatus;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long>, TenantScopedRepository {

    Optional<WorkflowInstance> findByTicketTypeAndTicketId(String ticketType, Long ticketId);

    @Query("""
        SELECT wi FROM WorkflowInstance wi
        WHERE wi.status = :status
          AND wi.assignedTo IN :assigneeIds
        ORDER BY wi.updatedAt DESC
        """)
    Page<WorkflowInstance> findPendingByAssignees(
            @Param("assigneeIds") Collection<String> assigneeIds,
            @Param("status") WorkflowStatus status,
            Pageable pageable);
}
