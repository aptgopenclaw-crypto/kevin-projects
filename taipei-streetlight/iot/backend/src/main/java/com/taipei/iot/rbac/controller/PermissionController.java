package com.taipei.iot.rbac.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.rbac.dto.response.PermissionDto;
import com.taipei.iot.rbac.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/auth/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public BaseResponse<List<PermissionDto>> listPermissions() {
        return BaseResponse.success(permissionService.listPermissions());
    }
}
