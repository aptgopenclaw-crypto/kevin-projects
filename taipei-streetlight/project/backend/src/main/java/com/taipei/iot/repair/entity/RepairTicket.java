package com.taipei.iot.repair.entity;

import com.taipei.iot.repair.enums.RepairTicketPriority;
import com.taipei.iot.repair.enums.RepairTicketSource;
import com.taipei.iot.repair.enums.RepairTicketStatus;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "repair_tickets")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RepairTicket implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "ticket_number", nullable = false, length = 50)
    private String ticketNumber;

    @Column(name = "fault_ticket_id")
    private Long faultTicketId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "circuit_id")
    private Long circuitId;

    @Column(name = "contract_id")
    private Long contractId;

    // 報修資訊
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private RepairTicketSource source;

    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    @Column(name = "reporter_phone", length = 50)
    private String reporterPhone;

    @Column(name = "reporter_email", length = 200)
    private String reporterEmail;

    @Column(name = "report_address", columnDefinition = "TEXT")
    private String reportAddress;

    @Column(name = "report_description", columnDefinition = "TEXT")
    private String reportDescription;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    // 維護資訊
    @Column(name = "fault_category", length = 50)
    private String faultCategory;

    @Column(name = "fault_cause", length = 50)
    private String faultCause;

    @Column(name = "repair_description", columnDefinition = "TEXT")
    private String repairDescription;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 狀態
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RepairTicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private RepairTicketPriority priority;

    @Column(name = "dept_id")
    private Long deptId;

    // 審計
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
