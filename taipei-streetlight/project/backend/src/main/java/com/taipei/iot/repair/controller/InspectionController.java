package com.taipei.iot.repair.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.repair.dto.InspectionRecordRequest;
import com.taipei.iot.repair.dto.InspectionRecordResponse;
import com.taipei.iot.repair.dto.InspectionTaskRequest;
import com.taipei.iot.repair.dto.InspectionTaskResponse;
import com.taipei.iot.repair.service.InspectionService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/v1/auth/inspection")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    // ── 巡查任務 ──────────────────────────────────────

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('INSPECTION_VIEW')")
    public BaseResponse<PageResponse<InspectionTaskResponse>> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InspectionTaskResponse> result = inspectionService.listTasks(PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('INSPECTION_VIEW')")
    public BaseResponse<InspectionTaskResponse> getTask(@PathVariable Long id) {
        return BaseResponse.success(inspectionService.getTaskById(id));
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasAuthority('INSPECTION_MANAGE')")
    public BaseResponse<InspectionTaskResponse> createTask(
            @Valid @RequestBody InspectionTaskRequest request) {
        return BaseResponse.success(inspectionService.createTask(request));
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('INSPECTION_MANAGE')")
    public BaseResponse<InspectionTaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody InspectionTaskRequest request) {
        return BaseResponse.success(inspectionService.updateTask(id, request));
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('INSPECTION_MANAGE')")
    public BaseResponse<Void> deactivateTask(@PathVariable Long id) {
        inspectionService.deactivateTask(id);
        return BaseResponse.success(null);
    }

    // ── 巡查紀錄 ──────────────────────────────────────

    @GetMapping("/tasks/{taskId}/records")
    @PreAuthorize("hasAuthority('INSPECTION_VIEW')")
    public BaseResponse<PageResponse<InspectionRecordResponse>> listRecords(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InspectionRecordResponse> result = inspectionService.getRecordsByTask(
                taskId, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @PostMapping("/records")
    @PreAuthorize("hasAuthority('INSPECTION_MANAGE')")
    public BaseResponse<InspectionRecordResponse> createRecord(
            @Valid @RequestBody InspectionRecordRequest request) {
        return BaseResponse.success(inspectionService.createRecord(request));
    }

    @GetMapping("/records/{id}")
    @PreAuthorize("hasAuthority('INSPECTION_VIEW')")
    public BaseResponse<InspectionRecordResponse> getRecord(@PathVariable Long id) {
        return BaseResponse.success(inspectionService.getRecordById(id));
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
