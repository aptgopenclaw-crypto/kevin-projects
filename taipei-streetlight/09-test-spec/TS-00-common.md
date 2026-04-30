# TS-00 共用基礎 — Test Specification

> **對應 SA**：SA-11-common (FN-00-001 ~ FN-00-030)  
> **對應 SD**：SD-11-common  
> **Test Classes**：21 classes, 137 test methods  
> **最新驗證**：2026-04-24 · Commit `c290a3b` · 全部 PASS

---

## 使用方式

本文件是 **AI 實作 prompt 的驗收標準**。請 AI 實作或修改某個 FN 時，附上對應段落：

```
請實作 FN-00-001 JWT 核發
【SA】SA-11 §認證核心
【SD】SD-11 §1.2 POST /v1/auth/login
【TC】（貼 FN-00-001 的 Test Cases 表）
請確保所有 TC PASS。
```

---

## FN-00-001 JWT 核發

**SA**: SA-11 §認證核心 | **SD**: SD-11 §1.2 | **API**: `POST /v1/noauth/login`  
**Service**: `AuthService.login()` | **SRS**: SRS-01-001 | **Spec**: §2-6

**商業規則**：
- bcrypt 驗證密碼
- 登入失敗 5 次 → 鎖定帳號 10 分鐘（configurable）
- 多租戶用戶 → 回傳 temp-token + tenantList，`needsSelection=true`
- 單租戶用戶 → 直接回傳 accessToken + refreshToken

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-001-01 | Happy | 單租戶正常登入 | 用戶有 1 個 tenant mapping | email + 正確密碼 + captcha | 200, accessToken + refreshToken, `needsSelection=false` | body.accessToken 非 null | ✅ AuthServiceTest.login_singleTenant_success |
| TC-00-001-02 | Happy | 多租戶登入 | 用戶有 2+ tenant mapping | email + 正確密碼 | 200, temp-token, `needsSelection=true`, tenants 陣列 | tenants.size() == 2 | ✅ AuthServiceTest.login_multiTenant_success |
| TC-00-001-03 | Happy | 超級管理員登入 | SUPER_ADMIN 角色 | email + 正確密碼 | 200, super-temp-token, 所有 enabled tenants | needsSelection=true | ✅ AuthServiceTest.login_superAdmin_success |
| TC-00-001-04 | Error | 密碼錯誤 | 用戶存在 | 錯誤密碼 | 401, `ErrorCode.LOGIN_FAIL` (10013) | errorCode == "10013" | ✅ AuthServiceTest.login_wrongPassword + AuthControllerTest.login_wrongPassword_returns401 |
| TC-00-001-05 | Error | 帳號不存在 | 無此 email | 不存在的 email | 404, `ErrorCode.USER_NOT_FOUND` (20005) | errorCode == "20005" | ✅ AuthServiceTest.login_userNotFound + AuthControllerTest |
| TC-00-001-06 | Error | 帳號停用 | `enabled=false` | 正確密碼 | 401, `ErrorCode.ACCOUNT_DISABLED` (10003) | errorCode == "10003" | ✅ AuthServiceTest.login_accountDisabled + AuthControllerTest |
| TC-00-001-07 | Error | 帳號鎖定 | failCount ≥ 5, `locked=true` | 正確密碼 | 401, `ErrorCode.ACCOUNT_LOCKED` (10002) | errorCode == "10002" | ✅ AuthServiceTest.login_accountLocked + AuthControllerTest |
| TC-00-001-08 | Error | 無租戶權限 | 0 tenant mappings | 正確密碼 | 403, `ErrorCode.TENANT_ACCESS_DENIED` (10021) | errorCode == "10021" | ✅ AuthServiceTest.login_noTenantMapping |
| TC-00-001-09 | Error | 驗證碼錯誤 | — | 錯誤 captchaAnswer | 400, `ErrorCode.CAPTCHA_INVALID` (10007) | errorCode == "10007" | ✅ AuthControllerTest.login_captchaInvalid_returns400 |
| TC-00-001-10 | API | HTTP 401 無 token | 無 Authorization header | — | 401 | HTTP status | ✅ AuthControllerTest |

---

## FN-00-002 JWT 驗證 (Filter)

**SA**: SA-11 §認證核心 | **SD**: SD-11 §1.3 | **API**: (JwtAuthenticationFilter)  
**Service**: `JwtAuthenticationFilter` + `JwtTokenProvider` | **SRS**: SRS-01-001 | **Spec**: §2-6

**商業規則**：
- OncePerRequestFilter：解析 Authorization: Bearer {token}
- Token 過期 / 簽名錯誤 → 401
- 成功 → 設定 SecurityContext

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-002-01 | Happy | 有效 Token 通過 | JWT 未過期 | Bearer {validToken} | SecurityContext 設定成功 | API 返回 200 | ✅ AuthControllerTest (多個 API 測試間接驗證) |
| TC-00-002-02 | Error | Token 過期 | JWT 已過期 | Bearer {expiredToken} | 401 | HTTP 401 | ✅ AuthControllerTest.selectTenant_noToken_returns401 |
| TC-00-002-03 | Error | 無 Token | 無 header | — | 401 | HTTP 401 | ✅ AuthControllerTest.selectTenant_noToken_returns401 |
| TC-00-002-04 | Edge | Token 格式錯誤 | — | Bearer invalid-string | 401, errorCode "10001" | HTTP 401 | ✅ AuthControllerTest.authApi_malformedToken_returns401 |

---

## FN-00-003 Token 刷新

**SA**: SA-11 §認證核心 | **SD**: SD-11 §1.2 | **API**: `POST /v1/noauth/token/refresh`  
**Service**: `AuthService.refreshToken()` | **SRS**: SRS-01-001 | **Spec**: §2-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-003-01 | Happy | 正常刷新 | 有效 refreshToken (含 tenantId claim) | Cookie: refresh_token={token} | 200, 新 accessToken + refreshToken | body.accessToken 非 null | ✅ AuthServiceTest.refreshToken_success + AuthControllerTest.refreshToken_success |
| TC-00-003-02 | Error | 無效 Token | 隨機字串 | Cookie: refresh_token=bad | 401, `REFRESH_TOKEN_INVALID` (10004) | errorCode == "10004" | ✅ AuthServiceTest.refreshToken_invalid + AuthControllerTest.refreshToken_noCookie_returns401 |
| TC-00-003-03 | Edge | 租戶漂移防護 | Token 的 tenantId 與 user mapping 不符 | refresh 請求 | 驗證 `findByUserIdAndTenantId` | 不會發出跨租戶 token | ✅ AuthServiceTest.refreshToken_success (驗證 tenantId 查詢) |

---

## FN-00-004 登出

**SA**: SA-11 §認證核心 | **SD**: SD-11 §1.2 | **API**: `POST /v1/auth/logout`  
**Service**: `AuthService.logout()` | **SRS**: SRS-01-001 | **Spec**: §2-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-004-01 | Happy | 正常登出 | 已登入 | Bearer {validToken} | 200, errorCode "00000" | Token 失效 | ✅ AuthControllerTest.logout_success |
| TC-00-004-02 | Error | 未登入登出 | 無 token | — | 401 | HTTP 401 | ✅ (Filter 攔截) |

---

## FN-00-005 驗證碼產生

**SA**: SA-11 §認證核心 | **SD**: SD-11 §1.2 | **API**: `POST /v1/noauth/captcha`  
**Service**: `CaptchaService.generate()` | **SRS**: SRS-01-001 | **Spec**: §2-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-005-01 | Happy | 產生驗證碼 | — | POST 請求 | 200, captchaKey + captchaImage (Base64) | 兩欄位非 null | ✅ CaptchaServiceTest.generate + AuthControllerTest.captcha_shouldReturn200 |
| TC-00-005-02 | Security | 超過頻率限制 | 同一 IP 已請求 20 次/分 | 第 21 次 POST | 429, `RATE_LIMIT_EXCEEDED` (10030), `Retry-After` header | HTTP 429 + errorCode | ✅ RateLimitInterceptorTest.preHandle_exceedLimit_shouldReturn429 |

---

## FN-00-006 驗證碼驗證

**SA**: SA-11 §認證核心 | **SD**: SD-11 (內部) | **API**: 內部 Service  
**Service**: `CaptchaService.verify()` | **SRS**: SRS-01-001 | **Spec**: §2-6

**商業規則**：5 分鐘有效期

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-006-01 | Happy | 正確驗證 | captcha 已產生 | key + 正確 answer | true | 回傳 true | ✅ CaptchaServiceTest.verify_shouldReturnTrue |
| TC-00-006-02 | Error | 答案錯誤 | captcha 已產生 | key + 錯誤 answer | false (NoOp: true) | — | ✅ CaptchaServiceTest.verify_wrongInput (NoOp impl) |
| TC-00-006-03 | Error | 過期 | captcha 超過 5 分鐘 | 過期 key | false (NoOp: true) | — | ✅ CaptchaServiceTest.verify_expired (NoOp impl) |
| TC-00-006-04 | Edge | 大小寫不敏感 | — | "AbCd" vs "abcd" | true | — | ✅ CaptchaServiceTest.verify_caseInsensitive |

> ⚠️ **注意**：測試環境使用 `NoOpCaptchaServiceImpl`（`@Profile("test")`，永遠回傳 true）。
> 正式/開發環境使用 `CaptchaServiceImpl`（`@Profile("!test")`）：Redis-backed + 圖片驗證碼 + 一次性驗證。
> 環境已部署 Redis，`CaptchaServiceImpl` 為實際運行的實作。

---

## FN-00-006a 速率限制 (Rate Limiting)

**SA**: SA-11 §安全機制 | **SD**: SD-11 §4 Rate Limiting | **API**: `@RateLimit` 攔截器  
**Service**: `RateLimitInterceptor` + `@RateLimit` 註解 | **SRS**: SRS-NFR-001 | **Spec**: §2-6

**商業規則**：
- 所有 noauth endpoint 必須有 `@RateLimit`
- Redis INCR + EXPIRE 固定窗口計數器
- 超限回 429 + `Retry-After` header + `ErrorCode.RATE_LIMIT_EXCEEDED` (10030)
- Redis 不可用時 fail-open（不阻斷服務）
- IP 取自 `request.getRemoteAddr()`（不信任 X-Forwarded-For，防繞過）

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-006a-01 | Happy | 無 @RateLimit 放行 | 方法無註解 | 任意請求 | 放行 | 不呼叫 Redis | ✅ RateLimitInterceptorTest.preHandle_noAnnotation_shouldPassThrough |
| TC-00-006a-02 | Happy | 非 HandlerMethod 放行 | 靜態資源 | — | 放行 | 不呼叫 Redis | ✅ RateLimitInterceptorTest.preHandle_notHandlerMethod_shouldPassThrough |
| TC-00-006a-03 | Happy | 限制內放行 | count=5, limit=10 | 請求 | 放行 | 不回 429 | ✅ RateLimitInterceptorTest.preHandle_underLimit_shouldAllow |
| TC-00-006a-04 | Happy | 首次請求設 TTL | count=1 | 首次請求 | 放行 + EXPIRE | expire 被呼叫 | ✅ RateLimitInterceptorTest.preHandle_firstRequest_shouldSetExpire |
| TC-00-006a-05 | Happy | 非首次不重設 TTL | count=3 | 後續請求 | 放行 | expire 不被呼叫 | ✅ RateLimitInterceptorTest.preHandle_subsequentRequest_shouldNotResetExpire |
| TC-00-006a-06 | Security | 超限攔截 | count=11, limit=10 | 第 11 次請求 | 429, errorCode "10030", Retry-After header | HTTP 429 | ✅ RateLimitInterceptorTest.preHandle_exceedLimit_shouldReturn429 |
| TC-00-006a-07 | Edge | 恰好等於 limit | count=10, limit=10 | 第 10 次請求 | 放行（limit inclusive） | 不回 429 | ✅ RateLimitInterceptorTest.preHandle_atExactLimit_shouldStillAllow |
| TC-00-006a-08 | Edge | Redis 不可用 | Redis 連線失敗 | 請求 | 放行 (fail-open) | 不回 429 | ✅ RateLimitInterceptorTest.preHandle_redisUnavailable_shouldFailOpen |
| TC-00-006a-09 | Edge | INCR 回傳 null | Redis 異常 | 請求 | 放行 | 容錯 | ✅ RateLimitInterceptorTest.preHandle_incrReturnsNull_shouldPassThrough |
| TC-00-006a-10 | Security | 使用 remoteAddr | 不信任 X-Forwarded-For | 請求 | 使用 TCP IP | 不呼叫 getHeader | ✅ RateLimitInterceptorTest.preHandle_shouldUseRemoteAddr_notForwardedHeader |
| TC-00-006a-11 | Edge | TTL=null 不設 Retry-After | Redis TTL 回 null | 超限請求 | 429 但無 Retry-After | 容錯 | ✅ RateLimitInterceptorTest.preHandle_exceedLimit_nullTtl_shouldNotSetRetryAfter |
| TC-00-006a-12 | Happy | 不同 key 不同計數器 | key=forgot-pwd | 請求 | Redis key 含 forgot-pwd | key 隔離 | ✅ RateLimitInterceptorTest.preHandle_differentKeys_shouldUseDifferentRedisKeys |

---

## FN-00-007 權限檢查 (AOP)

**SA**: SA-11 §授權核心 | **SD**: SD-11 §RBAC | **API**: `@PreAuthorize` AOP  
**Service**: Spring Security + `SecurityContextUtils` | **SRS**: SRS-01-002 | **Spec**: §2-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-007-01 | Happy | 有權限通過 | ADMIN 角色 | 呼叫需 ADMIN 權限的 API | 200 | 正常執行 | ✅ AuditControllerTest.getUserUsageHistory_shouldReturn200ForAdmin |
| TC-00-007-02 | Error | 無權限拒絕 | VIEWER 角色 | 呼叫需 ADMIN 權限的 API | 403 | HTTP 403 | ✅ AuditControllerTest.getUserUsageHistory_shouldReturn403ForViewer |
| TC-00-007-03 | Happy | SUPER_ADMIN 通過 | SUPER_ADMIN 角色 | 呼叫 admin API | 200 | 跨租戶也可 | ✅ AuditControllerTest.getUserUsageHistory_shouldReturn200ForSuperAdmin |

---

## FN-00-008 多租戶過濾 (JPA Filter)

**SA**: SA-11 §授權核心 | **SD**: SD-11 §Tenant | **API**: Hibernate `@Filter`  
**Service**: `TenantFilterAspect` + `TenantEntityListener` | **SRS**: SRS-01-002 | **Spec**: §2-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-008-01 | Happy | 設定並取得 tenantId | — | `TenantContext.set("T1")` | `get()` 回傳 "T1" | 值一致 | ✅ TenantContextTest.setAndGet |
| TC-00-008-02 | Happy | 清除 context | 已設定 "T1" | `clear()` | `get()` 回傳 null | null | ✅ TenantContextTest.clear |
| TC-00-008-03 | Happy | 系統 context | — | `setSystemContext()` | `isSystemContext()` = true | boolean | ✅ TenantContextTest.setSystemContext |
| TC-00-008-04 | Happy | Entity 自動填 tenantId | Entity.tenantId = null, context = "T1" | `prePersist` | entity.tenantId = "T1" | 自動設定 | ✅ TenantEntityListenerTest.prePersist_shouldSetTenantId |
| TC-00-008-05 | Edge | 不覆蓋已有 tenantId | Entity.tenantId = "T2", context = "T1" | `prePersist` | entity.tenantId = "T2" | 保持原值 | ✅ TenantEntityListenerTest.prePersist_shouldNotOverride |
| TC-00-008-06 | Happy | 單租戶模式 | mode="single" | 任意請求 | TenantContext = defaultId | 預設值 | ✅ TenantInterceptorTest.singleMode |
| TC-00-008-07 | Happy | 多租戶模式 | mode="multi" | 任意請求 | TenantContext = null (由 JWT 帶入) | null | ✅ TenantInterceptorTest.multiMode |
| TC-00-008-08 | Happy | 清除 context (Interceptor) | 已設定值 | afterCompletion | context = null | 清除 | ✅ TenantInterceptorTest.afterCompletion |

---

## FN-00-009 目前使用者取得

**SA**: SA-11 §授權核心 | **SD**: SD-11 (內部) | **API**: 內部工具  
**Service**: `SecurityContextUtils.getCurrentUser()` | **SRS**: SRS-01-002 | **Spec**: §2-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-009-01 | Happy | 已認證取得 userId | SecurityContext 有 auth | — | 回傳 "user-001" | userId 正確 | ✅ SecurityContextUtilsTest.getCurrentUserId_whenAuthenticated |
| TC-00-009-02 | Edge | 未認證回傳 null | 無 SecurityContext | — | null | 不拋例外 | ✅ SecurityContextUtilsTest.getCurrentUserId_whenUnauthenticated |
| TC-00-009-03 | Edge | 匿名回傳 null | AnonymousAuthenticationToken | — | null | 不拋例外 | ✅ SecurityContextUtilsTest.getCurrentUserId_whenAnonymous |
| TC-00-009-04 | Happy | 完整 UserInfo | Details 含 tenantId, deptId, dataScope | — | 完整 UserInfo DTO | 全欄位正確 | ✅ SecurityContextUtilsTest.getUserInfo_withFullDetails |
| TC-00-009-05 | Edge | deptId 字串→Long | deptId = "12" (String) | — | parsed 為 12L | 型別轉換 | ✅ SecurityContextUtilsTest.getUserInfo_deptIdAsString |
| TC-00-009-06 | Edge | deptId 無效字串 | deptId = "not-a-number" | — | deptId = null, 不拋例外 | 容錯 | ✅ SecurityContextUtilsTest.getUserInfo_deptIdInvalidString |
| TC-00-009-07 | Edge | Details 非 Map | Details 是 plain string | — | userId only, tenantId = null | 降級處理 | ✅ SecurityContextUtilsTest.getUserInfo_withoutMapDetails |
| TC-00-009-08 | Edge | 匿名取 UserInfo | Anonymous | — | null | 安全 | ✅ SecurityContextUtilsTest.getUserInfo_whenAnonymous |

---

## FN-00-010 操作日誌紀錄 (AOP)

**SA**: SA-11 §稽核日誌 | **SD**: SD-11 §2.1 | **API**: `@AuditLog` AOP  
**Service**: `BaseLoggerAspect` + `AuditAsyncWriter` | **SRS**: SRS-01-004 | **Spec**: §2-7

**商業規則**：
- `@Async` 非同步寫入，不阻塞業務
- `PayloadSanitizer` 脫敏密碼/Token
- 捕獲 BusinessException 的 errorCode
- 提取 X-Forwarded-For 的第一個 IP

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-010-01 | Happy | 正常紀錄 | 標註 @AuditLog 的方法 | API 呼叫 | auditAsyncWriter.saveAsync() 被呼叫 | tenantId, userId, eventType, IP, errorCode="00000" | ✅ BaseLoggerAspectTest.logApiCall_shouldDelegateToAsyncWriter |
| TC-00-010-02 | Error | BusinessException 捕獲 | 方法拋出 BusinessException(LOGIN_FAIL) | — | saveAsync 帶 errorCode "10013"，例外被重拋 | errorCode 正確 | ✅ BaseLoggerAspectTest.logApiCall_shouldCaptureErrorCodeOnBusinessException |
| TC-00-010-03 | Error | 未知例外 | RuntimeException | — | errorCode = "99999"，例外被重拋 | 預設 errorCode | ✅ BaseLoggerAspectTest.logApiCall_shouldUseDefaultErrorCodeOnGenericException |
| TC-00-010-04 | Edge | X-Forwarded-For 解析 | Header: "10.0.0.1, 172.16.0.1" | — | IP = "10.0.0.1" | 取第一個 | ✅ BaseLoggerAspectTest.logApiCall_shouldExtractClientIpFromXForwardedFor |
| TC-00-010-05 | Happy | 寫入成功 | 完整參數 | saveAsync 全參數 | Entity 持久化，含 displayName, email, deptId | 全欄位 | ✅ AuditAsyncWriterTest.saveAsync_shouldPersistEntity |
| TC-00-010-06 | Edge | DB 失敗容錯 | DB 拋 RuntimeException | — | 例外被吞（best-effort），不影響業務 | 不拋例外 | ✅ AuditAsyncWriterTest.saveAsync_shouldNotThrowOnDbFailure |
| TC-00-010-07 | Edge | 無 tenantId | 超級管理員操作 | tenantId = null | Entity 存入 null tenantId | 可為 null | ✅ AuditAsyncWriterTest.saveAsync_shouldHandleNullTenantId |
| TC-00-010-08 | Happy | 密碼脫敏 | payload 含 password | `{password:"abc123"}` | password → "***" | 遮蔽 | ✅ PayloadSanitizerTest.sanitize_shouldMaskPassword |
| TC-00-010-09 | Happy | Token 脫敏 | payload 含 accessToken | `{accessToken:"eyJ..."}` | token → "***" | 遮蔽 | ✅ PayloadSanitizerTest.sanitize_shouldMaskToken |
| TC-00-010-10 | Edge | 巢狀欄位脫敏 | 巢狀 JSON 含 password | `{data:{password:"secret"}}` | 深層遮蔽 | 遮蔽 | ✅ PayloadSanitizerTest.sanitize_shouldMaskNestedSensitiveFields |
| TC-00-010-11 | Edge | 長 payload 截斷 | >2000 字元 | 3000 字元 | ≤ 2000 字元 | 長度限制 | ✅ PayloadSanitizerTest.sanitize_shouldTruncateLongPayload |
| TC-00-010-12 | Edge | 空 payload | null 或 {} | null | 回傳 null | null safe | ✅ PayloadSanitizerTest.sanitize_shouldReturnNullForEmptyArgs |

---

## FN-00-011 稽核日誌查詢

**SA**: SA-11 §稽核日誌 | **SD**: SD-11 §2.2 | **API**: `GET /v1/auth/audit/logs`  
**Service**: `AuditService.getUserUsageHistory()` | **SRS**: SRS-01-004 | **Spec**: §2-7

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-011-01 | Happy | 管理員查詢 | ADMIN 角色 | 預設分頁 | 200, paged result | 有 LOGIN 紀錄 | ✅ AuditControllerTest.getUserUsageHistory_shouldReturn200ForAdmin |
| TC-00-011-02 | Happy | 超級管理員查詢 | SUPER_ADMIN | 預設分頁 | 200 | 跨租戶 | ✅ AuditControllerTest.getUserUsageHistory_shouldReturn200ForSuperAdmin |
| TC-00-011-03 | Error | 無權限 | VIEWER 角色 | — | 403 | 被 @PreAuthorize 擋 | ✅ AuditControllerTest.getUserUsageHistory_shouldReturn403ForViewer |
| TC-00-011-04 | Happy | 查個人紀錄 | 任何已認證用戶 | `/audit/user/login/my` | 200, 自己的紀錄 | userId 篩選 | ✅ AuditControllerTest.getMyLoginLog_shouldReturn200ForAnyAuthenticatedUser |
| TC-00-011-05 | Error | 未登入 | 無 token | — | 401 | HTTP 401 | ✅ AuditControllerTest.getMyLoginLog_shouldReturn401WithoutAuth |
| TC-00-011-06 | Happy | userName 篩選 | — | `userName="admin"` | Specification 查詢 | 有篩選 | ✅ AuditServiceTest.getUserUsageHistory_shouldFilterByUserName |
| TC-00-011-07 | Happy | eventDesc 篩選 | — | `eventDesc="USER_AUTH"` | Specification 查詢 | 有篩選 | ✅ AuditServiceTest.getUserUsageHistory_shouldFilterByEventDesc |
| TC-00-011-08 | Happy | 時間範圍篩選 | — | `start=04-01, end=04-30` | Specification 查詢 | 有篩選 | ✅ AuditServiceTest.getMyEventLogs_shouldFilterByTimeRange |

---

## FN-00-012 稽核日誌匯出

**SA**: SA-11 §稽核日誌 | **SD**: SD-11 §2.3 | **API**: `GET /v1/auth/audit/user/usage/history/export`  
**Service**: `AuditService` | **SRS**: SRS-01-004 | **Spec**: §2-7

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-012-01 | Happy | CSV 匯出 | 有日誌資料 | `format=csv` | CSV 串流 | Content-Type: text/csv | ✅ AuditControllerTest.exportCsv_shouldReturn200ForAdmin + AuditServiceTest.exportCsv_* (3) |
| TC-00-012-02 | Happy | XLSX 匯出 | 有日誌資料 | `format=xlsx` | XLSX 串流 | output > 0 bytes | ✅ AuditControllerTest.exportXlsx_shouldReturn200ForAdmin + AuditServiceTest.exportXlsx_* (1) |
| TC-00-012-03 | Negative | 無權限匯出 | VIEWER 角色 | `format=csv` | 403 Forbidden | 權限攔截 | ✅ AuditControllerTest.export_shouldReturn403ForViewer |
| TC-00-012-04 | Negative | 未登入匯出 | 無 Token | `format=csv` | 401 Unauthorized | 驗證攔截 | ✅ AuditControllerTest.export_shouldReturn401WithoutAuth |

> 匯出引擎本身在 FN-00-024~026 有 Service 層測試

---

## FN-00-013 日誌自動清理 (Job)

**SA**: SA-11 §稽核日誌 | **SD**: SD-11 §2.4 | **API**: 排程 Job  
**Service**: `AuditPurgeJob` | **SRS**: SRS-01-004 | **Spec**: §2-7

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-013-01 | Happy | 清理 7 天前紀錄 | 有舊資料 | Job 觸發 | `deleteByCreateTimeBefore` 被呼叫，cutoff ≈ 7 天前 | cutoff 在 6~8 天範圍 | ✅ AuditPurgeJobTest.purgeOldAuditLogs_shouldDeleteRecords |
| TC-00-013-02 | Edge | 無資料清理 | 無舊資料 | Job 觸發 | 0 筆刪除，不拋例外 | 容錯 | ✅ AuditPurgeJobTest.purgeOldAuditLogs_shouldHandleZeroDeleted |

---

## FN-00-014 ~ FN-00-019 通知引擎

**SA**: SA-11 §通知引擎 | **SD**: SD-11 §9 | **狀態**: ✅ 已實作  
**Package**: `com.taipei.iot.notification` | **SRS**: SRS-02-010 | **Spec**: §2-10

---

## FN-00-014 站內通知發送

**SA**: SA-11 §通知引擎 | **SD**: SD-11 §9 | **API**: 內部 Service  
**Service**: `NotificationService.send()` + `InAppChannel` | **SRS**: SRS-02-010 | **Spec**: §2-10

**商業規則**：
- `@Async` 非同步發送，不阻塞業務主流程
- 多管道並行：InApp / Email / SMS / WebSocket，單管道失敗不影響其他
- InApp 管道必寫 DB（`notifications` 表）
- 接收者不存在時 log error 但不拋例外（best-effort）

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-014-01 | Happy | 發送站內通知 | 用戶存在 | NotificationPayload (type=TODO, userId, title, refType, refId) | notification 紀錄建立 | repository.save() 被呼叫；entity 欄位正確 | ✅ NotificationServiceTest.send_shouldDelegateToAllChannels + InAppChannelTest.send_shouldSaveOneNotificationPerUser |
| TC-00-014-02 | Error | 接收者不存在 | userId 查不到 | payload with invalid userId | 錯誤回報(log)，不拋例外 | 不影響業務流程 | ✅ EmailChannelTest.send_shouldSkipWhenUserNotFound |
| TC-00-014-03 | Happy | 多管道並行 | targetChannels=["IN_APP","EMAIL"] | payload | InApp + Email 都被呼叫 | 兩個 channel.send() 被呼叫 | ✅ NotificationServiceTest.send_shouldDelegateToAllChannels |
| TC-00-014-04 | Edge | 單管道失敗不影響其他 | Email channel 拋 exception | payload | InApp 成功，Email 失敗被吞 | InApp entity 存在；無例外拋出 | ✅ NotificationServiceTest.send_shouldContinueWhenOneChannelFails |
| TC-00-014-05 | Happy | InAppChannel 寫入 DB | — | payload | Entity 含 tenantId, userId, type, title, content, refType, refId, read=false | 全欄位驗證 | ✅ InAppChannelTest.send_shouldSaveOneNotificationPerUser + send_shouldSetTenantIdOnEntity |

---

## FN-00-015 通知列表查詢

**SA**: SA-11 §通知引擎 | **SD**: SD-11 §9.4 | **API**: `GET /v1/auth/notifications`  
**Service**: `NotificationService.list()` | **SRS**: SRS-02-010 | **Spec**: §2-10

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-015-01 | Happy | 查詢通知列表 | 用戶有 5 筆通知 | 預設分頁 (page=0, size=20) | 200, 5 筆結果 | totalElements=5 | ✅ NotificationControllerTest.list_shouldReturn200ForAuthenticatedUser + NotificationServiceTest.list_shouldReturnPagedNotifications |
| TC-00-015-02 | Happy | 篩選未讀 | 3 筆未讀 + 2 筆已讀 | `read=false` | 200, 3 筆結果 | 僅含未讀 | ⬜ NotificationControllerTest.list_filterUnread_shouldReturn3 |
| TC-00-015-03 | Happy | 篩選類型 | 2 筆 TODO + 3 筆 INFO | `type=TODO` | 200, 2 筆結果 | 僅含 TODO | ✅ NotificationServiceTest.listTodos_shouldReturnOnlyTodoType + NotificationControllerTest.listTodos_shouldReturn200 |
| TC-00-015-04 | Security | 僅顯示自己的通知 | user-A 有 3 筆, user-B 有 2 筆 | user-A 查詢 | 200, 3 筆 | WHERE user_id = currentUser | ✅ NotificationServiceTest.list_shouldReturnPagedNotifications (query by userId) |
| TC-00-015-05 | Error | 未登入 | 無 token | — | 401 | HTTP 401 | ✅ NotificationControllerTest.list_shouldReturn401WithoutToken |

---

## FN-00-016 標記已讀

**SA**: SA-11 §通知引擎 | **SD**: SD-11 §9.4 | **API**: `PATCH /v1/auth/notifications/{id}/read` + `PATCH .../read-all`  
**Service**: `NotificationService.markRead()` / `markAllRead()` | **SRS**: SRS-02-010 | **Spec**: §2-10

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-016-01 | Happy | 標記單筆已讀 | 通知 id=1 未讀 | PATCH /{id}/read | 200, read=true, readAt 非 null | readAt ≈ now | ✅ NotificationControllerTest.markRead_shouldReturn200 + NotificationServiceTest.markRead_shouldSetReadTrueAndReadAt |
| TC-00-016-02 | Happy | 全部已讀 | 5 筆未讀 | PATCH /read-all | 200, updatedCount=5 | 全部 read=true | ✅ NotificationControllerTest.markAllRead_shouldReturn200 + NotificationServiceTest.markAllRead_shouldCallRepositoryBulkUpdate |
| TC-00-016-03 | Error | 通知不存在 | — | PATCH /999/read | 404, NOTIFICATION_NOT_FOUND | errorCode | ✅ NotificationServiceTest.markRead_shouldThrowWhenNotFound |
| TC-00-016-04 | Security | 標記他人通知 | 通知屬於 user-B | user-A PATCH /1/read | 404 (不可跨使用者) | 回傳 NOTIFICATION_NOT_FOUND | ✅ NotificationServiceTest.markRead_shouldThrowWhenUserMismatch |
| TC-00-016-05 | Edge | 重複標記已讀 | 已讀通知 | PATCH /{id}/read | 200 (冪等) | readAt 不變 | ✅ NotificationServiceTest.markRead_shouldSkipIfAlreadyRead |

---

## FN-00-017 WebSocket 即時推送

**SA**: SA-11 §通知引擎 | **SD**: SD-11 §9.5 | **API**: WebSocket `/ws/notifications`  
**Service**: `SimpMessagingTemplate` | **SRS**: SRS-02-010 | **Spec**: §2-10

**商業規則**：
- STOMP over WebSocket + SockJS fallback
- 認證：CONNECT 階段驗 JWT（StompAuthInterceptor）
- Per-user 頻道：`/user/topic/notifications`

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-017-01 | Happy | WebSocket 連線+推送 | 有效 JWT | STOMP CONNECT + 發送通知 | 用戶端收到 WS 訊息 | 訊息含 id, type, title, refType | ⬜ WebSocketNotificationTest.connect_andReceiveMessage |
| TC-00-017-02 | Edge | 無效 JWT 被拒 | JWT 過期/錯誤 | STOMP CONNECT | 連線失敗 | ERROR frame | ⬜ WebSocketNotificationTest.connect_invalidJwt_shouldReject |
| TC-00-017-03 | Happy | 多用戶隔離 | user-A 和 user-B 都連線 | 發通知給 user-A | 只有 user-A 收到 | user-B 不收到 | ⬜ WebSocketNotificationTest.push_shouldOnlyReachTargetUser |

---

## FN-00-018 Email 通知

**SA**: SA-11 §通知引擎 | **SD**: SD-11 §9.7 | **API**: 內部 Service  
**Service**: `EmailChannel` / `NoOpEmailChannel` | **SRS**: SRS-02-010 | **Spec**: §2-9

**商業規則**：
- `@Profile("!test")` → 真寄（JavaMailSender）
- `@Profile("test")` → NoOp（log only）
- best-effort：寄送失敗不拋例外
- 用戶可透過 `notifyEmailFlag` 關閉 Email 通知

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-018-01 | Happy | Email 寄送成功 | Mock JavaMailSender | payload(userEmail, title, content) | mailSender.send() 被呼叫 | SimpleMailMessage 欄位正確(to, subject, body) | ✅ EmailChannelTest.send_shouldSendEmailWhenUserHasEmailFlagTrue |
| TC-00-018-02 | Error | SMTP 失敗 | mailSender.send() 拋 RuntimeException | payload | 錯誤被 log，不拋例外 | 不影響業務 | ✅ EmailChannelTest.send_shouldNotThrowWhenMailSenderFails |
| TC-00-018-03 | Edge | NoOp 模式 | @Profile("test") | payload | 不真寄，不拋例外 | 無 SMTP 連線 | ✅ NoOpEmailChannelTest.send_shouldNotThrow |
| TC-00-018-04 | Edge | 用戶關閉 Email | notifyEmailFlag=false | payload | Email channel 跳過寄送 | mailSender.send() 未呼叫 | ✅ EmailChannelTest.send_shouldSkipWhenEmailFlagFalse |

---

## FN-00-019 SMS 通知

**SA**: SA-11 §通知引擎 | **SD**: SD-11 §9.8 | **API**: 內部 Service  
**Service**: `SmsChannel` / `NoOpSmsChannel` | **SRS**: SRS-02-010 | **Spec**: §2-9

**商業規則**：
- `@Profile("prod")` → 真閘道（待確定供應商）
- `@Profile("!prod")` → NoOp（log only）
- best-effort：閘道失敗不拋例外
- 用戶可透過 `notifySmsFlag` 關閉 SMS 通知

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-019-01 | Happy | NoOp SMS 發送 | @Profile("!prod") | payload(userPhone, title, content) | 不拋例外 | NoOp 行為 | ✅ NoOpSmsChannelTest.send_shouldNotThrow |
| TC-00-019-02 | Error | 閘道失敗 | Mock HTTP client 拋 exception | payload | 錯誤被 log，不拋例外 | 不影響業務 | ⬜ SmsChannelTest.send_gatewayFail_shouldLogAndNotThrow (待 prod SMS 閘道實作) |
| TC-00-019-03 | Edge | 用戶關閉 SMS | notifySmsFlag=false | payload | SMS channel 不被呼叫 | channel.send() 未執行 | ⬜ NotificationServiceTest.send_smsDisabled_shouldSkipSms (待 prod SMS 閘道實作) |

---

## FN-00-020 檔案上傳

**SA**: SA-11 §檔案管理 | **SD**: SD-11 §4.1 | **API**: `POST /v1/auth/files/upload`  
**Service**: `FileStorageService` | **SRS**: SRS-NFR-002 | **Spec**: §5-3

**商業規則**：副檔名白名單, 大小限制, 病毒掃描 (ClamAV)

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-020-01 | Happy | 正常上傳 | — | multipart/form-data (圖片) | 200, fileId + metadata | fileId 非 null | ⬜ 待補 |
| TC-00-020-02 | Error | 副檔名不允許 | 白名單: jpg,png,pdf | .exe 檔案 | 400, 拒絕上傳 | 安全驗證 | ⬜ 待補 |
| TC-00-020-03 | Error | 超過大小限制 | limit=10MB | 20MB 檔案 | 413 或 400 | 大小檢查 | ⬜ 待補 |
| TC-00-020-04 | Edge | 空檔案 | — | 0 byte 檔案 | 400, 拒絕 | 驗證 | ⬜ 待補 |

---

## FN-00-021 檔案下載

**SA**: SA-11 §檔案管理 | **SD**: SD-11 §4.2 | **API**: `GET /v1/auth/files/{id}/download`  
**Service**: `FileStorageService` | **SRS**: SRS-NFR-002 | **Spec**: §5-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-021-01 | Happy | 正常下載 | 檔案存在 | fileId | 200, 檔案串流 | Content-Disposition | ⬜ 待補 |
| TC-00-021-02 | Error | 檔案不存在 | — | 不存在的 ID | 404 | HTTP 404 | ⬜ 待補 |
| TC-00-021-03 | Error | 無權限 | 非檔案擁有者 | fileId | 403 | 權限檢查 | ⬜ 待補 |

---

## FN-00-022 ~ FN-00-023 檔案預覽/刪除

**狀態**: 🔲 未實作

| TC ID | FN | 類型 | 場景 | 預期結果 | 自動化 |
|-------|-----|------|------|---------|--------|
| TC-00-022-01 | FN-00-022 | Happy | 圖片預覽 | 預覽 URL | ⬜ 待實作 |
| TC-00-022-02 | FN-00-022 | Happy | PDF 預覽 | 預覽 URL | ⬜ 待實作 |
| TC-00-023-01 | FN-00-023 | Happy | 軟刪除 | `deleted=true`, 檔案不可再下載 | ⬜ 待實作 |
| TC-00-023-02 | FN-00-023 | Error | 已刪除再刪 | 404 或冪等成功 | ⬜ 待實作 |

---

## FN-00-024 CSV 匯出

**SA**: SA-11 §匯出引擎 | **SD**: SD-11 §5 | **API**: 共用 Service  
**Service**: `DeviceExportService.exportCsv()` | **SRS**: SRS-NFR-003 | **Spec**: §1-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-024-01 | Happy | 正常匯出 | 2 筆資料 | Device list | CSV 含 header + 2 data rows | 欄位: 設備類型, 設備編號 | ✅ DeviceExportServiceTest.exportCsv_containsHeadersAndData |
| TC-00-024-02 | Edge | 空資料 | 0 筆 | 空 list | 只有 header 行 | 1 行 | ✅ DeviceExportServiceTest.exportCsv_emptyList_headerOnly |
| TC-00-024-03 | Edge | 逗號/引號跳脫 | 資料含 `,` 和 `"` | `路燈,含逗號"引號` | 正確 CSV 跳脫 | 符合 RFC 4180 | ✅ DeviceExportServiceTest.exportCsv_escapesCommasAndQuotes |
| TC-00-024-04 | Edge | JSONB 欄位排序 | attributes 含 zzz, aaa, mmm | 動態欄位 | columns 按字母排序 | aaa < mmm < zzz | ✅ DeviceExportServiceTest.exportCsv_jsonbAttributeKeysAreSorted |

---

## FN-00-025 XLSX 匯出

**SA**: SA-11 §匯出引擎 | **SD**: SD-11 §5 | **API**: 共用 Service  
**Service**: `DeviceExportService.exportXlsx()` | **SRS**: SRS-NFR-003 | **Spec**: §1-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-025-01 | Happy | 正常匯出 | 2 筆資料 | Device list | XLSX 串流 > 0 bytes | 不拋例外 | ✅ DeviceExportServiceTest.exportXlsx_doesNotThrow |

---

## FN-00-026 ODS 匯出

**SA**: SA-11 §匯出引擎 | **SD**: SD-11 §5 | **API**: 共用 Service  
**Service**: `DeviceExportService.exportOds()` | **SRS**: SRS-NFR-003 | **Spec**: §1-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-026-01 | Happy | 正常匯出 | 2 筆資料 | Device list | ODS 串流 > 0 bytes | 不拋例外 | ✅ DeviceExportServiceTest.exportOds_doesNotThrow |

---

## FN-00-027 Excel 匯入引擎

**SA**: SA-11 §匯出引擎 | **SD**: SD-11 §5 | **API**: 共用 Service  
**Service**: `ApprovedMaterialService.importFromExcel()` | **SRS**: SRS-NFR-003 | **Spec**: §1-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-027-01 | Happy | 正常匯入 | 合法 Excel | 含 header + data 的 .xlsx | 匯入成功，回傳計數 | 筆數正確 | ⬜ 待補 |
| TC-00-027-02 | Error | 格式錯誤 | 欄位不符 | 缺必填欄位的 .xlsx | 400, 錯誤明細 | 驗證訊息 | ⬜ 待補 |
| TC-00-027-03 | Edge | 空檔案 | — | 空 .xlsx | 400, "無資料" | 錯誤提示 | ⬜ 待補 |
| TC-00-027-04 | Edge | 大量資料 | — | 10000 行 | 批次處理完成 | 效能 | ⬜ 待補 |

---

## FN-00-028 事件發佈

**SA**: SA-11 §事件匯流排 | **SD**: SD-11 §6 | **API**: 內部  
**Service**: `ApplicationEventPublisher` | **Spec**: 跨模組

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-028-01 | Happy | 發佈事件 | 有訂閱者 | publish(event) | 訂閱者被呼叫 | listener invoked | ✅ (各 Listener Test 間接驗證) |

---

## FN-00-029 事件訂閱處理

**SA**: SA-11 §事件匯流排 | **SD**: SD-11 §6 | **API**: `@EventListener`

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-029-01 | Happy | 訂閱處理 | @EventListener | 事件觸發 | 處理邏輯執行 | — | ✅ FaultApprovedListenerTest, RepairClosedListenerTest 等 |

---

## FN-00-030 事件紀錄

**SA**: SA-11 §事件匯流排 | **SD**: SD-11 §6 | **API**: 內部

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-00-030-01 | Happy | 事件寫入 audit | 事件觸發 | AuditAsyncWriter | audit_log 紀錄 | 持久化 | ✅ AuditAsyncWriterTest.saveAsync_shouldPersistEntity |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ✅ 已有自動化 | 113 |
| ⬜ 待補（已實作 FN） | 7 |
| ⬜ 待實作（未實作 FN：通知 WS/SMS） | 5 |
| ⬜ 待實作（未實作 FN：檔案/匯入/其他） | 4 |
| **總 TC 數** | **129** |

### 待補測試優先序

| 優先 | FN | 待建 TC | 原因 |
|------|-----|--------|------|
| 1 | FN-00-020~021 | TC-00-020/021 (7 TC) | 檔案上傳下載已實作，完全無測試 |
| 2 | FN-00-027 | TC-00-027 (4 TC) | Excel 匯入已實作，無測試 |
| 3 | FN-00-017 | TC-00-017 (3 TC) | WebSocket 整合測試，需 @SpringBootTest |
| 4 | FN-00-015 | TC-00-015-02 (1 TC) | 通知未讀篩選，待加 read filter param |
| 5 | FN-00-019 | TC-00-019-02/03 (2 TC) | SMS 閘道待 prod 實作後補 |
| ~~6~~ | ~~FN-00-002~~ | ~~TC-00-002-04~~ | ✅ 已補 — AuthControllerTest.authApi_malformedToken_returns401 |
| ~~7~~ | ~~FN-00-012~~ | ~~TC-00-012 (4 TC)~~ | ✅ 已補 — AuditControllerTest + AuditServiceTest export tests |
| ~~8~~ | ~~FN-00-005~~ | ~~TC-00-005-02~~ | ✅ 已補 — 改為 rate limit 測試 (RateLimitInterceptorTest) |
| ~~9~~ | ~~FN-00-014~019~~ | ~~TC-00-014~019 (20 TC)~~ | ✅ 已補 — Commit `c290a3b` (32 tests, 6 classes) |
