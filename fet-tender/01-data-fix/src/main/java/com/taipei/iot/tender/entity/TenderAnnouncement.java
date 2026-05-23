package com.taipei.iot.tender.entity;

import com.taipei.iot.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "tender_announcement",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tender_ann_key",
        columnNames = {"tenant_id", "solution", "matched_keyword", "tender_number", "announcement_date"}
    )
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TenderAnnouncement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "solution", length = 255)
    private String solution;

    @Column(name = "matched_keyword", length = 255)
    private String matchedKeyword;

    @Column(name = "agency_name", length = 500)
    private String agencyName;

    @Column(name = "tender_number", length = 255)
    private String tenderNumber;

    @Column(name = "tender_name", length = 1000)
    private String tenderName;

    @Column(name = "transmission_count")
    private Integer transmissionCount;

    @Column(name = "tender_method", length = 100)
    private String tenderMethod;

    @Column(name = "procurement_type", length = 100)
    private String procurementType;

    @Column(name = "announcement_date")
    private LocalDate announcementDate;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "budget_amount_raw", length = 500)
    private String budgetAmountRaw;

    @Column(name = "budget_amount", precision = 18, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "detail_url", columnDefinition = "TEXT")
    private String detailUrl;

    @Column(name = "agency_code", length = 50)
    private String agencyCode;

    @Column(name = "unit_name", length = 255)
    private String unitName;

    @Column(name = "agency_address", length = 500)
    private String agencyAddress;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(name = "contact_phone", length = 255)
    private String contactPhone;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "tender_category", columnDefinition = "TEXT")
    private String tenderCategory;

    @Column(name = "procurement_amount_range", length = 100)
    private String procurementAmountRange;

    @Column(name = "handling_method", length = 100)
    private String handlingMethod;

    @Column(name = "award_method", length = 100)
    private String awardMethod;

    @Column(name = "tender_status", length = 200)
    private String tenderStatus;

    @Column(name = "opening_time")
    private LocalDateTime openingTime;

    @Column(name = "opening_location", length = 500)
    private String openingLocation;

    @Column(name = "has_base_price")
    private Boolean hasBasePrice;

    @Column(name = "performance_location", length = 500)
    private String performanceLocation;

    @Column(name = "scraped_at", nullable = false)
    private LocalDateTime scrapedAt;
}
