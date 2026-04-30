package com.taipei.iot.material.entity;

import com.taipei.iot.material.enums.ApprovedMaterialStatus;
import com.taipei.iot.tenant.TenantAware;
import com.taipei.iot.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "approved_materials")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ApprovedMaterial implements TenantAware {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "material_spec_id", nullable = false)
    private Long materialSpecId;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "material_number", nullable = false, length = 100)
    private String materialNumber;

    @Column(name = "approval_date", nullable = false)
    private LocalDate approvalDate;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "brand", length = 200)
    private String brand;

    @Column(name = "model", length = 200)
    private String model;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "spec_details", columnDefinition = "jsonb")
    private Map<String, Object> specDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovedMaterialStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_spec_id", insertable = false, updatable = false)
    private MaterialSpec materialSpec;

    @Override
    public String getTenantId() { return tenantId; }

    @Override
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
