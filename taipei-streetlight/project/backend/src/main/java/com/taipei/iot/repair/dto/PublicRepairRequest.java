package com.taipei.iot.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PublicRepairRequest {

    @NotBlank(message = "報修人姓名為必填")
    private String reporterName;

    @NotBlank(message = "報修人電話為必填")
    @Pattern(regexp = "^09\\d{8}$", message = "電話格式不正確（需為手機號碼）")
    private String reporterPhone;

    private String reporterEmail;

    @NotBlank(message = "故障描述為必填")
    private String reportDescription;

    @NotBlank(message = "地點說明為必填")
    private String reportAddress;

    /** 號碼牌編號（QR Code 掃描帶入） */
    private String poleNumber;

    @NotBlank(message = "驗證碼 Key 為必填")
    private String captchaKey;

    @NotBlank(message = "驗證碼為必填")
    private String captchaValue;

    /** 個資聲明同意（必須為 true） */
    private boolean privacyAgreed;
}
