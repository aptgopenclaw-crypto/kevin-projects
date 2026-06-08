package com.taipei.iot.workflow.entity;

import com.taipei.iot.tenant.TenantAware;
import com.taipei.iot.tenant.TenantEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "workflow_step_logs")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({ TenantEntityListener.class })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepLog implements TenantAware {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false, length = 50)
	private String tenantId;

	@Column(name = "instance_id", nullable = false)
	private Long instanceId;

	@Column(name = "step_code", nullable = false, length = 50)
	private String stepCode;

	@Column(name = "action", nullable = false, length = 30)
	private String action;

	@Column(name = "actor_id", nullable = false, length = 50)
	private String actorId;

	@Column(name = "actor_name", length = 100)
	private String actorName;

	@Column(name = "original_assignee_id", length = 50)
	private String originalAssigneeId;

	@Column(name = "is_delegated", nullable = false)
	private Boolean isDelegated;

	@Column(name = "comment", columnDefinition = "TEXT")
	private String comment;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "attachments", columnDefinition = "jsonb")
	private List<Map<String, Object>> attachments;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_snapshot", columnDefinition = "jsonb")
	private Map<String, Object> beforeSnapshot;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_snapshot", columnDefinition = "jsonb")
	private Map<String, Object> afterSnapshot;

	@Column(name = "acted_at", nullable = false)
	private LocalDateTime actedAt;

	@Override
	public String getTenantId() {
		return tenantId;
	}

	@Override
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

}
