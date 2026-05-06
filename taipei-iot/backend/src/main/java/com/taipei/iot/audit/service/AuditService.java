package com.taipei.iot.audit.service;

import com.taipei.iot.audit.dto.AuditQueryRequest;
import com.taipei.iot.audit.dto.UserEventLogDto;
import com.taipei.iot.audit.entity.UserEventLogEntity;
import com.taipei.iot.audit.repository.UserEventLogRepository;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final UserEventLogRepository userEventLogRepository;
    private final DataScopeHelper dataScopeHelper;

    /**
     * 操作紀錄查詢（Audit History）。
     * <p>使用 SYSTEM context 繞過 Hibernate @Filter，由 Specification 手動過濾 tenantId + DataScope。</p>
     * <ul>
     *   <li>isAdmin = false（DEPT_USER）→ 只看自己的紀錄</li>
     *   <li>isAdmin = true（SUPER_ADMIN / ADMIN / DEPT_ADMIN）→ 依 DataScope 看部門範圍內的紀錄</li>
     * </ul>
     */
    public Page<UserEventLogDto> getUserUsageHistory(AuditQueryRequest request,
                                                      boolean isAdmin, Pageable pageable) {
        String currentUserId = SecurityContextUtils.getCurrentUserId();
        String currentTenantId = TenantContext.getCurrentTenantId();

        Specification<UserEventLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 手動租戶過濾：目前租戶 OR tenantId 為空（預選租戶階段的事件）
            if (currentTenantId != null && !"SYSTEM".equals(currentTenantId)) {
                predicates.add(cb.or(
                        cb.equal(root.get("tenantId"), currentTenantId),
                        cb.isNull(root.get("tenantId"))
                ));
            }

            if (!isAdmin) {
                // DEPT_USER：只能看自己的紀錄
                predicates.add(cb.equal(root.get("userId"), currentUserId));
            } else {
                // 管理角色：依 DataScope 過濾部門
                List<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();
                if (!visibleDeptIds.isEmpty()) {
                    predicates.add(cb.or(
                            root.get("deptId").in(visibleDeptIds),
                            cb.isNull(root.get("deptId"))
                    ));
                }
                // visibleDeptIds 為空 = ALL scope，不限制
            }

            // 事件類別過濾
            if (request.getEventDesc() != null && !request.getEventDesc().isBlank()) {
                predicates.add(cb.equal(root.get("eventDesc"), request.getEventDesc()));
            }

            // 使用者關鍵字搜尋
            if (request.getUserName() != null && !request.getUserName().isBlank()) {
                String pattern = "%" + request.getUserName().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("userLabel")), pattern)
                ));
            }

            // 時間範圍
            if (request.getStartTimestamp() != null && !request.getStartTimestamp().isBlank()) {
                LocalDateTime startTime = OffsetDateTime.parse(request.getStartTimestamp())
                        .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }
            if (request.getEndTimestamp() != null && !request.getEndTimestamp().isBlank()) {
                LocalDateTime endTime = OffsetDateTime.parse(request.getEndTimestamp())
                        .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), endTime));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createTime";
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSort())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(direction, sortBy)
        );

        // 使用 SYSTEM context 繞過 @Filter，由 Specification 自行處理 tenantId 過濾
        String previous = TenantContext.getCurrentTenantId();
        try {
            TenantContext.setSystemContext();
            return userEventLogRepository.findAll(spec, sorted).map(this::toDto);
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenantId(previous);
            } else {
                TenantContext.clear();
            }
        }
    }

    public Page<UserEventLogDto> getMyEventLogs(String eventType,
                                                  LocalDateTime start, LocalDateTime end,
                                                  Pageable pageable) {
        String currentUserId = SecurityContextUtils.getCurrentUserId();

        Specification<UserEventLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), currentUserId));

            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userEventLogRepository.findAll(spec, pageable).map(this::toDto);
    }

    // ─── Export ────────────────────────────────────────────────────────────

    private static final String[] EXPORT_HEADERS = {
            "帳號", "使用者", "事件類型", "事件描述", "API", "結果碼",
            "IP", "耗時(ms)", "時間"
    };

    private static final DateTimeFormatter EXPORT_DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 查詢匯出用的完整資料（無分頁限制，上限 10000 筆）。
     * 遵循與 getUserUsageHistory 相同的 tenant + DataScope 過濾邏輯。
     */
    public List<UserEventLogDto> queryForExport(AuditQueryRequest request, boolean isAdmin) {
        Pageable exportPage = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "createTime"));
        return getUserUsageHistory(request, isAdmin, exportPage).getContent();
    }

    public void exportCsv(List<UserEventLogDto> logs, OutputStream out) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            pw.print('\uFEFF'); // BOM
            pw.println(String.join(",", EXPORT_HEADERS));
            for (UserEventLogDto log : logs) {
                pw.println(String.join(",",
                        csvEscape(log.getUsername()),
                        csvEscape(log.getUserLabel()),
                        csvEscape(log.getEventType()),
                        csvEscape(log.getEventDesc()),
                        csvEscape(log.getApiEndpoint()),
                        csvEscape(log.getErrorCode()),
                        csvEscape(log.getIpAddress()),
                        log.getExecutionTime() != null ? String.valueOf(log.getExecutionTime()) : "",
                        log.getCreateTime() != null ? log.getCreateTime().format(EXPORT_DT_FMT) : ""
                ));
            }
        }
    }

    public void exportXlsx(List<UserEventLogDto> logs, OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("稽核日誌");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < logs.size(); i++) {
                UserEventLogDto log = logs.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(val(log.getUsername()));
                row.createCell(1).setCellValue(val(log.getUserLabel()));
                row.createCell(2).setCellValue(val(log.getEventType()));
                row.createCell(3).setCellValue(val(log.getEventDesc()));
                row.createCell(4).setCellValue(val(log.getApiEndpoint()));
                row.createCell(5).setCellValue(val(log.getErrorCode()));
                row.createCell(6).setCellValue(val(log.getIpAddress()));
                if (log.getExecutionTime() != null) {
                    row.createCell(7).setCellValue(log.getExecutionTime());
                }
                if (log.getCreateTime() != null) {
                    row.createCell(8).setCellValue(log.getCreateTime().format(EXPORT_DT_FMT));
                }
            }

            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
        }
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String val(String s) {
        return s != null ? s : "";
    }

    // ─── Mapping ─────────────────────────────────────────────────────────

    private UserEventLogDto toDto(UserEventLogEntity entity) {
        return UserEventLogDto.builder()
                .userEventLogPk(entity.getUserEventLogPk())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .userLabel(entity.getUserLabel())
                .email(entity.getEmail())
                .eventType(entity.getEventType())
                .eventDesc(entity.getEventDesc())
                .apiEndpoint(entity.getApiEndpoint())
                .payload(entity.getPayload())
                .errorCode(entity.getErrorCode())
                .message(entity.getMessage())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .executionTime(entity.getExecutionTime())
                .deptId(entity.getDeptId())
                .createTime(entity.getCreateTime())
                .build();
    }
}
