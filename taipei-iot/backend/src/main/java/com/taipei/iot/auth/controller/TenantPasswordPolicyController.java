package com.taipei.iot.auth.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.auth.policy.PasswordPolicyService;
import com.taipei.iot.auth.policy.dto.PasswordPolicyDto;
import com.taipei.iot.auth.policy.dto.UpdatePasswordPolicyRequest;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Tenant-scoped password-policy endpoints.
 *
 * <ul>
 * <li>{@code GET /} — effective merged policy (tenant override ∪ platform default)</li>
 * <li>{@code GET /tenant} — raw tenant overrides only (for "is this customised?"
 * badges)</li>
 * <li>{@code PUT /tenant} — upsert a single override (enforces platform lower bound)</li>
 * <li>{@code DELETE /tenant/{key}} — remove one override, falling back to platform
 * default</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/auth/password-policy")
@RequiredArgsConstructor
@Validated
public class TenantPasswordPolicyController {

	private final PasswordPolicyService policyService;

	@GetMapping
	public BaseResponse<PasswordPolicyDto> getEffective() {
		return BaseResponse.success(policyService.getEffective(TenantContext.getCurrentTenantId()));
	}

	@GetMapping("/tenant")
	@PreAuthorize("hasAuthority('PASSWORD_POLICY_MANAGE')")
	public BaseResponse<Map<String, String>> getTenantOverrides() {
		return BaseResponse.success(policyService.getTenantOverrides(TenantContext.getCurrentTenantId()));
	}

	@PutMapping("/tenant")
	@PreAuthorize("hasAuthority('PASSWORD_POLICY_MANAGE')")
	@AuditEvent(AuditEventType.UPDATE_PASSWORD_POLICY)
	public BaseResponse<Void> updateTenantOverride(@Valid @RequestBody UpdatePasswordPolicyRequest req) {
		policyService.updateTenantOverride(TenantContext.getCurrentTenantId(), req);
		return BaseResponse.success(null);
	}

	@DeleteMapping("/tenant/{key}")
	@PreAuthorize("hasAuthority('PASSWORD_POLICY_MANAGE')")
	@AuditEvent(AuditEventType.UPDATE_PASSWORD_POLICY)
	public BaseResponse<Void> deleteTenantOverride(@PathVariable @NotBlank String key) {
		policyService.deleteTenantOverride(TenantContext.getCurrentTenantId(), key);
		return BaseResponse.success(null);
	}

}
