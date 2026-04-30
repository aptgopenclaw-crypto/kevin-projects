package com.taipei.iot.workflow.service;

import com.taipei.iot.auth.entity.UserTenantMappingEntity;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.auth.repository.UserTenantMappingRepository;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.repository.DeptInfoRepository;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.workflow.dto.DelegateCandidateDto;
import com.taipei.iot.workflow.dto.DelegateSettingRequest;
import com.taipei.iot.workflow.dto.DelegateSettingResponse;
import com.taipei.iot.workflow.entity.DelegateSetting;
import com.taipei.iot.workflow.repository.DelegateSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DelegateService {

    private final DelegateSettingRepository delegateSettingRepository;
    private final UserRepository userRepository;
    private final UserTenantMappingRepository userTenantMappingRepository;
    private final DataScopeHelper dataScopeHelper;
    private final DeptInfoRepository deptInfoRepository;

    /**
     * 取得可選為代理人的使用者清單（依 DataScope 過濾，排除自己）。
     */
    public List<DelegateCandidateDto> getCandidates() {
        String currentUserId = SecurityContextUtils.getCurrentUserId();
        String tenantId = TenantContext.getCurrentTenantId();
        List<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();

        List<UserTenantMappingEntity> mappings;
        if (visibleDeptIds.isEmpty()) {
            // ALL scope
            mappings = userTenantMappingRepository.findByTenantIdAndEnabledTrue(tenantId);
        } else {
            mappings = userTenantMappingRepository.findByTenantIdAndEnabledTrue(tenantId)
                    .stream()
                    .filter(m -> m.getDeptId() != null && visibleDeptIds.contains(m.getDeptId()))
                    .toList();
        }

        return mappings.stream()
                .filter(m -> !m.getUserId().equals(currentUserId))
                .filter(m -> m.getUser() != null && !Boolean.TRUE.equals(m.getUser().getDeleted()))
                .map(m -> {
                    String deptName = null;
                    if (m.getDeptId() != null) {
                        deptName = deptInfoRepository.findById(m.getDeptId())
                                .map(d -> d.getDeptName()).orElse(null);
                    }
                    return DelegateCandidateDto.builder()
                            .userId(m.getUserId())
                            .displayName(m.getUser().getDisplayName())
                            .deptName(deptName)
                            .build();
                })
                .toList();
    }

    public List<DelegateSettingResponse> getMyDelegates() {
        String currentUserId = SecurityContextUtils.getCurrentUserId();
        return delegateSettingRepository.findByDelegatorIdOrderByCreatedAtDesc(currentUserId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DelegateSettingResponse create(DelegateSettingRequest request) {
        String currentUserId = SecurityContextUtils.getCurrentUserId();

        // 不可自我代理
        if (currentUserId.equals(request.getDelegateId())) {
            throw new BusinessException(ErrorCode.DELEGATE_SELF_NOT_ALLOWED);
        }

        // end_date 必填
        if (request.getEndDate() == null) {
            throw new BusinessException(ErrorCode.DELEGATE_END_DATE_REQUIRED);
        }

        // 日期重疊檢查
        if (delegateSettingRepository.hasOverlappingDelegation(
                currentUserId, request.getStartDate(), request.getEndDate())) {
            throw new BusinessException(ErrorCode.DELEGATE_PERIOD_OVERLAP);
        }

        DelegateSetting setting = DelegateSetting.builder()
                .delegatorId(currentUserId)
                .delegateId(request.getDelegateId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(delegateSettingRepository.save(setting));
    }

    @Transactional
    public void deactivate(Long id) {
        DelegateSetting setting = delegateSettingRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "代理設定不存在"));
        setting.setIsActive(false);
        delegateSettingRepository.save(setting);
    }

    private DelegateSettingResponse toResponse(DelegateSetting d) {
        String delegatorName = userRepository.findById(d.getDelegatorId())
                .map(u -> u.getDisplayName()).orElse(d.getDelegatorId());
        String delegateName = userRepository.findById(d.getDelegateId())
                .map(u -> u.getDisplayName()).orElse(d.getDelegateId());

        return DelegateSettingResponse.builder()
                .id(d.getId())
                .delegatorId(d.getDelegatorId())
                .delegatorName(delegatorName)
                .delegateId(d.getDelegateId())
                .delegateName(delegateName)
                .startDate(d.getStartDate())
                .endDate(d.getEndDate())
                .reason(d.getReason())
                .isActive(d.getIsActive())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
