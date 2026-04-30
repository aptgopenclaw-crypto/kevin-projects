package com.taipei.iot.replacement.dto;

import com.taipei.iot.replacement.enums.PoleNumberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PoleNumberResponse {

    private Long id;
    private String poleNumber;
    private Long deviceId;
    private String qrCodeUrl;
    private LocalDate issuedAt;
    private PoleNumberStatus status;
    private LocalDateTime createdAt;
}
