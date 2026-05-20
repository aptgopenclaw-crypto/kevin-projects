package com.taipei.iot.announcement.service;

import com.taipei.iot.announcement.dto.AnnouncementRequest;
import com.taipei.iot.announcement.dto.AnnouncementResponse;
import com.taipei.iot.announcement.entity.Announcement;
import com.taipei.iot.announcement.entity.AnnouncementDept;
import com.taipei.iot.announcement.entity.AnnouncementScope;
import com.taipei.iot.announcement.repository.AnnouncementDeptRepository;
import com.taipei.iot.announcement.repository.AnnouncementReadRepository;
import com.taipei.iot.announcement.repository.AnnouncementRepository;
import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.common.dto.UserInfo;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.entity.DeptInfoEntity;
import com.taipei.iot.dept.repository.DeptInfoRepository;
import com.taipei.iot.dept.enums.DataScopeEnum;
import com.taipei.iot.user.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementDeptRepository announcementDeptRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final DeptInfoRepository deptInfoRepository;
    private final UserRepository userRepository;

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("pinned"),
            Sort.Order.desc("publishAt")
    );

    // ── 前台查詢 ──

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> listVisible(int page, int size) {
        UserInfo user = SecurityContextUtils.getUserInfo();
        LocalDateTime now = LocalDateTime.now();
        Long deptId = user.getDeptId() != null ? user.getDeptId() : -1L;

        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<Announcement> pageResult = announcementRepository.findVisibleAnnouncements(deptId, now, pageable);

        List<AnnouncementResponse> content = toResponseList(pageResult.getContent(), user.getUserId(), false);
        return buildPageResponse(content, pageResult);
    }

    // ── 管理頁面查詢 ──

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> listAdmin(String statusFilter, String keyword, int page, int size) {
        UserInfo user = SecurityContextUtils.getUserInfo();
        LocalDateTime now = LocalDateTime.now();
        String safeFilter = statusFilter != null ? statusFilter : "ALL";
        String safeKeyword = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;

        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<Announcement> pageResult;

        DataScopeEnum scope = DataScopeEnum.fromString(user.getDataScope());
        if (scope == DataScopeEnum.ALL) {
            // ADMIN: 看全部
            pageResult = announcementRepository.findAdminAnnouncements(safeFilter, safeKeyword, now, pageable);
        } else {
            // DEPT_ADMIN: 自己建立的 + 受眾包含自己部門的
            Long deptId = user.getDeptId() != null ? user.getDeptId() : -1L;
            pageResult = announcementRepository.findDeptAdminAnnouncements(
                    user.getUserId(), deptId, safeFilter, safeKeyword, now, pageable);
        }

        List<AnnouncementResponse> content = toResponseList(pageResult.getContent(), user.getUserId(), true);
        return buildPageResponse(content, pageResult);
    }

    // ── 單筆查詢 ──

    @Transactional(readOnly = true)
    public AnnouncementResponse getById(Long id) {
        UserInfo user = SecurityContextUtils.getUserInfo();
        Announcement entity = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        return toResponse(entity, user.getUserId(), false);
    }

    // ── 新增 ──

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request) {
        UserInfo user = SecurityContextUtils.getUserInfo();
        String displayName = resolveDisplayName(user.getUserId());

        Announcement entity = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus())
                .scope(request.getScope())
                .pinned(request.getPinned() != null ? request.getPinned() : false)
                .publishAt(resolvePublishAt(request))
                .expireAt(request.getExpireAt())
                .createdBy(user.getUserId())
                .createdByName(displayName)
                .build();

        // DEPT_ADMIN 強制 scope=DEPT, targetDeptIds=[自己部門]
        DataScopeEnum dataScope = DataScopeEnum.fromString(user.getDataScope());
        List<Long> targetDeptIds;
        if (dataScope != DataScopeEnum.ALL) {
            entity.setScope(AnnouncementScope.DEPT.getValue());
            targetDeptIds = List.of(user.getDeptId());
        } else {
            targetDeptIds = request.getTargetDeptIds();
            if (AnnouncementScope.DEPT.getValue().equals(entity.getScope())) {
                if (targetDeptIds == null || targetDeptIds.isEmpty()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "指定部門公告必須選擇至少一個部門");
                }
            }
        }

        announcementRepository.save(entity);

        // 儲存 junction table
        if (AnnouncementScope.DEPT.getValue().equals(entity.getScope()) && targetDeptIds != null) {
            saveAnnouncementDepts(entity.getId(), targetDeptIds);
        }

        return toResponse(entity, user.getUserId(), false);
    }

    // ── 編輯 ──

    @Transactional
    public AnnouncementResponse update(Long id, AnnouncementRequest request) {
        UserInfo user = SecurityContextUtils.getUserInfo();

        Announcement entity = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        // DEPT_ADMIN 只能編輯自己建立的
        DataScopeEnum dataScope = DataScopeEnum.fromString(user.getDataScope());
        if (dataScope != DataScopeEnum.ALL && !entity.getCreatedBy().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        entity.setStatus(request.getStatus());
        entity.setPinned(request.getPinned() != null ? request.getPinned() : false);
        entity.setPublishAt(resolvePublishAt(request));
        entity.setExpireAt(request.getExpireAt());
        entity.setUpdatedAt(LocalDateTime.now());

        // DEPT_ADMIN 強制 scope=DEPT + 自己部門
        List<Long> targetDeptIds;
        if (dataScope != DataScopeEnum.ALL) {
            entity.setScope(AnnouncementScope.DEPT.getValue());
            targetDeptIds = List.of(user.getDeptId());
        } else {
            entity.setScope(request.getScope());
            targetDeptIds = request.getTargetDeptIds();
            if (AnnouncementScope.DEPT.getValue().equals(entity.getScope())) {
                if (targetDeptIds == null || targetDeptIds.isEmpty()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "指定部門公告必須選擇至少一個部門");
                }
            }
        }

        announcementRepository.save(entity);

        // 重建 junction table
        announcementDeptRepository.deleteByAnnouncementId(entity.getId());
        if (AnnouncementScope.DEPT.getValue().equals(entity.getScope()) && targetDeptIds != null) {
            saveAnnouncementDepts(entity.getId(), targetDeptIds);
        }

        return toResponse(entity, user.getUserId(), false);
    }

    // ── 刪除 ──

    @Transactional
    public void delete(Long id) {
        UserInfo user = SecurityContextUtils.getUserInfo();

        Announcement entity = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        // DEPT_ADMIN 只能刪除自己建立的
        DataScopeEnum dataScope = DataScopeEnum.fromString(user.getDataScope());
        if (dataScope != DataScopeEnum.ALL && !entity.getCreatedBy().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }

        // CASCADE 會自動刪除 announcement_depts + announcement_reads
        announcementRepository.delete(entity);
    }

    // ── private helpers ──

    private void saveAnnouncementDepts(Long announcementId, List<Long> deptIds) {
        List<AnnouncementDept> depts = deptIds.stream()
                .map(deptId -> AnnouncementDept.builder()
                        .announcementId(announcementId)
                        .deptId(deptId)
                        .build())
                .toList();
        announcementDeptRepository.saveAll(depts);
    }

    private LocalDateTime resolvePublishAt(AnnouncementRequest request) {
        return request.getPublishAt() != null ? request.getPublishAt() : LocalDateTime.now();
    }

    private String resolveDisplayName(String userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getDisplayName)
                .orElse(userId);
    }

    private AnnouncementResponse toResponse(Announcement entity, String currentUserId, boolean includeEditable) {
        List<AnnouncementDept> depts = announcementDeptRepository.findByAnnouncementId(entity.getId());
        List<Long> deptIds = depts.stream().map(AnnouncementDept::getDeptId).toList();
        List<String> deptNames = resolveDeptNames(deptIds);

        AnnouncementResponse.AnnouncementResponseBuilder builder = AnnouncementResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .scope(entity.getScope())
                .targetDeptIds(deptIds)
                .targetDeptNames(deptNames)
                .pinned(entity.getPinned())
                .publishAt(entity.getPublishAt())
                .expireAt(entity.getExpireAt())
                .createdBy(entity.getCreatedBy())
                .createdByName(entity.getCreatedByName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isRead(false); // default, overridden in list

        if (includeEditable) {
            UserInfo user = SecurityContextUtils.getUserInfo();
            DataScopeEnum dataScope = DataScopeEnum.fromString(user.getDataScope());
            boolean editable = dataScope == DataScopeEnum.ALL
                    || entity.getCreatedBy().equals(currentUserId);
            builder.editable(editable);
        }

        return builder.build();
    }

    private List<AnnouncementResponse> toResponseList(List<Announcement> entities, String currentUserId, boolean includeEditable) {
        if (entities.isEmpty()) return Collections.emptyList();

        // 批次載入 junction table
        List<Long> ids = entities.stream().map(Announcement::getId).toList();
        Map<Long, List<AnnouncementDept>> deptMap = announcementDeptRepository.findByAnnouncementIdIn(ids)
                .stream()
                .collect(Collectors.groupingBy(AnnouncementDept::getAnnouncementId));

        // 批次載入已讀狀態
        Set<Long> readIds = getReadAnnouncementIds(ids, currentUserId);

        // 收集所有部門 ID 一次查詢名稱
        Set<Long> allDeptIds = deptMap.values().stream()
                .flatMap(List::stream)
                .map(AnnouncementDept::getDeptId)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = resolveDeptNameMap(allDeptIds);

        UserInfo user = SecurityContextUtils.getUserInfo();
        DataScopeEnum dataScope = DataScopeEnum.fromString(user.getDataScope());

        return entities.stream().map(entity -> {
            List<AnnouncementDept> entityDepts = deptMap.getOrDefault(entity.getId(), Collections.emptyList());
            List<Long> deptIds = entityDepts.stream().map(AnnouncementDept::getDeptId).toList();
            List<String> deptNames = deptIds.stream()
                    .map(id -> deptNameMap.getOrDefault(id, String.valueOf(id)))
                    .toList();

            AnnouncementResponse.AnnouncementResponseBuilder builder = AnnouncementResponse.builder()
                    .id(entity.getId())
                    .title(entity.getTitle())
                    .content(entity.getContent())
                    .status(entity.getStatus())
                    .scope(entity.getScope())
                    .targetDeptIds(deptIds)
                    .targetDeptNames(deptNames)
                    .pinned(entity.getPinned())
                    .publishAt(entity.getPublishAt())
                    .expireAt(entity.getExpireAt())
                    .createdBy(entity.getCreatedBy())
                    .createdByName(entity.getCreatedByName())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .isRead(readIds.contains(entity.getId()));

            if (includeEditable) {
                boolean editable = dataScope == DataScopeEnum.ALL
                        || entity.getCreatedBy().equals(currentUserId);
                builder.editable(editable);
            }

            return builder.build();
        }).toList();
    }

    private Set<Long> getReadAnnouncementIds(List<Long> announcementIds, String userId) {
        return announcementReadRepository.findByAnnouncementIdInAndUserId(announcementIds, userId)
                .stream()
                .map(com.taipei.iot.announcement.entity.AnnouncementRead::getAnnouncementId)
                .collect(Collectors.toSet());
    }

    private List<String> resolveDeptNames(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return Collections.emptyList();
        return deptIds.stream()
                .map(id -> deptInfoRepository.findByDeptId(id)
                        .map(DeptInfoEntity::getDeptName)
                        .orElse(String.valueOf(id)))
                .toList();
    }

    private Map<Long, String> resolveDeptNameMap(Set<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return Collections.emptyMap();
        return deptIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> deptInfoRepository.findByDeptId(id)
                                .map(DeptInfoEntity::getDeptName)
                                .orElse(String.valueOf(id))
                ));
    }

    private <T> PageResponse<T> buildPageResponse(List<T> content, Page<?> page) {
        return PageResponse.<T>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
