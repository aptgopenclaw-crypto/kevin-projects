package com.taipei.iot.replacement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SelfCheckRequest {

    @NotEmpty(message = "檢核項目不可為空")
    @Valid
    private List<SelfCheckItemRequest> items;
}
