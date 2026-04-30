package com.taipei.iot.device.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.device.service.DeviceTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth/device-templates")
@RequiredArgsConstructor
public class DeviceTemplateController {

    private final DeviceTemplateService templateService;

    @GetMapping("/{deviceType}/schema")
    @PreAuthorize("hasAuthority('DEVICE_VIEW')")
    public BaseResponse<Map<String, Object>> getSchema(@PathVariable String deviceType) {
        Map<String, Object> schema = templateService.getSchema(deviceType);
        return BaseResponse.success(schema);
    }

    @PutMapping("/{deviceType}/schema")
    @PreAuthorize("hasAuthority('DEVICE_TEMPLATE_MANAGE')")
    public BaseResponse<Map<String, Object>> updateSchema(
            @PathVariable String deviceType,
            @RequestBody Map<String, Object> schema) {
        return BaseResponse.success(templateService.updateSchema(deviceType, schema));
    }
}
