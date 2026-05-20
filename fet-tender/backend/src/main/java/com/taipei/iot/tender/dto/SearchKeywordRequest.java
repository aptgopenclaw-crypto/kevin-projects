package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.AnnouncementSearchKeyword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchKeywordRequest {

    @NotBlank
    private String solution;

    @NotBlank
    private String keyword;

    private Boolean isActive = true;

    public AnnouncementSearchKeyword toEntity() {
        return AnnouncementSearchKeyword.builder()
                .solution(solution.trim())
                .keyword(keyword.trim())
                .isActive(isActive != null ? isActive : true)
                .build();
    }
}
