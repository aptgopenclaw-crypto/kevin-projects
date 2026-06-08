package com.taipei.iot.user.controller;

import com.taipei.iot.common.annotation.PaginationParams;
import com.taipei.iot.common.annotation.DeprecatedApi;
import com.taipei.iot.common.dto.PageQuery;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.user.dto.request.AddTenantRoleRequest;
import com.taipei.iot.user.dto.request.CreateUserRequest;
import com.taipei.iot.user.dto.request.UpdateUserRequest;
import com.taipei.iot.common.dto.PageResponse;
import com.taipei.iot.user.dto.response.UserListItemDto;
import com.taipei.iot.user.dto.response.UserTenantMappingDto;
import com.taipei.iot.user.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/users")
@RequiredArgsConstructor
public class UserAdminController {

	private final UserAdminService userAdminService;

	@GetMapping
	@PreAuthorize("hasAuthority('USER_LIST')")
	public BaseResponse<PageResponse<UserListItemDto>> listUsers(@PaginationParams PageQuery pageQuery,
			@RequestParam(required = false) String keyword) {
		return BaseResponse.success(userAdminService.listUsers(pageQuery.getPage(), pageQuery.getSize(), keyword));
	}

	@GetMapping("/{userId}")
	@PreAuthorize("hasAuthority('USER_LIST')")
	public BaseResponse<UserListItemDto> getUser(@PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId) {
		return BaseResponse.success(userAdminService.getUser(userId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('USER_CREATE')")
	public BaseResponse<UserListItemDto> createUser(Authentication authentication,
			@Valid @RequestBody CreateUserRequest req) {
		String adminUserId = (String) authentication.getPrincipal();
		return BaseResponse.success(userAdminService.createUser(adminUserId, req));
	}

	@PutMapping("/{userId}")
	@PreAuthorize("hasAuthority('USER_UPDATE')")
	public BaseResponse<UserListItemDto> updateUser(Authentication authentication,
			@PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId,
			@Valid @RequestBody UpdateUserRequest req) {
		String adminUserId = (String) authentication.getPrincipal();
		return BaseResponse.success(userAdminService.updateUser(adminUserId, userId, req));
	}

	@DeleteMapping("/{userId}")
	@PreAuthorize("hasAuthority('USER_DISABLE')")
	public BaseResponse<Void> disableUser(Authentication authentication,
			@PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId) {
		String adminUserId = (String) authentication.getPrincipal();
		userAdminService.disableUser(adminUserId, userId);
		return BaseResponse.success(null);
	}

	@PatchMapping("/{userId}/soft-delete")
	@PreAuthorize("hasAuthority('USER_DELETE')")
	public BaseResponse<Void> softDeleteUser(Authentication authentication,
			@PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId) {
		String adminUserId = (String) authentication.getPrincipal();
		userAdminService.softDeleteUser(adminUserId, userId);
		return BaseResponse.success(null);
	}

	@GetMapping("/{userId}/tenant-roles")
	@PreAuthorize("hasAuthority('PLATFORM_USER_TENANT_MAPPING')")
	@Deprecated
	@DeprecatedApi(successor = "/v1/platform/users/{userId}/tenant-roles")
	@Operation(summary = "[Deprecated] 列出使用者租戶角色映射", deprecated = true,
			description = "請改用 GET /v1/platform/users/{userId}/tenant-roles")
	public BaseResponse<List<UserTenantMappingDto>> getUserTenantMappings(
			@PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId) {
		return BaseResponse.success(userAdminService.getUserTenantMappings(userId));
	}

	@PostMapping("/{userId}/tenant-roles")
	@PreAuthorize("hasAuthority('PLATFORM_USER_TENANT_MAPPING')")
	@Deprecated
	@DeprecatedApi(successor = "/v1/platform/users/{userId}/tenant-roles")
	@Operation(summary = "[Deprecated] 為使用者新增租戶角色映射", deprecated = true,
			description = "請改用 POST /v1/platform/users/{userId}/tenant-roles")
	public BaseResponse<UserTenantMappingDto> addTenantRole(Authentication authentication,
			@PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId,
			@Valid @RequestBody AddTenantRoleRequest req) {
		String adminUserId = (String) authentication.getPrincipal();
		return BaseResponse.success(userAdminService.addTenantRole(adminUserId, userId, req));
	}

	@DeleteMapping("/{userId}/tenant-roles/{mappingId}")
	@PreAuthorize("hasAuthority('PLATFORM_USER_TENANT_MAPPING')")
	@Deprecated
	@DeprecatedApi(successor = "/v1/platform/users/{userId}/tenant-roles/{mappingId}")
	@Operation(summary = "[Deprecated] 移除使用者租戶角色映射", deprecated = true,
			description = "請改用 DELETE /v1/platform/users/{userId}/tenant-roles/{mappingId}")
	public BaseResponse<Void> removeTenantRole(Authentication authentication,
			@PathVariable @Pattern(regexp = "^[a-f0-9\\-]{36}$") String userId, @PathVariable Long mappingId) {
		String adminUserId = (String) authentication.getPrincipal();
		userAdminService.removeTenantRole(adminUserId, userId, mappingId);
		return BaseResponse.success(null);
	}

}
