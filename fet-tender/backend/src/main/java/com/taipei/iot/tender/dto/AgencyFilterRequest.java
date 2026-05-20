package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.AnnouncementAgencyFilter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgencyFilterRequest {

    @NotBlank
    private String solution;

    @NotBlank
    private String agencyKeyword;

    private Boolean isOrgOnlySearch = false;

    private Boolean isActive = true;

    public AnnouncementAgencyFilter toEntity() {
        return AnnouncementAgencyFilter.builder()
                .solution(solution.trim())
                .agencyKeyword(agencyKeyword.trim())
                .isOrgOnlySearch(isOrgOnlySearch != null ? isOrgOnlySearch : false)
                .isActive(isActive != null ? isActive : true)
                .build();
    }
}
