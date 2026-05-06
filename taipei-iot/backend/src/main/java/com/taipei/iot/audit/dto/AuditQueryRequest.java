package com.taipei.iot.audit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditQueryRequest {

    private String userName;
    private String eventDesc;
    private String startTimestamp;
    private String endTimestamp;
    private String sortBy;
    private String sort;
}
