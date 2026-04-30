package com.taipei.iot.auth.service;

import com.taipei.iot.auth.dto.request.ChangePasswordRequest;
import com.taipei.iot.auth.dto.request.ForgotPasswordRequest;
import com.taipei.iot.auth.dto.request.LoginRequest;
import com.taipei.iot.auth.dto.request.ResetPasswordRequest;
import com.taipei.iot.auth.dto.request.SelectTenantRequest;
import com.taipei.iot.auth.dto.request.SwitchTenantRequest;
import com.taipei.iot.auth.dto.response.LoginResult;
import com.taipei.iot.auth.dto.response.TokenResult;
import com.taipei.iot.auth.dto.response.UserInfoDto;
import com.taipei.iot.audit.repository.UserEventLogRepository;
import com.taipei.iot.auth.entity.RoleEntity;
import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.entity.UserResetPasswordTokenEntity;
import com.taipei.iot.auth.entity.UserTenantMappingEntity;
import com.taipei.iot.auth.repository.ChangePasswordLogRepository;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.auth.repository.UserResetPasswordTokenRepository;
import com.taipei.iot.auth.repository.UserTenantMappingRepository;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.auth.service.impl.AuthServiceImpl;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.user.repository.PasswordHistoryRepository;
import com.taipei.iot.user.service.PasswordValidator;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.tenant.TenantEntity;
import com.taipei.iot.tenant.TenantRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private UserRepository userRepository;
    @Mock private UserTenantMappingRepository userTenantMappingRepository;
    @Mock private UserEventLogRepository userEventLogRepository;
    @Mock private ChangePasswordLogRepository changePasswordLogRepository;
    @Mock private UserResetPasswordTokenRepository userResetPasswordTokenRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private CaptchaService captchaService;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EntityManager entityManager;
    @Mock private HttpServletRequest httpRequest;
    @Mock private PasswordResetMailService passwordResetMailService;
    @Mock private PasswordValidator passwordValidator;
    @Mock private PasswordHistoryRepository passwordHistoryRepository;

    private UserEntity singleTenantUser;
    private UserEntity multiTenantUser;
    private UserEntity superAdmin;
    private UserEntity disabledUser;
    private UserEntity lockedUser;
    private RoleEntity adminRole;
    private RoleEntity operatorRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "entityManager", entityManager);
        ReflectionTestUtils.setField(authService, "maxFailCount", 5);
        ReflectionTestUtils.setField(authService, "lockDurationMinutes", 10);

        adminRole = RoleEntity.builder()
                .roleId("ROLE_ADMIN").code("ADMIN").name("場域管理者").build();
        operatorRole = RoleEntity.builder()
                .roleId("ROLE_OPERATOR").code("OPERATOR").name("維運人員").build();

        singleTenantUser = UserEntity.builder()
                .userId("user-admin-001").email("admin@test.com")
                .passwordHash("$2a$10$hash").displayName("Tenant A Admin")
                .enabled(true).locked(false).loginFailCount(0).isSuperAdmin(false)
                .build();

        multiTenantUser = UserEntity.builder()
                .userId("user-op-001").email("operator@test.com")
                .passwordHash("$2a$10$hash").displayName("Multi-Tenant Operator")
                .enabled(true).locked(false).loginFailCount(0).isSuperAdmin(false)
                .build();

        superAdmin = UserEntity.builder()
                .userId("user-super-001").email("super@test.com")
                .passwordHash("$2a$10$hash").displayName("Super Admin")
                .enabled(true).locked(false).loginFailCount(0).isSuperAdmin(true)
                .build();

        disabledUser = UserEntity.builder()
                .userId("user-disabled-001").email("disabled@test.com")
                .passwordHash("$2a$10$hash").displayName("Disabled User")
                .enabled(false).locked(false).loginFailCount(0).isSuperAdmin(false)
                .build();

        lockedUser = UserEntity.builder()
                .userId("user-locked-001").email("locked@test.com")
                .passwordHash("$2a$10$hash").displayName("Locked User")
                .enabled(true).locked(true).loginFailCount(5).isSuperAdmin(false)
                .lockedAt(LocalDateTime.now())
                .build();
    }

    private LoginRequest buildLoginRequest(String email) {
        return LoginRequest.builder()
                .email(email).password("Test1234!")
                .captcha("1234").captchaKey("key-123")
                .build();
    }

    @Test
    void login_singleTenant_success() {
        LoginRequest request = buildLoginRequest("admin@test.com");
        when(captchaService.verify("key-123", "1234")).thenReturn(true);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(singleTenantUser));
        when(passwordEncoder.matches("Test1234!", "$2a$10$hash")).thenReturn(true);

        UserTenantMappingEntity mapping = UserTenantMappingEntity.builder()
                .userId("user-admin-001").tenantId("TENANT_A")
                .roleId("ROLE_ADMIN").deptId(1L).enabled(true)
                .build();
        mapping.setRole(adminRole);

        when(userTenantMappingRepository.findByUserIdAndEnabledTrue("user-admin-001"))
                .thenReturn(List.of(mapping));

        Query permQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("role_permissions"))).thenReturn(permQuery);
        when(permQuery.setParameter(eq("roleId"), any())).thenReturn(permQuery);
        when(permQuery.setParameter(eq("tenantId"), any())).thenReturn(permQuery);
        when(permQuery.getResultList()).thenReturn(List.of("DEVICE_VIEW", "DEVICE_CREATE"));

        when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        LoginResult result = authService.login(request, httpRequest);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertFalse(result.isNeedsSelection());
    }

    @Test
    void login_multiTenant_success() {
        LoginRequest request = buildLoginRequest("operator@test.com");
        when(captchaService.verify("key-123", "1234")).thenReturn(true);
        when(userRepository.findByEmail("operator@test.com")).thenReturn(Optional.of(multiTenantUser));
        when(passwordEncoder.matches("Test1234!", "$2a$10$hash")).thenReturn(true);

        UserTenantMappingEntity m1 = UserTenantMappingEntity.builder()
                .userId("user-op-001").tenantId("TENANT_A").roleId("ROLE_OPERATOR").enabled(true).build();
        m1.setRole(operatorRole);
        UserTenantMappingEntity m2 = UserTenantMappingEntity.builder()
                .userId("user-op-001").tenantId("TENANT_B").roleId("ROLE_OPERATOR").enabled(true).build();
        m2.setRole(operatorRole);

        when(userTenantMappingRepository.findByUserIdAndEnabledTrue("user-op-001"))
                .thenReturn(List.of(m1, m2));

        TenantEntity tenantA = new TenantEntity();
        tenantA.setTenantId("TENANT_A"); tenantA.setTenantCode("KHH_WATER"); tenantA.setTenantName("高雄市水情");
        TenantEntity tenantB = new TenantEntity();
        tenantB.setTenantId("TENANT_B"); tenantB.setTenantCode("NTPC_WATER"); tenantB.setTenantName("新北市水情");

        when(tenantRepository.findById("TENANT_A")).thenReturn(Optional.of(tenantA));
        when(tenantRepository.findById("TENANT_B")).thenReturn(Optional.of(tenantB));

        Query deptQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("dept_info"))).thenReturn(deptQuery);
        when(deptQuery.setParameter(eq("deptId"), any())).thenReturn(deptQuery);
        when(deptQuery.getResultList()).thenReturn(Collections.emptyList());

        when(jwtUtil.generateTemporaryToken(any(), any(), eq(false))).thenReturn("temp-token");

        LoginResult result = authService.login(request, httpRequest);

        assertNotNull(result);
        assertTrue(result.isNeedsSelection());
        assertEquals("temp-token", result.getAccessToken());
        assertNull(result.getRefreshToken());
        assertNotNull(result.getTenants());
        assertEquals(2, result.getTenants().size());
    }

    @Test
    void login_superAdmin_success() {
        LoginRequest request = buildLoginRequest("super@test.com");
        when(captchaService.verify("key-123", "1234")).thenReturn(true);
        when(userRepository.findByEmail("super@test.com")).thenReturn(Optional.of(superAdmin));
        when(passwordEncoder.matches("Test1234!", "$2a$10$hash")).thenReturn(true);

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantId("TENANT_A"); tenant.setTenantCode("KHH_WATER"); tenant.setTenantName("高雄市水情");
        when(tenantRepository.findByEnabledTrue()).thenReturn(List.of(tenant));

        when(jwtUtil.generateTemporaryToken(any(), any(), eq(true))).thenReturn("super-temp-token");

        LoginResult result = authService.login(request, httpRequest);

        assertNotNull(result);
        assertTrue(result.isNeedsSelection());
        assertEquals("super-temp-token", result.getAccessToken());
    }

    @Test
    void login_wrongPassword_throwsLoginFail() {
        LoginRequest request = buildLoginRequest("admin@test.com");
        when(captchaService.verify("key-123", "1234")).thenReturn(true);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(singleTenantUser));
        when(passwordEncoder.matches("Test1234!", "$2a$10$hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, httpRequest));
        assertEquals(ErrorCode.LOGIN_FAIL, ex.getErrorCode());
    }

    @Test
    void login_userNotFound_throwsUserNotFound() {
        LoginRequest request = buildLoginRequest("nonexistent@test.com");
        when(captchaService.verify("key-123", "1234")).thenReturn(true);
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, httpRequest));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void login_accountDisabled_throwsAccountDisabled() {
        LoginRequest request = buildLoginRequest("disabled@test.com");
        when(captchaService.verify("key-123", "1234")).thenReturn(true);
        when(userRepository.findByEmail("disabled@test.com")).thenReturn(Optional.of(disabledUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, httpRequest));
        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
    }

    @Test
    void login_accountLocked_throwsAccountLocked() {
        LoginRequest request = buildLoginRequest("locked@test.com");
        when(captchaService.verify("key-123", "1234")).thenReturn(true);
        when(userRepository.findByEmail("locked@test.com")).thenReturn(Optional.of(lockedUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, httpRequest));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
    }

    @Test
    void login_noTenantMapping_throwsTenantAccessDenied() {
        LoginRequest request = buildLoginRequest("admin@test.com");
        when(captchaService.verify("key-123", "1234")).thenReturn(true);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(singleTenantUser));
        when(passwordEncoder.matches("Test1234!", "$2a$10$hash")).thenReturn(true);
        when(userTenantMappingRepository.findByUserIdAndEnabledTrue("user-admin-001"))
                .thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, httpRequest));
        assertEquals(ErrorCode.TENANT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void selectTenant_success() {
        when(userRepository.findById("user-op-001")).thenReturn(Optional.of(multiTenantUser));

        UserTenantMappingEntity mapping = UserTenantMappingEntity.builder()
                .userId("user-op-001").tenantId("TENANT_A")
                .roleId("ROLE_OPERATOR").enabled(true).build();
        mapping.setRole(operatorRole);
        when(userTenantMappingRepository.findByUserIdAndTenantId("user-op-001", "TENANT_A"))
                .thenReturn(Optional.of(mapping));

        Query permQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("role_permissions"))).thenReturn(permQuery);
        when(permQuery.setParameter(eq("roleId"), any())).thenReturn(permQuery);
        when(permQuery.setParameter(eq("tenantId"), any())).thenReturn(permQuery);
        when(permQuery.getResultList()).thenReturn(List.of("DEVICE_VIEW"));

        when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("full-access-token");
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        SelectTenantRequest request = SelectTenantRequest.builder().tenantId("TENANT_A").build();
        TokenResult result = authService.selectTenant("user-op-001", request, httpRequest);

        assertEquals("full-access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
    }

    @Test
    void selectTenant_invalidTenant_throwsTenantAccessDenied() {
        when(userRepository.findById("user-op-001")).thenReturn(Optional.of(multiTenantUser));
        when(userTenantMappingRepository.findByUserIdAndTenantId("user-op-001", "INVALID"))
                .thenReturn(Optional.empty());

        SelectTenantRequest request = SelectTenantRequest.builder().tenantId("INVALID").build();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.selectTenant("user-op-001", request, httpRequest));
        assertEquals(ErrorCode.TENANT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void switchTenant_success() {
        when(userRepository.findById("user-op-001")).thenReturn(Optional.of(multiTenantUser));

        UserTenantMappingEntity mapping = UserTenantMappingEntity.builder()
                .userId("user-op-001").tenantId("TENANT_B")
                .roleId("ROLE_OPERATOR").enabled(true).build();
        mapping.setRole(operatorRole);
        when(userTenantMappingRepository.findByUserIdAndTenantId("user-op-001", "TENANT_B"))
                .thenReturn(Optional.of(mapping));

        Query permQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("role_permissions"))).thenReturn(permQuery);
        when(permQuery.setParameter(eq("roleId"), any())).thenReturn(permQuery);
        when(permQuery.setParameter(eq("tenantId"), any())).thenReturn(permQuery);
        when(permQuery.getResultList()).thenReturn(List.of("DEVICE_VIEW"));

        when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("new-refresh-token");

        SwitchTenantRequest request = SwitchTenantRequest.builder().tenantId("TENANT_B").build();
        TokenResult result = authService.switchTenant("user-op-001", request, httpRequest);

        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
    }

    @Test
    void switchTenant_noMapping_throwsTenantAccessDenied() {
        when(userRepository.findById("user-op-001")).thenReturn(Optional.of(multiTenantUser));
        when(userTenantMappingRepository.findByUserIdAndTenantId("user-op-001", "INVALID"))
                .thenReturn(Optional.empty());

        SwitchTenantRequest request = SwitchTenantRequest.builder().tenantId("INVALID").build();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.switchTenant("user-op-001", request, httpRequest));
        assertEquals(ErrorCode.TENANT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void refreshToken_success() {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("uid", "user-admin-001");
        claimsMap.put("tenantId", "TENANT_A"); // refresh token 帶入 tenantId
        claimsMap.put("type", "refresh");       // token 類型驗證需要此欄位
        claimsMap.put("sub", "user-admin-001");
        claimsMap.put("exp", new java.util.Date(System.currentTimeMillis() + 3600000));
        claimsMap.put("iat", new java.util.Date());
        Claims claims = new DefaultClaims(claimsMap);

        when(jwtUtil.parseToken("valid-refresh-token")).thenReturn(claims);
        when(userRepository.findById("user-admin-001")).thenReturn(Optional.of(singleTenantUser));

        UserTenantMappingEntity mapping = UserTenantMappingEntity.builder()
                .userId("user-admin-001").tenantId("TENANT_A")
                .roleId("ROLE_ADMIN").deptId(1L).enabled(true).build();
        mapping.setRole(adminRole);
        // 新逐輯：用 findByUserIdAndTenantId 保證租戶不漂移
        when(userTenantMappingRepository.findByUserIdAndTenantId("user-admin-001", "TENANT_A"))
                .thenReturn(Optional.of(mapping));

        Query permQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("role_permissions"))).thenReturn(permQuery);
        when(permQuery.setParameter(eq("roleId"), any())).thenReturn(permQuery);
        when(permQuery.setParameter(eq("tenantId"), any())).thenReturn(permQuery);
        when(permQuery.getResultList()).thenReturn(List.of("DEVICE_VIEW"));

        when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn("new-refresh-token");

        TokenResult result = authService.refreshToken("valid-refresh-token");

        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
    }

    @Test
    void refreshToken_invalid_throwsRefreshTokenInvalid() {
        when(jwtUtil.parseToken("bad-token")).thenThrow(new RuntimeException("invalid"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshToken("bad-token"));
        assertEquals(ErrorCode.REFRESH_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void getCurrentUser_success() {
        // getCurrentUser 是認證後 API，TenantContext 由 interceptor 設定
        TenantContext.setCurrentTenantId("TENANT_A");
        try {
        when(userRepository.findById("user-admin-001")).thenReturn(Optional.of(singleTenantUser));

        UserTenantMappingEntity mapping = UserTenantMappingEntity.builder()
                .userId("user-admin-001").tenantId("TENANT_A")
                .roleId("ROLE_ADMIN").deptId(1L).enabled(true).build();
        mapping.setRole(adminRole);
        when(userTenantMappingRepository.findByUserIdAndEnabledTrue("user-admin-001"))
                .thenReturn(List.of(mapping));

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantId("TENANT_A"); tenant.setTenantCode("KHH_WATER"); tenant.setTenantName("高雄市水情");
        when(tenantRepository.findById("TENANT_A")).thenReturn(Optional.of(tenant));

        Query permQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("role_permissions"))).thenReturn(permQuery);
        when(permQuery.setParameter(eq("roleId"), any())).thenReturn(permQuery);
        when(permQuery.setParameter(eq("tenantId"), any())).thenReturn(permQuery);
        when(permQuery.getResultList()).thenReturn(List.of("DEVICE_VIEW", "DEVICE_CREATE"));

        // 批次載入 dept 名稱（batchResolveDeptNames使用 IN :deptIds）
        Query deptBatchQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("IN :deptIds"))).thenReturn(deptBatchQuery);
        when(deptBatchQuery.setParameter(eq("deptIds"), any())).thenReturn(deptBatchQuery);
        when(deptBatchQuery.getResultList()).thenReturn(Collections.singletonList(new Object[]{1L, "水利工程科"}));

        // 單一 dept 查詢（resolveDeptName 使用 = :deptId）
        Query deptSingleQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("dept_id = :deptId"))).thenReturn(deptSingleQuery);
        when(deptSingleQuery.setParameter(eq("deptId"), any())).thenReturn(deptSingleQuery);
        when(deptSingleQuery.getResultList()).thenReturn(List.of("水利工程科"));

        UserInfoDto userInfo = authService.getCurrentUser("user-admin-001", "TENANT_A");

        assertEquals("user-admin-001", userInfo.getUserId());
        assertEquals("admin@test.com", userInfo.getEmail());
        assertEquals("TENANT_A", userInfo.getTenantId());
        assertEquals("高雄市水情", userInfo.getTenantName());
        assertFalse(userInfo.isSuperAdmin());
        assertEquals(List.of("ADMIN"), userInfo.getRoles());
        } finally {
            TenantContext.clear();
        }
    }

    // ---- TC-01-031: forgotPassword — generates token and sends email ----

    @Test
    void forgotPassword_existingUser_generatesTokenAndSendsEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("admin@test.com").build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(singleTenantUser));

        authService.forgotPassword(request);

        verify(userResetPasswordTokenRepository).save(any(UserResetPasswordTokenEntity.class));
        verify(passwordResetMailService).send(eq("admin@test.com"), any(), any());
    }

    @Test
    void forgotPassword_nonExistentUser_silentSuccess() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("nobody@test.com").build();
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request);

        verify(userResetPasswordTokenRepository, never()).save(any());
        verify(passwordResetMailService, never()).send(any(), any(), any());
    }

    // ---- TC-01-032: resetPassword — validates and resets ----

    @Test
    void resetPassword_validToken_resetsPasswordAndSavesHistory() {
        String tokenValue = "valid-token-uuid";
        UserResetPasswordTokenEntity tokenEntity = UserResetPasswordTokenEntity.builder()
                .tokenId("tid-1").userId("user-admin-001").token(tokenValue)
                .expiredAt(LocalDateTime.now().plusMinutes(15)).used(false).build();

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(tokenValue).newPassword("NewPass1234").build();

        when(userResetPasswordTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(tokenEntity));
        when(userRepository.findById("user-admin-001")).thenReturn(Optional.of(singleTenantUser));
        when(passwordEncoder.encode("NewPass1234")).thenReturn("encoded-hash");

        authService.resetPassword(request, httpRequest);

        verify(passwordValidator).validate("NewPass1234");
        verify(passwordValidator).checkNotRecentlyUsed("user-admin-001", "NewPass1234");
        verify(passwordHistoryRepository).save(any());
        verify(changePasswordLogRepository).save(argThat(log -> "RESET".equals(log.getChangeType())));
        assertTrue(tokenEntity.getUsed());
    }

    // ---- TC-01-033: resetPassword — expired token ----

    @Test
    void resetPassword_expiredToken_throwsException() {
        String tokenValue = "expired-token";
        UserResetPasswordTokenEntity tokenEntity = UserResetPasswordTokenEntity.builder()
                .tokenId("tid-2").userId("user-admin-001").token(tokenValue)
                .expiredAt(LocalDateTime.now().minusMinutes(1)).used(false).build();

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(tokenValue).newPassword("NewPass1234").build();

        when(userResetPasswordTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(tokenEntity));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(request, httpRequest));
        assertEquals(ErrorCode.RESET_PASSWORD_INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    void resetPassword_usedToken_throwsException() {
        String tokenValue = "used-token";
        UserResetPasswordTokenEntity tokenEntity = UserResetPasswordTokenEntity.builder()
                .tokenId("tid-3").userId("user-admin-001").token(tokenValue)
                .expiredAt(LocalDateTime.now().plusMinutes(15)).used(true).build();

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(tokenValue).newPassword("NewPass1234").build();

        when(userResetPasswordTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(tokenEntity));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(request, httpRequest));
        assertEquals(ErrorCode.RESET_PASSWORD_INVALID_TOKEN, ex.getErrorCode());
    }
}
