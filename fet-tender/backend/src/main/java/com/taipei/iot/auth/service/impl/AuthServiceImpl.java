package com.taipei.iot.auth.service.impl;

import com.taipei.iot.auth.dto.request.ChangePasswordRequest;
import com.taipei.iot.auth.dto.request.ForgotPasswordRequest;
import com.taipei.iot.auth.dto.request.LoginRequest;
import com.taipei.iot.auth.dto.request.ResetPasswordRequest;
import com.taipei.iot.auth.dto.request.SelectTenantRequest;
import com.taipei.iot.auth.dto.request.SwitchTenantRequest;
import com.taipei.iot.auth.dto.response.LoginResult;
import com.taipei.iot.auth.dto.response.TenantOption;
import com.taipei.iot.auth.dto.response.TokenResult;
import com.taipei.iot.auth.dto.response.UserInfoDto;
import com.taipei.iot.audit.entity.UserEventLogEntity;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.audit.repository.UserEventLogRepository;
import com.taipei.iot.auth.entity.ChangePasswordLogEntity;
import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.entity.UserResetPasswordTokenEntity;
import com.taipei.iot.auth.entity.UserTenantMappingEntity;
import com.taipei.iot.auth.repository.ChangePasswordLogRepository;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.auth.repository.UserResetPasswordTokenRepository;
import com.taipei.iot.auth.repository.UserTenantMappingRepository;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.auth.service.AuthService;
import com.taipei.iot.auth.service.CaptchaService;
import com.taipei.iot.auth.service.PasswordResetMailService;
import com.taipei.iot.auth.service.TurnstileService;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.enums.SecurityEvent;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityLogger;
import com.taipei.iot.user.entity.PasswordHistoryEntity;
import com.taipei.iot.user.repository.PasswordHistoryRepository;
import com.taipei.iot.user.service.PasswordValidator;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.tenant.TenantEntity;
import com.taipei.iot.tenant.TenantRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taipei.iot.common.util.JwtClaimKeys;
import com.taipei.iot.common.util.TenantAwareQuery;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String SUPER_ADMIN_ROLE_NAME = "超級管理員";

    private final UserRepository userRepository;
    private final UserTenantMappingRepository userTenantMappingRepository;
    private final UserEventLogRepository userEventLogRepository;
    private final ChangePasswordLogRepository changePasswordLogRepository;
    private final UserResetPasswordTokenRepository userResetPasswordTokenRepository;
    private final TenantRepository tenantRepository;
    private final CaptchaService captchaService;
    private final TurnstileService turnstileService;
    private final PasswordResetMailService passwordResetMailService;
    private final PasswordValidator passwordValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.security.lock.max-fail-count:5}")
    private int maxFailCount;

    @Value("${app.security.lock.lock-duration-minutes:10}")
    private int lockDurationMinutes;

    @Override
    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
        // 1. Verify captcha（支援圖片驗證碼 或 Turnstile 兩種方式）
        verifyCaptcha(request, httpRequest);

        // 2. Find user by email
        String clientIp = httpRequest.getRemoteAddr();
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElse(null);
        if (user == null) {
            SecurityLogger.warn(SecurityEvent.LOGIN_FAILED, clientIp,
                    "email=" + request.getEmail(), "reason=user_not_found");
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. Check enabled
        if (!user.getEnabled()) {
            SecurityLogger.warn(SecurityEvent.LOGIN_FAILED, clientIp,
                    "email=" + user.getEmail(), "reason=account_disabled");
            logLoginEvent(user.getUserId(), null, user.getEmail(), user.getDisplayName(), null,
                    AuditEventType.LOGIN_FAIL, "Account disabled", httpRequest);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 4. Check locked (with auto-unlock)
        if (user.getLocked()) {
            if (isLockExpired(user)) {
                // Auto-unlock
                user.setLocked(false);
                user.setLockedAt(null);
                user.setLoginFailCount(0);
                userRepository.save(user);
            } else {
                SecurityLogger.warn(SecurityEvent.LOGIN_FAILED, clientIp,
                        "email=" + user.getEmail(), "reason=account_locked");
                logLoginEvent(user.getUserId(), null, user.getEmail(), user.getDisplayName(), null,
                        AuditEventType.LOGIN_FAIL, "Account locked", httpRequest);
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
            }
        }

        // 5. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int newFailCount = user.getLoginFailCount() + 1;
            user.setLoginFailCount(newFailCount);

            if (newFailCount >= maxFailCount) {
                user.setLocked(true);
                user.setLockedAt(LocalDateTime.now());
            }

            userRepository.save(user);
            SecurityLogger.warn(SecurityEvent.LOGIN_FAILED, clientIp,
                    "email=" + user.getEmail(), "reason=bad_password",
                    "failCount=" + newFailCount);
            logLoginEvent(user.getUserId(), null, user.getEmail(), user.getDisplayName(), null,
                    AuditEventType.LOGIN_FAIL, "Wrong password (fail count: " + newFailCount + ")", httpRequest);
            throw new BusinessException(ErrorCode.LOGIN_FAIL);
        }

        // 6. Success — reset fail count
        user.setLoginFailCount(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 7. Query user_tenant_mapping (enabled = true) — bypass tenant filter
        List<UserTenantMappingEntity> mappings = queryMappingsWithSystemContext(
                () -> userTenantMappingRepository.findByUserIdAndEnabledTrue(user.getUserId()));

        // 8. Determine response
        if (user.getIsSuperAdmin()) {
            // SUPER_ADMIN → temporary token + all tenants
            String tempToken = jwtUtil.generateTemporaryToken(
                    user.getUserId(), user.getEmail(), true);

            List<TenantOption> tenantOptions = buildTenantOptionsForSuperAdmin();
            logLoginEvent(user.getUserId(), null, user.getEmail(), user.getDisplayName(), null,
                    AuditEventType.LOGIN_SUCCESS, "Super admin - needs tenant selection", httpRequest);

            return LoginResult.builder()
                    .accessToken(tempToken)
                    .needsSelection(true)
                    .isSuperAdmin(true)
                    .tenants(tenantOptions)
                    .build();
        }

        if (mappings.isEmpty()) {
            logLoginEvent(user.getUserId(), null, user.getEmail(), user.getDisplayName(), null,
                    AuditEventType.LOGIN_FAIL, "No tenant mapping", httpRequest);
            throw new BusinessException(ErrorCode.TENANT_ACCESS_DENIED);
        }

        if (mappings.size() == 1) {
            // Single tenant → full JWT
            UserTenantMappingEntity mapping = mappings.get(0);
            List<String> permissions = resolvePermissions(mapping.getRoleId(), mapping.getTenantId());

            String accessToken = jwtUtil.generateAccessToken(
                    user.getUserId(), user.getEmail(),
                    mapping.getTenantId(),
                    List.of(mapping.getRole() != null ? mapping.getRole().getCode() : ""),
                    mapping.getDeptId() != null ? mapping.getDeptId().toString() : null,
                    permissions,
                    mapping.getRole() != null ? mapping.getRole().getDataScope() : "ALL");
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), mapping.getTenantId());

            logLoginEvent(user.getUserId(), mapping.getTenantId(), user.getEmail(),
                    user.getDisplayName(), mapping.getDeptId(),
                    AuditEventType.LOGIN_SUCCESS, "Single tenant login", httpRequest);

            return LoginResult.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .needsSelection(false)
                    .build();
        }

        // Multi-tenant → temporary token + tenant list
        String tempToken = jwtUtil.generateTemporaryToken(
                user.getUserId(), user.getEmail(), false);
        List<TenantOption> tenantOptions = buildTenantOptions(mappings);

        logLoginEvent(user.getUserId(), null, user.getEmail(), user.getDisplayName(), null,
                AuditEventType.LOGIN_SUCCESS, "Multi-tenant - needs tenant selection", httpRequest);

        return LoginResult.builder()
                .accessToken(tempToken)
                .needsSelection(true)
                .tenants(tenantOptions)
                .build();
    }

    @Override
    @Transactional
    public TokenResult selectTenant(String userId, SelectTenantRequest request,
                                     HttpServletRequest httpRequest) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserTenantMappingEntity mapping;

        if (user.getIsSuperAdmin()) {
            // Super admin 需驗證租戶存在且已啟用
            TenantEntity requestedTenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
            if (!requestedTenant.getEnabled()) {
                throw new BusinessException(ErrorCode.TENANT_ACCESS_DENIED);
            }

            mapping = queryMappingsWithSystemContext(
                    () -> userTenantMappingRepository
                            .findByUserIdAndTenantId(userId, request.getTenantId())
                            .orElse(null));

            String roleCode = "SUPER_ADMIN";
            String deptId = mapping != null && mapping.getDeptId() != null ? mapping.getDeptId().toString() : null;
            List<String> permissions = resolvePermissions("ROLE_SUPER_ADMIN", request.getTenantId());

            String accessToken = jwtUtil.generateAccessToken(
                    userId, user.getEmail(), request.getTenantId(),
                    List.of(roleCode), deptId, permissions, "ALL");
            String refreshToken = jwtUtil.generateRefreshToken(userId, request.getTenantId());

            logLoginEvent(userId, request.getTenantId(), user.getEmail(),
                    user.getDisplayName(), mapping != null ? mapping.getDeptId() : null,
                    AuditEventType.LOGIN_SUCCESS, "Super admin selected tenant", httpRequest);

            return TokenResult.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        mapping = queryMappingsWithSystemContext(
                () -> userTenantMappingRepository
                        .findByUserIdAndTenantId(userId, request.getTenantId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_ACCESS_DENIED)));

        if (!mapping.getEnabled()) {
            throw new BusinessException(ErrorCode.TENANT_ACCESS_DENIED);
        }

        List<String> permissions = resolvePermissions(mapping.getRoleId(), mapping.getTenantId());
        String roleCode = mapping.getRole() != null ? mapping.getRole().getCode() : "";

        String accessToken = jwtUtil.generateAccessToken(
                userId, user.getEmail(), mapping.getTenantId(),
                List.of(roleCode), mapping.getDeptId() != null ? mapping.getDeptId().toString() : null,
                permissions, mapping.getRole() != null ? mapping.getRole().getDataScope() : "ALL");
        String refreshToken = jwtUtil.generateRefreshToken(userId, mapping.getTenantId());

        logLoginEvent(userId, mapping.getTenantId(), user.getEmail(),
                user.getDisplayName(), mapping.getDeptId(),
                AuditEventType.LOGIN_SUCCESS, "Tenant selected", httpRequest);

        return TokenResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public TokenResult switchTenant(String userId,
                                     SwitchTenantRequest request, HttpServletRequest httpRequest) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getIsSuperAdmin()) {
            // Super admin 需驗證租戶存在且已啟用
            TenantEntity requestedTenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
            if (!requestedTenant.getEnabled()) {
                throw new BusinessException(ErrorCode.TENANT_ACCESS_DENIED);
            }

            List<String> permissions = resolvePermissions("ROLE_SUPER_ADMIN", request.getTenantId());
            String accessToken = jwtUtil.generateAccessToken(
                    userId, user.getEmail(), request.getTenantId(),
                    List.of("SUPER_ADMIN"), null, permissions, "ALL");
            String refreshToken = jwtUtil.generateRefreshToken(userId, request.getTenantId());

            logLoginEvent(userId, request.getTenantId(), user.getEmail(),
                    user.getDisplayName(), null,
                    AuditEventType.TENANT_SWITCH, "Super admin switched tenant", httpRequest);

            return TokenResult.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        UserTenantMappingEntity mapping = queryMappingsWithSystemContext(
                () -> userTenantMappingRepository
                        .findByUserIdAndTenantId(userId, request.getTenantId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_ACCESS_DENIED)));

        if (!mapping.getEnabled()) {
            throw new BusinessException(ErrorCode.TENANT_ACCESS_DENIED);
        }

        List<String> permissions = resolvePermissions(mapping.getRoleId(), mapping.getTenantId());
        String roleCode = mapping.getRole() != null ? mapping.getRole().getCode() : "";

        String accessToken = jwtUtil.generateAccessToken(
                userId, user.getEmail(), mapping.getTenantId(),
                List.of(roleCode), mapping.getDeptId() != null ? mapping.getDeptId().toString() : null,
                permissions, mapping.getRole() != null ? mapping.getRole().getDataScope() : "ALL");
        String refreshToken = jwtUtil.generateRefreshToken(userId, mapping.getTenantId());

        logLoginEvent(userId, mapping.getTenantId(), user.getEmail(),
                user.getDisplayName(), mapping.getDeptId(),
                AuditEventType.TENANT_SWITCH, "Tenant switched", httpRequest);

        return TokenResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public TokenResult refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 驗證 token 類型：防止 access token 冒用為 refresh token
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String userId = claims.get("uid", String.class);
        if (userId == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        // 從 refresh token claims 取出發行時的 tenantId，沿用以避免多租戶環境下靜默切換租戶
        String tenantId = claims.get(JwtClaimKeys.TENANT_ID, String.class);

        List<String> roles;
        String deptId;
        List<String> permissions;
        String dataScope;

        if (user.getIsSuperAdmin()) {
            // 驗證租戶仍為啟用狀態，防止 SUPER_ADMIN 在租戶停用後繼續透過 refresh 延長授權
            if (tenantId != null) {
                TenantEntity refreshTenant = tenantRepository.findById(tenantId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
                if (!refreshTenant.getEnabled()) {
                    throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
                }
            }
            roles = List.of("SUPER_ADMIN");
            deptId = null;
            dataScope = "ALL";
            permissions = tenantId != null
                    ? resolvePermissions("ROLE_SUPER_ADMIN", tenantId)
                    : Collections.emptyList();
        } else {
            if (tenantId == null) {
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
            }
            // 查詢 refresh token 發行時對應的 tenantId mapping，確保租戶不漂移
            UserTenantMappingEntity mapping = queryMappingsWithSystemContext(
                    () -> userTenantMappingRepository
                            .findByUserIdAndTenantId(userId, tenantId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID)));
            if (!mapping.getEnabled()) {
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
            }
            roles = List.of(mapping.getRole() != null ? mapping.getRole().getCode() : "");
            deptId = mapping.getDeptId() != null ? mapping.getDeptId().toString() : null;
            dataScope = mapping.getRole() != null ? mapping.getRole().getDataScope() : "ALL";
            permissions = resolvePermissions(mapping.getRoleId(), tenantId);
        }

        String newAccessToken = jwtUtil.generateAccessToken(
                userId, user.getEmail(), tenantId, roles, deptId, permissions, dataScope);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, tenantId);

        return TokenResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        // Stateless JWT — cookie clearing is done by controller
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoDto getCurrentUser(String userId, String tenantId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<UserTenantMappingEntity> allMappings = queryMappingsWithSystemContext(
                () -> userTenantMappingRepository.findByUserIdAndEnabledTrue(userId));

        // Build available tenants list
        List<TenantOption> availableTenants;
        if (user.getIsSuperAdmin()) {
            availableTenants = buildTenantOptionsForSuperAdmin();
        } else {
            availableTenants = buildTenantOptions(allMappings);
        }

        // Current tenant context
        String currentTenantName = "";
        String roleCode = "";
        Long deptId = null;
        String deptName = null;
        List<String> permissions = Collections.emptyList();
        List<String> roles = Collections.emptyList();

        if (tenantId != null) {
            TenantEntity tenant = tenantRepository.findById(tenantId).orElse(null);
            currentTenantName = tenant != null ? tenant.getTenantName() : "";

            if (user.getIsSuperAdmin()) {
                roles = List.of("SUPER_ADMIN");
                permissions = resolvePermissions("ROLE_SUPER_ADMIN", tenantId);
            } else {
                UserTenantMappingEntity currentMapping = allMappings.stream()
                        .filter(m -> tenantId.equals(m.getTenantId()))
                        .findFirst()
                        .orElse(null);

                if (currentMapping != null) {
                    roleCode = currentMapping.getRole() != null
                            ? currentMapping.getRole().getCode() : "";
                    roles = List.of(roleCode);
                    deptId = currentMapping.getDeptId();
                    deptName = resolveDeptName(deptId);
                    permissions = resolvePermissions(currentMapping.getRoleId(), tenantId);
                }
            }
        }

        return UserInfoDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .tenantId(tenantId)
                .tenantName(currentTenantName)
                .roles(roles)
                .deptId(deptId != null ? deptId.toString() : null)
                .deptName(deptName)
                .permissions(permissions)
                .isSuperAdmin(user.getIsSuperAdmin())
                .availableTenants(availableTenants)
                .build();
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request,
                                HttpServletRequest httpRequest) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAIL);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        ChangePasswordLogEntity logEntity = ChangePasswordLogEntity.builder()
                .userId(userId)
                .changeType("USER")
                .ipAddress(httpRequest != null ? httpRequest.getRemoteAddr() : null)
                .build();
        changePasswordLogRepository.save(logEntity);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Silent success to avoid account enumeration
        UserEntity user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return;
        }

        String tokenValue = UUID.randomUUID().toString();
        UserResetPasswordTokenEntity tokenEntity = UserResetPasswordTokenEntity.builder()
                .tokenId(UUID.randomUUID().toString())
                .userId(user.getUserId())
                .token(tokenValue)
                .expiredAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        userResetPasswordTokenRepository.save(tokenEntity);

        // Send reset email
        passwordResetMailService.send(user.getEmail(), user.getDisplayName(), tokenValue);

        SecurityLogger.info(SecurityEvent.PASSWORD_RESET_REQUEST, "N/A",
                "email=" + user.getEmail());
        log.info("Reset password token generated and email sent for user {} (ref: {}...)",
                user.getEmail(), tokenValue.substring(0, 6));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        UserResetPasswordTokenEntity tokenEntity = userResetPasswordTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_PASSWORD_INVALID_TOKEN));

        if (tokenEntity.getUsed() || tokenEntity.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.RESET_PASSWORD_INVALID_TOKEN);
        }

        UserEntity user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Validate password complexity and history
        passwordValidator.validate(request.getNewPassword());
        passwordValidator.checkNotRecentlyUsed(user.getUserId(), request.getNewPassword());

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);

        // Save password history
        PasswordHistoryEntity historyEntity = PasswordHistoryEntity.builder()
                .userId(user.getUserId())
                .passwordHash(encodedPassword)
                .build();
        passwordHistoryRepository.save(historyEntity);

        tokenEntity.setUsed(true);
        userResetPasswordTokenRepository.save(tokenEntity);

        ChangePasswordLogEntity logEntity = ChangePasswordLogEntity.builder()
                .userId(user.getUserId())
                .changeType("RESET")
                .ipAddress(httpRequest != null ? httpRequest.getRemoteAddr() : null)
                .build();
        changePasswordLogRepository.save(logEntity);
    }

    // ---- Private helpers ----

    /**
     * Execute a repository query with system context to bypass tenantFilter.
     * Auth flows (login, selectTenant, switchTenant, refreshToken) are cross-tenant by nature.
     */
    private <T> T queryMappingsWithSystemContext(java.util.function.Supplier<T> query) {
        String previous = TenantContext.getCurrentTenantId();
        try {
            TenantContext.setSystemContext();
            return query.get();
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenantId(previous);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * 驗證 CAPTCHA — 支援兩種模式：
     * <ul>
     *   <li>Turnstile：前端傳入 turnstileToken，後端呼叫 Cloudflare API 驗證</li>
     *   <li>圖片驗證碼：前端傳入 captchaKey + captcha，後端比對 Redis 中的值</li>
     * </ul>
     * 如果兩種都沒傳，或驗證失敗，拋出 BusinessException。
     */
    private void verifyCaptcha(LoginRequest request, HttpServletRequest httpRequest) {
        // 優先使用 Turnstile（如果前端傳了 turnstileToken）
        if (request.getTurnstileToken() != null && !request.getTurnstileToken().isBlank()) {
            if (!turnstileService.isEnabled()) {
                throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
            }
            String captchaClientIp = httpRequest.getRemoteAddr();
            if (!turnstileService.verify(request.getTurnstileToken(), captchaClientIp)) {
                SecurityLogger.warn(SecurityEvent.CAPTCHA_FAILED, captchaClientIp,
                        "type=turnstile");
                throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
            }
            return;
        }

        // 退回使用圖片驗證碼
        if (request.getCaptchaKey() == null || request.getCaptchaKey().isBlank()
                || request.getCaptcha() == null || request.getCaptcha().isBlank()) {
            SecurityLogger.warn(SecurityEvent.CAPTCHA_FAILED, httpRequest.getRemoteAddr(),
                    "type=image", "reason=missing_fields");
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        if (!captchaService.verify(request.getCaptchaKey(), request.getCaptcha())) {
            SecurityLogger.warn(SecurityEvent.CAPTCHA_FAILED, httpRequest.getRemoteAddr(),
                    "type=image");
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
    }

    private boolean isLockExpired(UserEntity user) {
        if (user.getLockedAt() == null) {
            return false;
        }
        return user.getLockedAt().plusMinutes(lockDurationMinutes).isBefore(LocalDateTime.now());
    }

    @SuppressWarnings("unchecked")
    private List<String> resolvePermissions(String roleId, String tenantId) {
        // SUPER_ADMIN 擁有所有權限（不透過 role_permissions 表）
        if ("ROLE_SUPER_ADMIN".equals(roleId)) {
            String allPermsSql = "SELECT p.code FROM permissions p ORDER BY p.code";
            return queryMappingsWithSystemContext(() -> {
                Query query = TenantAwareQuery.create(entityManager, allPermsSql);
                return query.getResultList();
            });
        }

        // role_permissions 有 tenant_id 欄位 → 使用 TenantAwareQuery
        // 在 auth 流程（login / selectTenant / refreshToken）中，TenantContext 尚未設定，
        // 因此使用 System Context 包裝。租戶隔離由明確的 :tenantId 參數保證。
        String sql = "SELECT DISTINCT p.code FROM role_permissions rp " +
                "JOIN permissions p ON p.permission_id = rp.permission_id " +
                "WHERE rp.role_id = :roleId " +
                "AND (rp.tenant_id = :tenantId OR rp.tenant_id IS NULL) " +
                "ORDER BY p.code";

        return queryMappingsWithSystemContext(() -> {
            Query query = TenantAwareQuery.create(entityManager, sql);
            query.setParameter("roleId", roleId);
            query.setParameter("tenantId", tenantId);
            return query.getResultList();
        });
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        // dept_info 有 tenant_id 欄位 → 加入 tenant_id 條件防止跨租戶查詢
        // [安全修復] 原本只 WHERE dept_id = :deptId，缺少租戶隔離
        String sql = "SELECT d.dept_name FROM dept_info d " +
                "WHERE d.dept_id = :deptId AND d.tenant_id = :tenantId";
        Query query = TenantAwareQuery.create(entityManager, sql);
        query.setParameter("deptId", deptId);

        @SuppressWarnings("unchecked")
        List<String> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private List<TenantOption> buildTenantOptions(List<UserTenantMappingEntity> mappings) {
        // 批次載入所有相關 TenantEntity，避免 N+1 查詢
        Set<String> tenantIds = mappings.stream()
                .map(UserTenantMappingEntity::getTenantId)
                .collect(Collectors.toSet());
        Map<String, TenantEntity> tenantMap = tenantRepository.findAllById(tenantIds)
                .stream()
                .collect(Collectors.toMap(TenantEntity::getTenantId, Function.identity()));

        // 批次載入所有相關 deptName，避免 N+1 查詢
        Set<Long> deptIds = mappings.stream()
                .map(UserTenantMappingEntity::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Collections.emptyMap()
                : batchResolveDeptNames(deptIds);

        return mappings.stream()
                .map(m -> {
                    TenantEntity tenant = tenantMap.get(m.getTenantId());
                    String roleName = m.getRole() != null ? m.getRole().getName() : "";
                    String deptName = m.getDeptId() != null ? deptNameMap.get(m.getDeptId()) : null;

                    return TenantOption.builder()
                            .tenantId(m.getTenantId())
                            .tenantCode(tenant != null ? tenant.getTenantCode() : "")
                            .tenantName(tenant != null ? tenant.getTenantName() : "")
                            .roleName(roleName)
                            .deptName(deptName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<TenantOption> buildTenantOptionsForSuperAdmin() {
        return tenantRepository.findByEnabledTrue().stream()
                .map(t -> TenantOption.builder()
                        .tenantId(t.getTenantId())
                        .tenantCode(t.getTenantCode())
                        .tenantName(t.getTenantName())
                        .roleName(SUPER_ADMIN_ROLE_NAME)
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> batchResolveDeptNames(Set<Long> deptIds) {
        // dept_info 有 tenant_id 欄位 → 加入 tenant_id 條件防止跨租戶查詢
        // [安全修復] 原本只 WHERE dept_id IN :deptIds，缺少租戶隔離
        String sql = "SELECT d.dept_id, d.dept_name FROM dept_info d " +
                "WHERE d.dept_id IN :deptIds AND d.tenant_id = :tenantId";
        List<Object[]> rows = TenantAwareQuery.create(entityManager, sql)
                .setParameter("deptIds", deptIds)
                .getResultList();
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> (String) r[1]
        ));
    }

    private void logLoginEvent(String userId, String tenantId, String email,
                                String displayName, Long deptId,
                                AuditEventType eventType, String detail,
                                HttpServletRequest request) {
        UserEventLogEntity entity = new UserEventLogEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setUsername(email);
        entity.setUserLabel(displayName);
        entity.setEmail(email);
        entity.setEventType(eventType.getValue());
        entity.setEventDesc(eventType.getCategory().getValue());
        entity.setApiEndpoint(request != null ? request.getRequestURI() : null);
        entity.setErrorCode(eventType.errorCode());
        entity.setMessage(detail);
        entity.setIpAddress(request != null ? request.getRemoteAddr() : null);
        entity.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        entity.setExecutionTime(0L);
        entity.setDeptId(deptId);
        entity.setCreateTime(LocalDateTime.now());

        // 使用 SYSTEM context 繞過 tenant filter 存檔（登入時可能尚無 tenant context）
        String previous = TenantContext.getCurrentTenantId();
        try {
            TenantContext.setSystemContext();
            userEventLogRepository.save(entity);
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenantId(previous);
            } else {
                TenantContext.clear();
            }
        }
    }
}
