package com.taipei.iot.tender.entity;

import com.taipei.iot.common.entity.BaseEntity;
import com.taipei.iot.tenant.TenantAware;
import com.taipei.iot.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "tender_mail_recipient",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tender_mail_recipient",
        columnNames = {"tenant_id", "email"}
    )
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TenderMailRecipient extends BaseEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
