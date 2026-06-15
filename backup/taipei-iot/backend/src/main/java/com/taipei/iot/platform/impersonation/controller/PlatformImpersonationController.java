package com.taipei.iot.platform.impersonation.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.platform.impersonation.dto.CreateImpersonationRequest;
import com.taipei.iot.platform.impersonation.dto.ImpersonationSessionDto;
import com.taipei.iot.platform.impersonation.dto.ImpersonationTokenResponse;
import com.taipei.iot.platform.impersonation.service.ImpersonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Platform-level impersonation API (ADR-002 / Phase 1).
 *
 * <p>
 * SUPER_ADMIN only — gated by {@code PLATFORM_IMPERSONATE} permission (seeded in V58) and
 * {@code ScopeEnforcementFilter}'s {@code /v1/platform/} prefix rule.
 *
 * <ul>
 * <li>{@code POST} → create new session + issue IMPERSONATION-scope token</li>
 * <li>{@code DELETE /{id}} → revoke (idempotent)</li>
 * <li>{@code GET} → list operator's own sessions (optional status filter)</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/platform/impersonations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PLATFORM_IMPERSONATE')")
public class PlatformImpersonationController {

	private final ImpersonationService impersonationService;

	@PostMapping
	@AuditEvent(AuditEventType.IMPERSONATE_START)
	public BaseResponse<ImpersonationTokenResponse> create(@Valid @RequestBody CreateImpersonationRequest req) {
		String operatorId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(impersonationService.create(req, operatorId));
	}

	@DeleteMapping("/{sessionId}")
	@AuditEvent(AuditEventType.IMPERSONATE_END)
	public BaseResponse<Void> revoke(@PathVariable String sessionId) {
		String operatorId = SecurityContextUtils.requireCurrentUserIdStrict();
		impersonationService.revoke(sessionId, operatorId);
		return BaseResponse.success(null);
	}

	@GetMapping
	public BaseResponse<List<ImpersonationSessionDto>> list(@RequestParam(required = false) String status) {
		String operatorId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(impersonationService.listByOperator(operatorId, status));
	}

}
