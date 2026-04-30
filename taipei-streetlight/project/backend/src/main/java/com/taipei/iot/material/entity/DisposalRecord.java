package com.taipei.iot.material.entity;

import com.taipei.iot.material.enums.DisposalType;
import com.taipei.iot.tenant.TenantAware;
import com.taipei.iot.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "disposal_records")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DisposalRecord implements TenantAware {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "material_spec_id", nullable = false)
    private Long materialSpecId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposal_type", nullable = false, length = 20)
    private DisposalType disposalType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "disposed_by", length = 50)
    private String disposedBy;

    @Column(name = "disposed_at", nullable = false)
    private LocalDateTime disposedAt;

    @Override
    public String getTenantId() { return tenantId; }

    @Override
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
