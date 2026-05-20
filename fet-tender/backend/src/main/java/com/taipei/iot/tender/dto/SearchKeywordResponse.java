package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.AnnouncementSearchKeyword;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SearchKeywordResponse {

    private Long id;
    private String solution;
    private String keyword;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static SearchKeywordResponse from(AnnouncementSearchKeyword e) {
        return SearchKeywordResponse.builder()
                .id(e.getId())
                .solution(e.getSolution())
                .keyword(e.getKeyword())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
