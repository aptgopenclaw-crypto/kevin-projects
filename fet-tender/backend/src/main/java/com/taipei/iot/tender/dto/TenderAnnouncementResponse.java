package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.TenderAnnouncement;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TenderAnnouncementResponse {

    private Long id;
    private String solution;
    private String matchedKeyword;
    private String agencyName;
    private String tenderNumber;
    private String tenderName;
    private Integer transmissionCount;
    private String tenderMethod;
    private String procurementType;
    private LocalDate announcementDate;
    private LocalDateTime deadline;
    private String budgetAmountRaw;
    private BigDecimal budgetAmount;
    private String detailUrl;
    private String agencyCode;
    private String unitName;
    private String agencyAddress;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String tenderCategory;
    private String procurementAmountRange;
    private String handlingMethod;
    private String awardMethod;
    private String tenderStatus;
    private LocalDateTime openingTime;
    private String openingLocation;
    private Boolean hasBasePrice;
    private String performanceLocation;
    private LocalDateTime scrapedAt;

    public static TenderAnnouncementResponse from(TenderAnnouncement e) {
        return TenderAnnouncementResponse.builder()
                .id(e.getId())
                .solution(e.getSolution())
                .matchedKeyword(e.getMatchedKeyword())
                .agencyName(e.getAgencyName())
                .tenderNumber(e.getTenderNumber())
                .tenderName(e.getTenderName())
                .transmissionCount(e.getTransmissionCount())
                .tenderMethod(e.getTenderMethod())
                .procurementType(e.getProcurementType())
                .announcementDate(e.getAnnouncementDate())
                .deadline(e.getDeadline())
                .budgetAmountRaw(e.getBudgetAmountRaw())
                .budgetAmount(e.getBudgetAmount())
                .detailUrl(e.getDetailUrl())
                .agencyCode(e.getAgencyCode())
                .unitName(e.getUnitName())
                .agencyAddress(e.getAgencyAddress())
                .contactPerson(e.getContactPerson())
                .contactPhone(e.getContactPhone())
                .contactEmail(e.getContactEmail())
                .tenderCategory(e.getTenderCategory())
                .procurementAmountRange(e.getProcurementAmountRange())
                .handlingMethod(e.getHandlingMethod())
                .awardMethod(e.getAwardMethod())
                .tenderStatus(e.getTenderStatus())
                .openingTime(e.getOpeningTime())
                .openingLocation(e.getOpeningLocation())
                .hasBasePrice(e.getHasBasePrice())
                .performanceLocation(e.getPerformanceLocation())
                .scrapedAt(e.getScrapedAt())
                .build();
    }
}
