package com.taipei.iot.device.controller;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.device.dto.ComponentReplaceRequest;
import com.taipei.iot.device.dto.DeviceRequest;
import com.taipei.iot.device.dto.DeviceResponse;
import com.taipei.iot.device.entity.DeviceEvent;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.device.service.DeviceExportService;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceEventService deviceEventService;
    private final DeviceExportService deviceExportService;

    @GetMapping
    @PreAuthorize("hasAuthority('DEVICE_VIEW')")
    public BaseResponse<PageResponse<DeviceResponse>> list(
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DeviceResponse> result = deviceService.listDevices(deviceType, status, keyword,
                PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEVICE_VIEW')")
    public BaseResponse<DeviceResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(deviceService.getByIdWithComponents(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DEVICE_MANAGE')")
    @AuditEvent(AuditEventType.CREATE_DEVICE)
    public BaseResponse<DeviceResponse> create(@Valid @RequestBody DeviceRequest request) {
        return BaseResponse.success(deviceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEVICE_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_DEVICE)
    public BaseResponse<DeviceResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody DeviceRequest request) {
        return BaseResponse.success(deviceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DEVICE_MANAGE')")
    @AuditEvent(AuditEventType.DELETE_DEVICE)
    public BaseResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return BaseResponse.success(null);
    }

    @PostMapping("/{id}/decommission")
    @PreAuthorize("hasAuthority('DEVICE_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_DEVICE)
    public BaseResponse<Void> decommission(@PathVariable Long id) {
        deviceService.decommission(id);
        return BaseResponse.success(null);
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAuthority('DEVICE_VIEW')")
    public BaseResponse<PageResponse<DeviceEvent>> getEvents(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DeviceEvent> result = deviceEventService.getByDeviceId(id, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    // ── 組合元件 API ────────────────────────────────────────

    /** 查詢燈桿目前掛載的存活元件 */
    @GetMapping("/{id}/components")
    @PreAuthorize("hasAuthority('DEVICE_VIEW')")
    public BaseResponse<List<DeviceResponse>> getComponents(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeDecommissioned) {
        List<DeviceResponse> components = includeDecommissioned
                ? deviceService.getAllComponents(id)
                : deviceService.getActiveComponents(id);
        return BaseResponse.success(components);
    }

    /** 置換元件（停用舊元件 + 建立新元件 + 記錄事件） */
    @PostMapping("/{id}/components/replace")
    @PreAuthorize("hasAuthority('DEVICE_MANAGE')")
    @AuditEvent(AuditEventType.UPDATE_DEVICE)
    public BaseResponse<DeviceResponse> replaceComponent(
            @PathVariable Long id,
            @Valid @RequestBody ComponentReplaceRequest request) {
        return BaseResponse.success(deviceService.replaceComponent(id, request, deviceEventService));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('DEVICE_EXPORT')")
    @AuditEvent(AuditEventType.EXPORT_DEVICE)
    public void export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws Exception {

        var devices = deviceExportService.queryForExport(deviceType, status, keyword);

        switch (format.toLowerCase()) {
            case "xlsx" -> {
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition", "attachment; filename=devices.xlsx");
                deviceExportService.exportXlsx(devices, response.getOutputStream());
            }
            case "ods" -> {
                response.setContentType("application/vnd.oasis.opendocument.spreadsheet");
                response.setHeader("Content-Disposition", "attachment; filename=devices.ods");
                deviceExportService.exportOds(devices, response.getOutputStream());
            }
            default -> {
                response.setContentType("text/csv; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=devices.csv");
                deviceExportService.exportCsv(devices, response.getOutputStream());
            }
        }
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
