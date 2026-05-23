package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.TenderMailRecipient;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MailRecipientResponse {

    private Long id;
    private String email;
    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static MailRecipientResponse from(TenderMailRecipient e) {
        return MailRecipientResponse.builder()
                .id(e.getId())
                .email(e.getEmail())
                .name(e.getName())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
