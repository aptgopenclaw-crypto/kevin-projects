package com.taipei.iot.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DelegateSetRequest(@NotBlank String delegateFor, @NotBlank String delegateTo, String businessType,
		@NotNull LocalDate effectiveFrom, @NotNull LocalDate effectiveTo) {

}
