package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.AnnouncementAgencyFilter;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgencyFilterResponse {

    private Long id;
    private String solution;
    private String agencyKeyword;
    private Boolean isOrgOnlySearch;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static AgencyFilterResponse from(AnnouncementAgencyFilter e) {
        return AgencyFilterResponse.builder()
                .id(e.getId())
                .solution(e.getSolution())
                .agencyKeyword(e.getAgencyKeyword())
                .isOrgOnlySearch(e.getIsOrgOnlySearch())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
