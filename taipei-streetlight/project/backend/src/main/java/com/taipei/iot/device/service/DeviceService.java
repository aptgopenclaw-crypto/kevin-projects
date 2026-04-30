package com.taipei.iot.device.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.annotation.DataPermission;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.dto.ComponentReplaceRequest;
import com.taipei.iot.device.dto.DeviceRequest;
import com.taipei.iot.device.dto.DeviceResponse;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.gis.service.CoordinateService;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceTemplateService deviceTemplateService;
    private final CoordinateService coordinateService;
    private final DataScopeHelper dataScopeHelper;

    private static final int MAX_JSONB_SIZE = 10_000;

    @DataPermission
    public Page<DeviceResponse> listDevices(DeviceType deviceType, DeviceStatus status,
                                            String keyword, Pageable pageable) {
        List<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();
        Page<Device> page = deviceRepository.findByFilters(
                deviceType, status, keyword,
                visibleDeptIds.isEmpty() ? null : visibleDeptIds,
                pageable);
        return page.map(this::toResponse);
    }

    public DeviceResponse getById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        return toResponse(device);
    }

    @Transactional
    public DeviceResponse create(DeviceRequest request) {
        String tenantId = TenantContext.getCurrentTenantId();
        deviceRepository.findByTenantIdAndDeviceCode(tenantId, request.getDeviceCode())
                .ifPresent(d -> { throw new BusinessException(ErrorCode.DEVICE_CODE_DUPLICATE); });

        validateJsonbSize(request.getAttributes());
        validateJsonbSize(request.getNetworkConfig());
        deviceTemplateService.validateAttributes(
                request.getDeviceType().name(), request.getAttributes());

        if (request.getParentDeviceId() != null) {
            validateNoCircularReference(null, request.getParentDeviceId());
        }

        var coords = coordinateService.autoFill(
                request.getLng(), request.getLat(),
                request.getTwd97X(), request.getTwd97Y(),
                request.getTwd67X(), request.getTwd67Y());

        Device device = Device.builder()
                .deviceType(request.getDeviceType())
                .deviceCode(request.getDeviceCode())
                .deviceName(request.getDeviceName())
                .twd97X(coords.twd97X())
                .twd97Y(coords.twd97Y())
                .lng(coords.lng())
                .lat(coords.lat())
                .elevation(request.getElevation())
                .twd67X(coords.twd67X())
                .twd67Y(coords.twd67Y())
                .taipowerCoord(request.getTaipowerCoord())
                .deptId(request.getDeptId())
                .contractId(request.getContractId())
                .propertyOwner(request.getPropertyOwner())
                .status(DeviceStatus.ACTIVE)
                .installedAt(request.getInstalledAt())
                .parentDeviceId(request.getParentDeviceId())
                .mountPosition(request.getMountPosition())
                .connectivityType(request.getConnectivityType())
                .networkConfig(request.getNetworkConfig())
                .circuitId(request.getCircuitId())
                .attributes(request.getAttributes())
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();

        return toResponse(deviceRepository.save(device));
    }

    @Transactional
    public DeviceResponse update(Long id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        validateJsonbSize(request.getAttributes());
        validateJsonbSize(request.getNetworkConfig());
        deviceTemplateService.validateAttributes(
                request.getDeviceType().name(), request.getAttributes());

        if (request.getParentDeviceId() != null) {
            validateNoCircularReference(id, request.getParentDeviceId());
        }

        var coords = coordinateService.autoFill(
                request.getLng(), request.getLat(),
                request.getTwd97X(), request.getTwd97Y(),
                request.getTwd67X(), request.getTwd67Y());

        device.setDeviceType(request.getDeviceType());
        device.setDeviceCode(request.getDeviceCode());
        device.setDeviceName(request.getDeviceName());
        device.setTwd97X(coords.twd97X());
        device.setTwd97Y(coords.twd97Y());
        device.setLng(coords.lng());
        device.setLat(coords.lat());
        device.setElevation(request.getElevation());
        device.setTwd67X(coords.twd67X());
        device.setTwd67Y(coords.twd67Y());
        device.setTaipowerCoord(request.getTaipowerCoord());
        device.setDeptId(request.getDeptId());
        device.setContractId(request.getContractId());
        device.setPropertyOwner(request.getPropertyOwner());
        device.setInstalledAt(request.getInstalledAt());
        device.setParentDeviceId(request.getParentDeviceId());
        device.setMountPosition(request.getMountPosition());
        device.setConnectivityType(request.getConnectivityType());
        device.setNetworkConfig(request.getNetworkConfig());
        device.setCircuitId(request.getCircuitId());
        device.setAttributes(request.getAttributes());

        return toResponse(deviceRepository.save(device));
    }

    @Transactional
    public void delete(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        if (deviceRepository.existsByParentDeviceId(id)) {
            throw new BusinessException(ErrorCode.DEVICE_HAS_CHILDREN);
        }

        deviceRepository.delete(device);
    }

    @Transactional
    public void decommission(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        device.setStatus(DeviceStatus.DECOMMISSIONED);
        device.setDecommissionedAt(java.time.LocalDate.now());
        deviceRepository.save(device);
    }

    @Transactional
    public void updateStatus(Long deviceId, DeviceStatus status) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        device.setStatus(status);
        deviceRepository.save(device);
    }

    // ── 組合元件操作 ──────────────────────────────────────────

    /**
     * 取得某設備目前掛載的存活元件（燈具、控制器等）
     */
    public List<DeviceResponse> getActiveComponents(Long parentId) {
        deviceRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        return deviceRepository.findByParentDeviceIdAndStatusNot(parentId, DeviceStatus.DECOMMISSIONED)
                .stream().map(this::toResponse).toList();
    }

    /**
     * 取得某設備的所有元件（含已除役，用於歷史查看）
     */
    public List<DeviceResponse> getAllComponents(Long parentId) {
        deviceRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        return deviceRepository.findByParentDeviceId(parentId)
                .stream().map(this::toResponse).toList();
    }

    /**
     * 置換元件：停用舊元件 → 建立新元件 → 記錄事件
     * 新元件自動繼承舊元件的 parentDeviceId 和 mountPosition
     */
    @Transactional
    public DeviceResponse replaceComponent(Long poleId, ComponentReplaceRequest request,
                                           DeviceEventService deviceEventService) {
        Device pole = deviceRepository.findById(poleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (pole.getDeviceType() != DeviceType.POLE && pole.getDeviceType() != DeviceType.PANEL_BOX) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "只有燈桿或分電箱可以置換子元件");
        }

        Device oldDevice = deviceRepository.findById(request.getOldDeviceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (!poleId.equals(oldDevice.getParentDeviceId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "舊元件不屬於此燈桿");
        }
        if (oldDevice.getStatus() == DeviceStatus.DECOMMISSIONED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "舊元件已除役");
        }

        // 1. 停用舊元件
        oldDevice.setStatus(DeviceStatus.DECOMMISSIONED);
        oldDevice.setDecommissionedAt(java.time.LocalDate.now());
        deviceRepository.save(oldDevice);

        // 2. 建立新元件（繼承 parentDeviceId + mountPosition）
        DeviceRequest newReq = request.getNewDevice();
        String tenantId = TenantContext.getCurrentTenantId();
        deviceRepository.findByTenantIdAndDeviceCode(tenantId, newReq.getDeviceCode())
                .ifPresent(d -> { throw new BusinessException(ErrorCode.DEVICE_CODE_DUPLICATE); });
        validateJsonbSize(newReq.getAttributes());

        Device newDevice = Device.builder()
                .deviceType(newReq.getDeviceType())
                .deviceCode(newReq.getDeviceCode())
                .deviceName(newReq.getDeviceName())
                .twd97X(newReq.getTwd97X() != null ? newReq.getTwd97X() : pole.getTwd97X())
                .twd97Y(newReq.getTwd97Y() != null ? newReq.getTwd97Y() : pole.getTwd97Y())
                .lng(newReq.getLng() != null ? newReq.getLng() : pole.getLng())
                .lat(newReq.getLat() != null ? newReq.getLat() : pole.getLat())
                .elevation(newReq.getElevation())
                .twd67X(newReq.getTwd67X() != null ? newReq.getTwd67X() : pole.getTwd67X())
                .twd67Y(newReq.getTwd67Y() != null ? newReq.getTwd67Y() : pole.getTwd67Y())
                .deptId(pole.getDeptId())
                .contractId(newReq.getContractId() != null ? newReq.getContractId() : pole.getContractId())
                .propertyOwner(pole.getPropertyOwner())
                .status(DeviceStatus.ACTIVE)
                .installedAt(java.time.LocalDate.now())
                .parentDeviceId(poleId)
                .mountPosition(oldDevice.getMountPosition())
                .connectivityType(newReq.getConnectivityType())
                .networkConfig(newReq.getNetworkConfig())
                .circuitId(pole.getCircuitId())
                .attributes(newReq.getAttributes())
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();
        newDevice = deviceRepository.save(newDevice);

        // 3. 記錄事件
        String desc = String.format("置換元件：%s(%s) → %s(%s)。%s",
                oldDevice.getDeviceCode(), oldDevice.getDeviceType(),
                newDevice.getDeviceCode(), newDevice.getDeviceType(),
                request.getReason() != null ? request.getReason() : "");
        deviceEventService.recordEvent(poleId, DeviceEventType.REPLACE, desc, null);
        deviceEventService.recordEvent(oldDevice.getId(), DeviceEventType.DECOMMISSION,
                "因置換而除役", null);
        deviceEventService.recordEvent(newDevice.getId(), DeviceEventType.INSTALL,
                "置換安裝", null);

        return toResponse(newDevice);
    }

    /**
     * 取得設備明細（含組合元件）
     */
    public DeviceResponse getByIdWithComponents(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        DeviceResponse response = toResponse(device);
        if (device.getDeviceType() == DeviceType.POLE || device.getDeviceType() == DeviceType.PANEL_BOX) {
            List<DeviceResponse> children = deviceRepository
                    .findByParentDeviceIdAndStatusNot(id, DeviceStatus.DECOMMISSIONED)
                    .stream().map(this::toResponse).toList();
            response.setChildren(children);
        }
        return response;
    }

    private void validateNoCircularReference(Long childId, Long parentId) {
        if (parentId == null) return;
        if (childId != null && deviceRepository.checkCircularReference(childId, parentId)) {
            throw new BusinessException(ErrorCode.DEVICE_CIRCULAR_REFERENCE);
        }
    }

    private void validateJsonbSize(Map<String, Object> json) {
        if (json != null && json.toString().length() > MAX_JSONB_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "JSONB 內容超過大小限制");
        }
    }

    private DeviceResponse toResponse(Device d) {
        long childCount = 0;
        if (d.getDeviceType() == DeviceType.POLE || d.getDeviceType() == DeviceType.PANEL_BOX) {
            childCount = deviceRepository.countByParentDeviceIdAndStatusNot(d.getId(), DeviceStatus.DECOMMISSIONED);
        }
        return DeviceResponse.builder()
                .id(d.getId())
                .deviceType(d.getDeviceType())
                .deviceCode(d.getDeviceCode())
                .deviceName(d.getDeviceName())
                .twd97X(d.getTwd97X())
                .twd97Y(d.getTwd97Y())
                .lng(d.getLng())
                .lat(d.getLat())
                .elevation(d.getElevation())
                .twd67X(d.getTwd67X())
                .twd67Y(d.getTwd67Y())
                .taipowerCoord(d.getTaipowerCoord())
                .deptId(d.getDeptId())
                .contractId(d.getContractId())
                .propertyOwner(d.getPropertyOwner())
                .status(d.getStatus())
                .installedAt(d.getInstalledAt())
                .decommissionedAt(d.getDecommissionedAt())
                .parentDeviceId(d.getParentDeviceId())
                .mountPosition(d.getMountPosition())
                .connectivityType(d.getConnectivityType())
                .networkConfig(d.getNetworkConfig())
                .lastHeartbeatAt(d.getLastHeartbeatAt())
                .circuitId(d.getCircuitId())
                .attributes(d.getAttributes())
                .childrenCount(childCount)
                .createdBy(d.getCreatedBy())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
