package com.taipei.iot.tender.dto;

import com.taipei.iot.tender.entity.TenderMailRecipient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MailRecipientRequest {

    @NotBlank
    @Email
    private String email;

    private String name;

    private Boolean isActive = true;

    public TenderMailRecipient toEntity() {
        return TenderMailRecipient.builder()
                .email(email.trim().toLowerCase())
                .name(name != null ? name.trim() : null)
                .isActive(isActive != null ? isActive : true)
                .build();
    }
}
