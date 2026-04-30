package com.taipei.iot.replacement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PoleNumberRequest {

    @NotBlank(message = "號碼牌編號為必填")
    private String poleNumber;

    private Long deviceId;
}
