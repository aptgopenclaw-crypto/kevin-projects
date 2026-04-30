package com.taipei.iot.common.util;

import com.taipei.iot.common.dto.UserInfo;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public final class SecurityContextUtils {

    private SecurityContextUtils() {}

    /**
     * 回傳已認證使用者的 userId（即 JWT subject）。
     * <p>
     * 未認證或 anonymous 使用者一律回傳 {@code null}。
     * <p>
     * <strong>⚠ 執行緒限制：</strong>本方法依賴 {@link SecurityContextHolder} 的 ThreadLocal，
     * 在 {@code @Async} 方法、{@code CompletableFuture} 等非同步執行緒中將回傳 {@code null}。
     * 如需在非同步上下文中取得使用者識別碼，請將 userId 作為參數傳入，
     * 或在應用程式啟動時設定
     * {@code SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)}。
     */
    public static String getCurrentUserId() {
        Authentication auth = getValidAuth();
        if (auth == null) return null;
        return auth.getPrincipal().toString();
    }

    /**
     * 回傳已認證使用者的帳號名稱。
     * <p>
     * 未認證或 anonymous 使用者一律回傳 {@code null}。
     * <p>
     * <strong>注意：</strong>在目前的 JWT 設計中，principal 即為帳號名稱（JWT subject），
     * 因此本方法與 {@link #getCurrentUserId()} 回傳值相同。
     * 若未來需要區分「顯示用帳號名稱」與「不可變唯一 ID」，
     * 應在 JWT 中加入獨立的 {@code username} claim 並從 details 取出。
     * <p>
     * <strong>⚠ 執行緒限制：</strong>同 {@link #getCurrentUserId()}，在 {@code @Async} 或非同步執行緒中將回傳 {@code null}。
     */
    public static String getCurrentUsername() {
        Authentication auth = getValidAuth();
        if (auth == null) return null;
        return auth.getName();
    }

    /**
     * 從 {@link Authentication#getDetails()} 組裝完整的 {@link UserInfo}。
     * <p>
     * JWT filter 負責將使用者資訊存入 details（型態為 {@code Map<String, Object>}）；
     * 若 details 不符合預期型態，則回傳僅含 userId / username 的基本物件。
     * <p>
     * <strong>⚠ 執行緒限制：</strong>同 {@link #getCurrentUserId()}，在 {@code @Async} 或非同步執行緒中將回傳 {@code null}。
     */
    public static UserInfo getUserInfo() {
        Authentication auth = getValidAuth();
        if (auth == null) return null;

        Object details = auth.getDetails();
        if (details instanceof Map) {
            // JWT filter 保證 details 為 Map<String, Object>，此轉型安全
            @SuppressWarnings("unchecked")
            Map<String, Object> detailsMap = (Map<String, Object>) details;

            Long deptId = null;
            Object deptIdVal = detailsMap.get(JwtClaimKeys.DEPT_ID);
            if (deptIdVal instanceof Number) {
                deptId = ((Number) deptIdVal).longValue();
            } else if (deptIdVal instanceof String deptIdStr && !deptIdStr.isEmpty()) {
                try {
                    deptId = Long.parseLong(deptIdStr);
                } catch (NumberFormatException ignored) {
                }
            }

            // 使用 instanceof pattern，避免非 String 型別時 ClassCastException
            String tenantId  = detailsMap.get(JwtClaimKeys.TENANT_ID)  instanceof String s ? s : null;
            String dataScope = detailsMap.get(JwtClaimKeys.DATA_SCOPE) instanceof String s ? s : null;

            return UserInfo.builder()
                    .userId(auth.getPrincipal().toString())
                    .username(auth.getName())
                    .tenantId(tenantId)
                    .deptId(deptId)
                    .dataScope(dataScope)
                    .build();
        }

        return UserInfo.builder()
                .userId(auth.getPrincipal().toString())
                .username(auth.getName())
                .build();
    }

    // --- private helpers ---

    /**
     * 取得有效（已認證且非 anonymous）的 Authentication；否則回傳 {@code null}。
     */
    private static Authentication getValidAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || isAnonymous(auth)) return null;
        return auth;
    }

    /**
     * 判斷是否為 anonymous 使用者。
     * <p>
     * 使用型別判斷而非字串比對，確保即使 anonymous principal 被自訂
     * （透過 {@code HttpSecurity.anonymous().principal(...)}），行為仍然正確。
     */
    private static boolean isAnonymous(Authentication auth) {
        return auth instanceof AnonymousAuthenticationToken;
    }
}

