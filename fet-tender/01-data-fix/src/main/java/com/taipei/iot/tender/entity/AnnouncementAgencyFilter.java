package com.taipei.iot.tender.entity;

import com.taipei.iot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "announcement_agency_filters",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ann_af",
        columnNames = {"tenant_id", "solution", "agency_keyword"}
    )
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementAgencyFilter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "solution", nullable = false, length = 255)
    private String solution;

    @Column(name = "agency_keyword", nullable = false, length = 255)
    private String agencyKeyword;

    @Column(name = "is_org_only_search", nullable = false)
    @Builder.Default
    private Boolean isOrgOnlySearch = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
