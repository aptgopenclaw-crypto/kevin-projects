package com.taipei.iot.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 50000)
    private String content;

    @NotNull
    @Pattern(regexp = "^(DRAFT|PUBLISHED)$", message = "status must be DRAFT or PUBLISHED")
    private String status;

    @NotNull
    @Pattern(regexp = "^(ALL|DEPT)$", message = "scope must be ALL or DEPT")
    private String scope;

    private List<Long> targetDeptIds;

    private Boolean pinned;

    private LocalDateTime publishAt;

    private LocalDateTime expireAt;
}
