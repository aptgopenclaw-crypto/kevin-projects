package com.taipei.iot.smartiot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DimmingGroupResponse {
    private Long id;
    private String groupName;
    private Long[] deviceIds;
    private LocalDateTime createdAt;
}
