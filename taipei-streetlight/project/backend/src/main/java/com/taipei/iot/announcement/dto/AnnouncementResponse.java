package com.taipei.iot.announcement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementResponse {

    private Long id;
    private String title;
    private String content;
    private String status;
    private String scope;
    private List<Long> targetDeptIds;
    private List<String> targetDeptNames;
    private Boolean pinned;
    private LocalDateTime publishAt;
    private LocalDateTime expireAt;
    private String createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isRead;
    private Boolean editable;
}
