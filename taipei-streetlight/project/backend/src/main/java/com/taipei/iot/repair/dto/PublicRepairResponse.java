package com.taipei.iot.repair.dto;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PublicRepairResponse {

    private String ticketNumber;
    private String message;
}
