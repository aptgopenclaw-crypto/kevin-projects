package com.taipei.iot.dept.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.dept.dto.CreateDeptRequest;
import com.taipei.iot.dept.dto.DeptDto;
import com.taipei.iot.dept.dto.DeptOptionVO;
import com.taipei.iot.dept.dto.UpdateDeptRequest;
import com.taipei.iot.dept.service.DeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/dept")
@RequiredArgsConstructor
public class DeptController {

	private final DeptService deptService;

	@GetMapping("/list")
	@PreAuthorize("hasAuthority('DEPT_LIST')")
	public BaseResponse<List<DeptDto>> getDeptTree() {
		return BaseResponse.success(deptService.getDeptTree());
	}

	@GetMapping("/options")
	@PreAuthorize("isAuthenticated()")
	public BaseResponse<List<DeptOptionVO>> getDeptOptions() {
		return BaseResponse.success(deptService.getDeptOptions());
	}

	@GetMapping("/scope-options")
	@PreAuthorize("isAuthenticated()")
	public BaseResponse<List<DeptOptionVO>> getScopedDeptOptions() {
		return BaseResponse.success(deptService.getScopedDeptOptions());
	}

	@GetMapping("/{deptId}")
	@PreAuthorize("isAuthenticated()")
	public BaseResponse<DeptDto> getDeptById(@PathVariable Long deptId) {
		return BaseResponse.success(deptService.getDeptById(deptId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('DEPT_CREATE')")
	@AuditEvent(AuditEventType.CREATE_DEPT)
	public BaseResponse<DeptDto> createDept(@Valid @RequestBody CreateDeptRequest request) {
		return BaseResponse.success(deptService.createDept(request));
	}

	@PutMapping
	@PreAuthorize("hasAuthority('DEPT_UPDATE')")
	@AuditEvent(AuditEventType.UPDATE_DEPT)
	public BaseResponse<DeptDto> updateDept(@Valid @RequestBody UpdateDeptRequest request) {
		return BaseResponse.success(deptService.updateDept(request));
	}

	@DeleteMapping("/{deptId}")
	@PreAuthorize("hasAuthority('DEPT_DELETE')")
	@AuditEvent(AuditEventType.DELETE_DEPT)
	public BaseResponse<Void> deleteDept(@PathVariable Long deptId) {
		deptService.deleteDept(deptId);
		return BaseResponse.success(null);
	}

}
