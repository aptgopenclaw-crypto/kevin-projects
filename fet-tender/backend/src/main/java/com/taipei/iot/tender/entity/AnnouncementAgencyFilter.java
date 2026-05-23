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
    name = "announcement_agency_filters",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ann_af",
        columnNames = {"tenant_id", "solution", "agency_keyword"}
    )
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementAgencyFilter extends BaseEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "solution", nullable = false, length = 255)
    private String solution;

    /**
     * 機關關鍵字。
     * is_org_only_search=FALSE 時：作為機關名稱的事後過濾條件（contains）。
     * is_org_only_search=TRUE  時：直接以此字串搜尋機關名稱欄位（ESG-建研所補助 模式）。
     */
    @Column(name = "agency_keyword", nullable = false, length = 255)
    private String agencyKeyword;

    @Column(name = "is_org_only_search", nullable = false)
    @Builder.Default
    private Boolean isOrgOnlySearch = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
