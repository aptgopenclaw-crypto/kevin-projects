package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.TenderAward;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TenderAwardResponse {

    private Long id;
    private String solution;
    private String matchedKeyword;

    // 列表頁欄位
    private String agencyName;
    private String tenderNumber;
    private String tenderName;
    private String tenderMethod;
    private String procurementType;
    private LocalDate awardAnnounceDate;
    private String awardAmountRaw;
    private BigDecimal awardAmount;
    private String awardAnnounceSeq;
    private String detailUrl;

    // 詳細頁：機關資料
    private String agencyCode;
    private String unitName;
    private String agencyAddress;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;

    // 詳細頁：採購資料
    private String tenderCategory;
    private String procurementAmountRange;

    // 詳細頁：決標資料
    private String awardMethod;
    private Boolean hasBasePrice;
    private LocalDate awardDate;
    private String performancePeriod;
    private String performanceLocation;

    // 詳細頁：廠商資料
    private Integer vendorOrderSeq;
    private String vendorName;
    private String vendorTaxId;
    private String vendorAddress;
    private String vendorPhone;
    private String vendorAwardAmountRaw;
    private BigDecimal vendorAwardAmount;

    private LocalDateTime scrapedAt;

    public static TenderAwardResponse from(TenderAward e) {
        return TenderAwardResponse.builder()
                .id(e.getId())
                .solution(e.getSolution())
                .matchedKeyword(e.getMatchedKeyword())
                .agencyName(e.getAgencyName())
                .tenderNumber(e.getTenderNumber())
                .tenderName(e.getTenderName())
                .tenderMethod(e.getTenderMethod())
                .procurementType(e.getProcurementType())
                .awardAnnounceDate(e.getAwardAnnounceDate())
                .awardAmountRaw(e.getAwardAmountRaw())
                .awardAmount(e.getAwardAmount())
                .awardAnnounceSeq(e.getAwardAnnounceSeq())
                .detailUrl(e.getDetailUrl())
                .agencyCode(e.getAgencyCode())
                .unitName(e.getUnitName())
                .agencyAddress(e.getAgencyAddress())
                .contactPerson(e.getContactPerson())
                .contactPhone(e.getContactPhone())
                .contactEmail(e.getContactEmail())
                .tenderCategory(e.getTenderCategory())
                .procurementAmountRange(e.getProcurementAmountRange())
                .awardMethod(e.getAwardMethod())
                .hasBasePrice(e.getHasBasePrice())
                .awardDate(e.getAwardDate())
                .performancePeriod(e.getPerformancePeriod())
                .performanceLocation(e.getPerformanceLocation())
                .vendorOrderSeq(e.getVendorOrderSeq())
                .vendorName(e.getVendorName())
                .vendorTaxId(e.getVendorTaxId())
                .vendorAddress(e.getVendorAddress())
                .vendorPhone(e.getVendorPhone())
                .vendorAwardAmountRaw(e.getVendorAwardAmountRaw())
                .vendorAwardAmount(e.getVendorAwardAmount())
                .scrapedAt(e.getScrapedAt())
                .build();
    }
}
