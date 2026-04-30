package com.taipei.iot.gis.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.gis.dto.GeoJsonResponse;
import com.taipei.iot.gis.dto.GmlImportDiff;
import com.taipei.iot.gis.entity.ZoneType;
import com.taipei.iot.gis.service.GisService;
import com.taipei.iot.gis.service.GmlExportService;
import com.taipei.iot.gis.service.GmlImportService;
import com.taipei.iot.gis.service.MapZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/gis")
@RequiredArgsConstructor
public class GisController {

    private final GisService gisService;
    private final MapZoneService mapZoneService;
    private final GmlExportService gmlExportService;
    private final GmlImportService gmlImportService;

    /**
     * GET /v1/gis/devices — all device points as GeoJSON
     */
    @GetMapping("/devices")
    @PreAuthorize("hasAuthority('GIS_VIEW')")
    public BaseResponse<GeoJsonResponse> allDevices(
            @RequestParam(required = false) String deviceType) {
        return BaseResponse.success(gisService.findAllDevices(deviceType));
    }

    /**
     * GET /v1/gis/devices/bounds — device points within bounding box
     */
    @GetMapping("/devices/bounds")
    @PreAuthorize("hasAuthority('GIS_VIEW')")
    public BaseResponse<GeoJsonResponse> devicesInBounds(
            @RequestParam BigDecimal minLng,
            @RequestParam BigDecimal minLat,
            @RequestParam BigDecimal maxLng,
            @RequestParam BigDecimal maxLat,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) Integer zoom) {
        return BaseResponse.success(
                gisService.findDevicesInBounds(minLng, minLat, maxLng, maxLat, deviceType, zoom));
    }

    /**
     * GET /v1/gis/devices/nearby — device points within radius of a point
     */
    @GetMapping("/devices/nearby")
    @PreAuthorize("hasAuthority('GIS_VIEW')")
    public BaseResponse<GeoJsonResponse> devicesNearby(
            @RequestParam BigDecimal lng,
            @RequestParam BigDecimal lat,
            @RequestParam(defaultValue = "500") double radius) {
        return BaseResponse.success(gisService.findDevicesNearby(lng, lat, radius));
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAuthority('GIS_VIEW')")
    public BaseResponse<GeoJsonResponse> zones(
            @RequestParam ZoneType type) {
        return BaseResponse.success(mapZoneService.getZonesByType(type));
    }

    @GetMapping("/zones/{id}/devices")
    @PreAuthorize("hasAuthority('GIS_VIEW')")
    public BaseResponse<GeoJsonResponse> devicesInZone(
            @PathVariable Long id) {
        return BaseResponse.success(mapZoneService.findDevicesInZone(id));
    }

    // ── Export endpoints ──

    @GetMapping("/devices/export/gml")
    @PreAuthorize("hasAuthority('GIS_VIEW')")
    public ResponseEntity<byte[]> exportGml(
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String district) {
        String gml = gmlExportService.exportAsGml(deviceType, district);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=streetlight_export.gml")
                .contentType(MediaType.APPLICATION_XML)
                .body(gml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/devices/export/open-data")
    @PreAuthorize("hasAuthority('GIS_VIEW')")
    public ResponseEntity<byte[]> exportOpenData(
            @RequestParam(required = false) String deviceType) {
        String csv = gmlExportService.exportAsOpenDataCsv(deviceType);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=streetlight_opendata.csv")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(result);
    }

    // ── Import endpoints ──

    @PostMapping("/devices/import/gml")
    @PreAuthorize("hasAuthority('GIS_MANAGE')")
    public BaseResponse<GmlImportDiff> importGmlPreview(
            @RequestParam("file") MultipartFile file) throws IOException {
        return BaseResponse.success(gmlImportService.parseAndDiff(file.getInputStream()));
    }

    @PostMapping("/devices/import/gml/confirm")
    @PreAuthorize("hasAuthority('GIS_MANAGE')")
    public BaseResponse<Integer> importGmlConfirm(
            @RequestBody GmlImportDiff diff) {
        return BaseResponse.success(gmlImportService.applyImport(diff));
    }
}
