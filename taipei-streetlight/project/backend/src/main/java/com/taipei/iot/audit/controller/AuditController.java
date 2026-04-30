package com.taipei.iot.audit.controller;

import com.taipei.iot.audit.dto.AuditQueryRequest;
import com.taipei.iot.audit.dto.UserEventLogDto;
import com.taipei.iot.audit.enums.AuditCategory;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.audit.service.AuditService;
import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.common.response.BaseResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/auth/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/categories")
    public BaseResponse<List<String>> getCategories() {
        List<String> categories = Arrays.stream(AuditCategory.values())
                .map(AuditCategory::getValue)
                .toList();
        return BaseResponse.success(categories);
    }

    @GetMapping("/user/usage/history/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN') or hasAuthority('AUDIT_EXPORT')")
    @AuditEvent(AuditEventType.EXPORT_AUDIT)
    public void exportUsageHistory(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String eventDesc,
            @RequestParam(required = false) String startTimestamp,
            @RequestParam(required = false) String endTimestamp,
            @RequestParam(defaultValue = "csv") String format,
            HttpServletResponse response) throws Exception {

        AuditQueryRequest request = new AuditQueryRequest();
        request.setUserName(userName);
        request.setEventDesc(eventDesc);
        request.setStartTimestamp(startTimestamp);
        request.setEndTimestamp(endTimestamp);

        boolean isAdmin = isAdminRole();
        List<UserEventLogDto> logs = auditService.queryForExport(request, isAdmin);

        response.setCharacterEncoding("UTF-8");
        if ("xlsx".equalsIgnoreCase(format)) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=audit-logs.xlsx");
            auditService.exportXlsx(logs, response.getOutputStream());
        } else {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=audit-logs.csv");
            auditService.exportCsv(logs, response.getOutputStream());
        }
    }

    @GetMapping("/user/usage/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPT_ADMIN') or hasAuthority('AUDIT_LIST')")
    public BaseResponse<Page<UserEventLogDto>> getUserUsageHistory(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String eventDesc,
            @RequestParam(required = false) String startTimestamp,
            @RequestParam(required = false) String endTimestamp,
            @RequestParam(required = false, defaultValue = "createTime") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sort,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int page) {

        AuditQueryRequest request = new AuditQueryRequest();
        request.setUserName(userName);
        request.setEventDesc(eventDesc);
        request.setStartTimestamp(startTimestamp);
        request.setEndTimestamp(endTimestamp);
        request.setSortBy(sortBy);
        request.setSort(sort);

        boolean isAdmin = isAdminRole();

        Page<UserEventLogDto> result = auditService.getUserUsageHistory(
                request, isAdmin, PageRequest.of(page, pageSize));
        return BaseResponse.success(result);
    }

    @GetMapping("/user/login/my")
    public BaseResponse<Page<UserEventLogDto>> getMyLoginLog(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String startTimestamp,
            @RequestParam(required = false) String endTimestamp,
            @RequestParam(required = false, defaultValue = "DESC") String sort,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int page) {

        LocalDateTime start = startTimestamp != null && !startTimestamp.isBlank()
                ? OffsetDateTime.parse(startTimestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() : null;
        LocalDateTime end = endTimestamp != null && !endTimestamp.isBlank()
                ? OffsetDateTime.parse(endTimestamp).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() : null;

        Sort.Direction direction = "ASC".equalsIgnoreCase(sort)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<UserEventLogDto> result = auditService.getMyEventLogs(
                eventType, start, end,
                PageRequest.of(page, pageSize, Sort.by(direction, "createTime")));
        return BaseResponse.success(result);
    }

    private static final Set<String> ADMIN_ROLES = Set.of(
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_DEPT_ADMIN");

    private boolean isAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_ROLES::contains);
    }
}
