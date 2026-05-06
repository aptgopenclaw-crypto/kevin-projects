package com.taipei.iot.dept.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeptRequest {

    @NotNull
    private Long deptId;

    private String deptName;

    private Integer deptSort;

    private Short status;
}
