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
    name = "announcement_search_keywords",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ann_kw",
        columnNames = {"tenant_id", "solution", "keyword"}
    )
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementSearchKeyword extends BaseEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "solution", nullable = false, length = 255)
    private String solution;

    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
