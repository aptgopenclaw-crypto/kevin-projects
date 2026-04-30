package com.taipei.iot.workflow.service;

import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.entity.UserTenantMappingEntity;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.auth.repository.UserTenantMappingRepository;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.dept.repository.DeptInfoRepository;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.workflow.dto.DelegateCandidateDto;
import com.taipei.iot.workflow.dto.DelegateSettingRequest;
import com.taipei.iot.workflow.dto.DelegateSettingResponse;
import com.taipei.iot.workflow.entity.DelegateSetting;
import com.taipei.iot.workflow.repository.DelegateSettingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelegateServiceTest {

    @InjectMocks private DelegateService delegateService;
    @Mock private DelegateSettingRepository delegateSettingRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserTenantMappingRepository userTenantMappingRepository;
    @Mock private DataScopeHelper dataScopeHelper;
    @Mock private DeptInfoRepository deptInfoRepository;

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken("user-001", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        TenantContext.setCurrentTenantId("TENANT_A");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void create_selfDelegation_throws() {
        DelegateSettingRequest req = new DelegateSettingRequest();
        req.setDelegateId("user-001");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(7));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> delegateService.create(req));
        assertEquals(ErrorCode.DELEGATE_SELF_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void create_endDateNull_throws() {
        DelegateSettingRequest req = new DelegateSettingRequest();
        req.setDelegateId("user-002");
        req.setStartDate(LocalDate.now());
        req.setEndDate(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> delegateService.create(req));
        assertEquals(ErrorCode.DELEGATE_END_DATE_REQUIRED, ex.getErrorCode());
    }

    @Test
    void create_overlappingPeriod_throws() {
        DelegateSettingRequest req = new DelegateSettingRequest();
        req.setDelegateId("user-002");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(7));

        when(delegateSettingRepository.hasOverlappingDelegation("user-001",
                LocalDate.now(), LocalDate.now().plusDays(7)))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> delegateService.create(req));
        assertEquals(ErrorCode.DELEGATE_PERIOD_OVERLAP, ex.getErrorCode());
    }

    @Test
    void create_success() {
        DelegateSettingRequest req = new DelegateSettingRequest();
        req.setDelegateId("user-002");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(7));

        when(delegateSettingRepository.hasOverlappingDelegation(any(), any(), any()))
                .thenReturn(false);
        when(delegateSettingRepository.save(any(DelegateSetting.class))).thenAnswer(inv -> {
            DelegateSetting d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        when(userRepository.findById("user-001")).thenReturn(
                Optional.of(UserEntity.builder().userId("user-001").displayName("測試管理員").build()));
        when(userRepository.findById("user-002")).thenReturn(
                Optional.of(UserEntity.builder().userId("user-002").displayName("王小明").build()));

        DelegateSettingResponse res = delegateService.create(req);
        assertNotNull(res);
        assertEquals("測試管理員", res.getDelegatorName());
        assertEquals("王小明", res.getDelegateName());
        verify(delegateSettingRepository).save(argThat(d -> d.getIsActive()));
    }

    @Test
    void deactivate_setsInactive() {
        DelegateSetting setting = DelegateSetting.builder()
                .id(1L).delegatorId("user-001").delegateId("user-002").isActive(true).build();
        when(delegateSettingRepository.findById(1L)).thenReturn(Optional.of(setting));
        when(delegateSettingRepository.save(any())).thenReturn(setting);

        delegateService.deactivate(1L);

        assertFalse(setting.getIsActive());
    }

    @Test
    void deactivate_notFound_throws() {
        when(delegateSettingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> delegateService.deactivate(99L));
    }

    @Test
    void getMyDelegates_returnsList() {
        when(delegateSettingRepository.findByDelegatorIdOrderByCreatedAtDesc("user-001"))
                .thenReturn(List.of());

        List<DelegateSettingResponse> result = delegateService.getMyDelegates();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCandidates_allScope_excludesSelf() {
        UserEntity user2 = UserEntity.builder().userId("user-002").displayName("王小明").deleted(false).build();
        UserEntity user3 = UserEntity.builder().userId("user-003").displayName("李大華").deleted(false).build();
        UserTenantMappingEntity m2 = UserTenantMappingEntity.builder()
                .userId("user-002").tenantId("TENANT_A").deptId(1L).enabled(true).build();
        m2.setUser(user2);
        UserTenantMappingEntity m3 = UserTenantMappingEntity.builder()
                .userId("user-003").tenantId("TENANT_A").deptId(2L).enabled(true).build();
        m3.setUser(user3);
        // current user mapping – should be excluded
        UserEntity selfUser = UserEntity.builder().userId("user-001").displayName("Self").deleted(false).build();
        UserTenantMappingEntity mSelf = UserTenantMappingEntity.builder()
                .userId("user-001").tenantId("TENANT_A").deptId(1L).enabled(true).build();
        mSelf.setUser(selfUser);

        when(dataScopeHelper.getVisibleDeptIds()).thenReturn(List.of());
        when(userTenantMappingRepository.findByTenantIdAndEnabledTrue("TENANT_A"))
                .thenReturn(List.of(mSelf, m2, m3));

        List<DelegateCandidateDto> result = delegateService.getCandidates();

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(c -> c.getUserId().equals("user-001")));
    }

    @Test
    void getCandidates_deptScope_filtersOtherDepts() {
        UserEntity user2 = UserEntity.builder().userId("user-002").displayName("王小明").deleted(false).build();
        UserEntity user3 = UserEntity.builder().userId("user-003").displayName("李大華").deleted(false).build();
        UserTenantMappingEntity m2 = UserTenantMappingEntity.builder()
                .userId("user-002").tenantId("TENANT_A").deptId(1L).enabled(true).build();
        m2.setUser(user2);
        UserTenantMappingEntity m3 = UserTenantMappingEntity.builder()
                .userId("user-003").tenantId("TENANT_A").deptId(2L).enabled(true).build();
        m3.setUser(user3);

        when(dataScopeHelper.getVisibleDeptIds()).thenReturn(List.of(1L));
        when(userTenantMappingRepository.findByTenantIdAndEnabledTrue("TENANT_A"))
                .thenReturn(List.of(m2, m3));

        List<DelegateCandidateDto> result = delegateService.getCandidates();

        assertEquals(1, result.size());
        assertEquals("user-002", result.get(0).getUserId());
    }
}
