package com.taipei.iot.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_steps_template")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowStepsTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_type", nullable = false, length = 50)
    private String workflowType;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_code", nullable = false, length = 50)
    private String stepCode;

    @Column(name = "step_name", nullable = false, length = 200)
    private String stepName;

    @Column(name = "required_role", length = 50)
    private String requiredRole;

    @Column(name = "auto_action", length = 50)
    private String autoAction;

    @Column(name = "timeout_hours")
    private Integer timeoutHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
