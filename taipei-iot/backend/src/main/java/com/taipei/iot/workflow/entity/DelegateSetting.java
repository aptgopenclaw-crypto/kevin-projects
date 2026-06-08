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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "delegate_settings")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({ TenantEntityListener.class })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegateSetting implements TenantAware {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false, length = 50)
	private String tenantId;

	@Column(name = "delegator_id", nullable = false, length = 50)
	private String delegatorId;

	@Column(name = "delegate_id", nullable = false, length = 50)
	private String delegateId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "reason", length = 200)
	private String reason;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Override
	public String getTenantId() {
		return tenantId;
	}

	@Override
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

}
