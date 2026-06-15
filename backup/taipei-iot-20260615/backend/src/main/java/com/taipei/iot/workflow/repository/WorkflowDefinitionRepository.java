package com.taipei.iot.workflow.repository;

import com.taipei.iot.tenant.TenantScopedRepository;
import com.taipei.iot.workflow.entity.WorkflowDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowDefinitionRepository
		extends JpaRepository<WorkflowDefinitionEntity, Long>, TenantScopedRepository {

	Optional<WorkflowDefinitionEntity> findByCodeAndEnabledTrue(String code);

}
