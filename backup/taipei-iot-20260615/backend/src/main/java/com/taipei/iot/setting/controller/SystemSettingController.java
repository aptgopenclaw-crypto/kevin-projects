package com.taipei.iot.setting.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.setting.dto.SystemSettingDto;
import com.taipei.iot.setting.service.SystemSettingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/system-settings")
@RequiredArgsConstructor
@Validated
public class SystemSettingController {

	private final SystemSettingService settingService;

	@GetMapping
	@PreAuthorize("hasAuthority('SYSTEM_SETTINGS_VIEW')")
	public BaseResponse<List<SystemSettingDto>> listSettings() {
		return BaseResponse.success(settingService.findAllSettings());
	}

	@PutMapping("/{key}")
	@PreAuthorize("hasAuthority('SYSTEM_SETTINGS_MANAGE')")
	@AuditEvent(AuditEventType.UPDATE_SETTING)
	public BaseResponse<SystemSettingDto> updateSetting(@PathVariable @NotBlank String key,
			@RequestParam @NotBlank @Size(max = 500) String value) {
		return BaseResponse.success(settingService.updateSetting(key, value));
	}

	@GetMapping("/idle-timeout")
	public BaseResponse<Integer> getIdleTimeout() {
		return BaseResponse.success(settingService.getIdleTimeoutMinutes());
	}

	@PutMapping("/idle-timeout")
	@PreAuthorize("hasAuthority('SYSTEM_SETTINGS_MANAGE')")
	@AuditEvent(AuditEventType.UPDATE_SETTING)
	public BaseResponse<Integer> updateIdleTimeout(@RequestParam @Min(1) @Max(480) int minutes) {
		return BaseResponse.success(settingService.updateIdleTimeoutMinutes(minutes));
	}

}
