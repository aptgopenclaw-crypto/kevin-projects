package com.taipei.iot.smartiot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DimmingGroupRequest {

    @NotBlank(message = "群組名稱為必填")
    private String groupName;

    @NotEmpty(message = "設備清單不可為空")
    private Long[] deviceIds;
}
