package com.taipei.iot.dept.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptDto {
    private Long id;
    private Long pid;
    private String deptName;
    private Integer deptSort;
    private Short status;
    private String hierarchyPath;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<DeptDto> children;
}
