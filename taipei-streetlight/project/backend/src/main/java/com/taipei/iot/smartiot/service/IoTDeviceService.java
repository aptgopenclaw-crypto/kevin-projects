package com.taipei.iot.smartiot.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.dept.annotation.DataPermission;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.smartiot.dto.IoTDeviceRegisterRequest;
import com.taipei.iot.smartiot.dto.IoTDeviceResponse;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IoTDeviceService {

    private final DeviceRepository deviceRepository;
    private final DataScopeHelper dataScopeHelper;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 將既有設備啟用 IoT 功能，產生 device_token。
     */
    @Transactional
    public IoTDeviceResponse registerIoT(IoTDeviceRegisterRequest request) {
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        // 確認同 tenant
        String currentTenant = TenantContext.getCurrentTenantId();
        if (!device.getTenantId().equals(currentTenant)) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        }

        // 若已有 token，拒絕重複註冊
        if (device.getDeviceToken() != null) {
            throw new BusinessException(ErrorCode.IOT_DEVICE_ALREADY_REGISTERED);
        }

        // 產生安全 token
        device.setDeviceToken(generateDeviceToken());
        device.setAuthType(request.getAuthType() != null ? request.getAuthType() : "TOKEN");
        device.setFirmwareVersion(request.getFirmwareVersion());
        device.setFormatId(request.getFormatId());

        deviceRepository.save(device);
        log.info("[IoT] Device registered — id={}, token={}***", device.getId(),
                device.getDeviceToken().substring(0, 8));

        return toResponse(device);
    }

    /**
     * 查詢所有 IoT 設備（device_token IS NOT NULL）。
     */
    @DataPermission
    public Page<IoTDeviceResponse> listIoTDevices(String keyword, Pageable pageable) {
        Collection<Long> deptIds = dataScopeHelper.getVisibleDeptIds();
        return deviceRepository.findIoTDevices(keyword, deptIds, pageable)
                .map(this::toResponse);
    }

    /**
     * 產生 32 bytes Base64url 編碼的安全 token。
     */
    private String generateDeviceToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private IoTDeviceResponse toResponse(Device d) {
        return IoTDeviceResponse.builder()
                .id(d.getId())
                .tenantId(d.getTenantId())
                .deviceCode(d.getDeviceCode())
                .deviceName(d.getDeviceName())
                .deviceType(d.getDeviceType())
                .status(d.getStatus())
                .connectivityType(d.getConnectivityType())
                .deviceToken(d.getDeviceToken())
                .authType(d.getAuthType())
                .firmwareVersion(d.getFirmwareVersion())
                .lastTelemetryAt(d.getLastTelemetryAt())
                .lastHeartbeatAt(d.getLastHeartbeatAt())
                .formatId(d.getFormatId())
                .lng(d.getLng() != null ? d.getLng().doubleValue() : null)
                .lat(d.getLat() != null ? d.getLat().doubleValue() : null)
                .deptId(d.getDeptId())
                .build();
    }
}
