package com.taipei.iot.replacement.entity;

import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderType;
import com.taipei.iot.tenant.TenantAware;
import com.taipei.iot.tenant.TenantEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "replacement_orders")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementOrder implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "repair_ticket_id")
    private Long repairTicketId;

    @Column(name = "contract_id")
    private Long contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 30)
    private ReplacementOrderType orderType;

    @Column(name = "dispatch_reason")
    private String dispatchReason;

    @Column(name = "location")
    private String location;

    @Column(name = "expected_quantity")
    private Integer expectedQuantity;

    @Column(name = "work_period_start")
    private LocalDate workPeriodStart;

    @Column(name = "work_period_end")
    private LocalDate workPeriodEnd;

    @Column(name = "assigned_contractor", length = 200)
    private String assignedContractor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReplacementOrderStatus status;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public String getTenantId() { return tenantId; }

    @Override
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
