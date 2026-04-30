package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.MaterialCategory;
import com.taipei.iot.material.enums.MaterialStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MaterialSpecRequest {

    @NotBlank(message = "規格代碼為必填")
    private String specCode;

    @NotBlank(message = "規格名稱為必填")
    private String specName;

    @NotNull(message = "類別為必填")
    private MaterialCategory category;

    private String unit;
    private Map<String, Object> attributes;
    private MaterialStatus status;
}
