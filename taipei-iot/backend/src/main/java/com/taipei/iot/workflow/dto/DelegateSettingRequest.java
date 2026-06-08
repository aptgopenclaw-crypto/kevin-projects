package com.taipei.iot.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegateSettingRequest {

	@NotBlank(message = "代理人為必填")
	private String delegateId;

	@NotNull(message = "起始日期為必填")
	private LocalDate startDate;

	@NotNull(message = "結束日期為必填")
	private LocalDate endDate;

	private String reason;

}
