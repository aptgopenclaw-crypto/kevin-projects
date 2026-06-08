package com.taipei.iot.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegateCandidateDto {

	private String userId;

	private String displayName;

	private String deptName;

}
