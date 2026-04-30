package com.taipei.iot.smartiot.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.smartiot.dto.IoTDeviceRegisterRequest;
import com.taipei.iot.smartiot.dto.IoTDeviceResponse;
import com.taipei.iot.smartiot.service.IoTDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/iot/devices")
@RequiredArgsConstructor
public class IoTDeviceController {

    private final IoTDeviceService ioTDeviceService;

    /**
     * POST /v1/auth/iot/devices — 將既有設備啟用 IoT 功能 (FN-07-001)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('IOT_MANAGE')")
    @AuditEvent(AuditEventType.REGISTER_IOT_DEVICE)
    public BaseResponse<IoTDeviceResponse> registerIoT(@Valid @RequestBody IoTDeviceRegisterRequest request) {
        return BaseResponse.success(ioTDeviceService.registerIoT(request));
    }

    /**
     * GET /v1/auth/iot/devices — IoT 設備列表 (FN-07-002)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('IOT_VIEW')")
    public BaseResponse<Page<IoTDeviceResponse>> listIoTDevices(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return BaseResponse.success(ioTDeviceService.listIoTDevices(keyword, PageRequest.of(page, size)));
    }
}
