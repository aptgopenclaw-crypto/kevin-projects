package com.taipei.iot.workflow.repository;

import com.taipei.iot.workflow.entity.WorkflowStepLog;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepLogRepository extends JpaRepository<WorkflowStepLog, Long>, TenantScopedRepository {

    List<WorkflowStepLog> findByInstanceIdOrderByActedAtAsc(Long instanceId);

    void deleteByInstanceId(Long instanceId);
}
