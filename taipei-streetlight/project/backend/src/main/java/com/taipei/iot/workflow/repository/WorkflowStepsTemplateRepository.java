package com.taipei.iot.workflow.repository;

import com.taipei.iot.workflow.entity.WorkflowStepsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepsTemplateRepository extends JpaRepository<WorkflowStepsTemplate, Long> {

    List<WorkflowStepsTemplate> findByWorkflowTypeOrderByStepOrder(String workflowType);
}
