package com.taipei.iot.tender.entity;

import com.taipei.iot.common.entity.BaseEntity;
import com.taipei.iot.tenant.TenantAware;
import com.taipei.iot.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "tender_award",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tender_award_key",
        columnNames = {"tenant_id", "solution", "matched_keyword", "tender_number", "award_announce_date", "award_announce_seq", "vendor_order_seq"}
    )
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TenderAward extends BaseEntity implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    // ── 搜尋來源 ──────────────────────────────────────────────────────────────
    @Column(name = "solution", length = 255)
    private String solution;

    @Column(name = "matched_keyword", length = 255)
    private String matchedKeyword;

    // ── 列表頁欄位 ────────────────────────────────────────────────────────────
    @Column(name = "agency_name", length = 500)
    private String agencyName;

    @Column(name = "tender_number", length = 255)
    private String tenderNumber;

    @Column(name = "tender_name", length = 1000)
    private String tenderName;

    @Column(name = "tender_method", length = 100)
    private String tenderMethod;

    @Column(name = "procurement_type", length = 100)
    private String procurementType;

    @Column(name = "award_announce_date")
    private LocalDate awardAnnounceDate;

    @Column(name = "award_amount_raw", length = 500)
    private String awardAmountRaw;

    @Column(name = "award_amount", precision = 18, scale = 2)
    private BigDecimal awardAmount;

    @Column(name = "award_announce_seq", length = 20)
    private String awardAnnounceSeq;

    @Column(name = "detail_url", columnDefinition = "TEXT")
    private String detailUrl;

    // ── 詳細頁欄位（機關資料）────────────────────────────────────────────────
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

    // ── 詳細頁欄位（採購資料）────────────────────────────────────────────────
    @Column(name = "tender_category", columnDefinition = "TEXT")
    private String tenderCategory;

    @Column(name = "procurement_amount_range", length = 100)
    private String procurementAmountRange;

    // ── 詳細頁欄位（決標資料）────────────────────────────────────────────────
    @Column(name = "award_method", length = 100)
    private String awardMethod;

    @Column(name = "has_base_price")
    private Boolean hasBasePrice;

    @Column(name = "award_date")
    private LocalDate awardDate;

    @Column(name = "performance_period", length = 500)
    private String performancePeriod;

    @Column(name = "performance_location", length = 500)
    private String performanceLocation;

    // ── 詳細頁欄位（得標廠商資料，每廠商一列）────────────────────────────────
    @Column(name = "vendor_order_seq", nullable = false)
    private Integer vendorOrderSeq;

    @Column(name = "vendor_name", length = 500)
    private String vendorName;

    @Column(name = "vendor_tax_id", length = 50)
    private String vendorTaxId;

    @Column(name = "vendor_address", length = 500)
    private String vendorAddress;

    @Column(name = "vendor_phone", length = 255)
    private String vendorPhone;

    @Column(name = "vendor_award_amount_raw", length = 500)
    private String vendorAwardAmountRaw;

    @Column(name = "vendor_award_amount", precision = 18, scale = 2)
    private BigDecimal vendorAwardAmount;

    // ── 稽核 ─────────────────────────────────────────────────────────────────
    @Column(name = "scraped_at", nullable = false)
    private LocalDateTime scrapedAt;
}
