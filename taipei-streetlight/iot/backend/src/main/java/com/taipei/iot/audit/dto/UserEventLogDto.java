package com.taipei.iot.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventLogDto {

    private Long userEventLogPk;
    private String userId;
    private String username;
    private String userLabel;
    private String email;
    private String eventType;
    private String eventDesc;
    private String apiEndpoint;
    private String payload;
    private String errorCode;
    private String message;
    private String ipAddress;
    private String userAgent;
    private Long executionTime;
    private Long deptId;
    private LocalDateTime createTime;
}
