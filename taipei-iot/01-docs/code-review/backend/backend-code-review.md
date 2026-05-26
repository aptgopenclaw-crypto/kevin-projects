# Backend 整體 Code Review

> 審查日期：2026-05-20
> 審查範圍：`backend/` 整體架構、安全基礎設施、跨模組共用元件、配置與依賴管理
> 測試狀態：366 tests all passing

---

## 一、架構總覽

```
┌─────────────────────────────────────────────────────────────────────┐
│                         HTTP Request                                 │
├─────────────────────────────────────────────────────────────────────┤
│  SecurityConfig (CORS / Security Headers / Filter Chain)            │
│    ↓                                                                │
│  JwtAuthenticationFilter (Bearer → SecurityContext + TenantContext) │
│    ↓                                                                │
│  RateLimitInterceptor (Redis Lua INCR + EXPIRE)                    │
│    ↓                                                                │
│  TenantInterceptor (single/multi mode)                             │
│    ↓                                                                │
│  Controller (@PreAuthorize + @Validated + @AuditEvent)             │
│    ↓                                                                │
│  Service (Business Logic)                                          │
│    ↓                                                                │
│  Repository (TenantFilterAspect → Hibernate @Filter)               │
│    ↓                                                                │
│  DataPermissionAspect (row-level filtering by dept hierarchy)      │
├─────────────────────────────────────────────────────────────────────┤
│  GlobalExceptionHandler → BaseResponse<T>                          │
│  BaseLoggerAspect → AuditAsyncWriter (非同步寫入稽核 DB)            │
│  TenantContext.clear() (finally block — prevent ThreadLocal leak)  │
└─────────────────────────────────────────────────────────────────────┘
```

### 技術棧

| 類別 | 技術 |
|------|------|
| 框架 | Spring Boot 3.4.1, Java 21 |
| 安全 | Spring Security 6, JWT (JJWT 0.12.6), BCrypt |
| 資料庫 | PostgreSQL + PostGIS, Hibernate Spatial, Flyway Migration |
| 快取/限流 | Redis (Lua atomic script) |
| 即時通訊 | WebSocket STOMP + SockJS |
| IoT | Spring Integration MQTT |
| 導出 | Apache POI (Excel), PDFBox, Commons CSV, ODF Toolkit |
| 檔案驗證 | Apache Tika (magic bytes) |
| 映射 | MapStruct 1.6.3 + Lombok |
| 測試 | JUnit 5, Mockito, Spring Security Test |

### 模組清單

| 模組 | 說明 | 租戶隔離 |
|------|------|----------|
| `common` | 共用 DTO / Exception / Response / Util / Interceptor | — |
| `config` | SecurityConfig / WebMvcConfig / RedisConfig | — |
| `auth` | 登入 / JWT / 密碼重設 / Captcha | ✅ |
| `tenant` | TenantContext / TenantFilter / TenantInterceptor | 核心 |
| `user` | 使用者 CRUD / 密碼政策 / 帳號鎖定 | ✅ |
| `rbac` | 角色 / 權限 / 選單管理 | ✅ |
| `dept` | 部門管理 / 資料權限 (DataScope) | ✅ |
| `audit` | 稽核日誌 / AOP 攔截 / 非同步寫入 | ✅ |
| `announcement` | 公告管理 / 排程發送 | ✅ |
| `notification` | WebSocket 即時推播 / Email 通知 | ✅ |
| `setting` | 系統設定 (idle timeout 等) | ✅ |

---

## 二、安全性審查 (Security Review)

### ✅ 優秀的安全實踐

| 項目 | 實作 | 評價 |
|------|------|------|
| **認證架構** | Stateless JWT + Bearer token | 正確，無 session fixation 風險 |
| **密碼儲存** | BCrypt (cost factor=10) | 業界標準 |
| **JWT 密鑰驗證** | 啟動時檢查 ≥32 bytes (256-bit) | 防弱密鑰 |
| **Security Headers** | HSTS (1y) + CSP + X-Frame-Options + Permissions-Policy + Referrer-Policy | 全面，附中文註解說明原因 |
| **CORS** | 白名單 origin + credentials + explicit methods | 正確限縮 |
| **速率限制** | Redis Lua 原子操作 + `request.getRemoteAddr()` | 不信任 X-Forwarded-For，正確 |
| **租戶隔離** | AOP + Hibernate @Filter + fail-closed (null = reject) | 嚴格 |
| **權限模型** | URL-level (SecurityConfig) + Method-level (@PreAuthorize) 雙層 | 深度防禦 |
| **異常處理** | GlobalExceptionHandler 統一格式，不洩漏 stacktrace | 安全 |
| **SQL Injection 偵測** | GlobalExceptionHandler 內建模式比對 + SecurityLogger | 偵測+告警 |
| **ThreadLocal 清理** | JWT Filter finally block + Interceptor afterCompletion | 防跨請求污染 |
| **Token 類型區分** | Access / Refresh / Temporary 三種 token，各有獨立 TTL | 正確 |
| **場域停用即時拒絕** | `TenantEnabledCache` 在 JWT Filter 即時檢查 | 不等 token 過期 |
| **稽核日誌** | 非同步寫入 + 專用 logback appender (7 天 rolling) | 效能與合規兼顧 |
| **安全日誌** | 獨立 security.log (30 天 rolling) + 結構化事件分類 | 便於 SIEM 接入 |
| **檔案上傳** | Apache Tika magic bytes + 白名單副檔名 + size 限制 | 多重驗證 |
| **WebSocket 認證** | STOMP CONNECT 階段驗證 JWT | 正確，不依賴 HTTP session |
| **Swagger 生產環境** | `@ConditionalOnProperty` 控制，預設 disabled | 不會暴露 |
| **密碼政策** | 長度 + 大寫 + 小寫 + 數字 + 歷史 5 筆 + 90 天過期 | 合規 |
| **帳號鎖定** | 5 次失敗 → 10 分鐘鎖定 | 防暴力破解 |

### ⚠️ 需要注意的安全項目

#### 1. [中等] BaseLoggerAspect 中 `getClientIp()` 讀取 X-Forwarded-For

```java
private String getClientIp() {
    HttpServletRequest req = getRequest();
    if (req == null) return null;
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
        return xff.split(",")[0].trim();
    }
    return req.getRemoteAddr();
}
```

**問題**：RateLimitInterceptor 正確使用 `request.getRemoteAddr()`（有詳細安全註解），但 BaseLoggerAspect 的 `getClientIp()` 仍讀取 X-Forwarded-For。在無反向代理的部署環境中，攻擊者可偽造 IP 寫入稽核日誌，造成誤判和追查困難。

**建議**：統一使用 `request.getRemoteAddr()`，與 RateLimitInterceptor 保持一致。

#### 2. [低] CSRF disabled — 需搭配其他防護

```java
.csrf(csrf -> csrf.disable())
```

**評估**：對純 REST API + JWT Bearer token 是正確做法（token 不會自動附加）。但 WebSocket `/ws/**` 配合 SockJS fallback 使用 HTTP transport 時，需確認 STOMP 層認證已充分保護（已實作 StompAuthInterceptor → ✅）。

#### 3. [低] `application-dev.yml` 中寫死的 JWT Secret

```yaml
jwt:
  secret: ${JWT_SECRET:x7G2kLp9QzRtW4vN8mYfA3hJ6bD0sCeU1oXiPaZqTnMwKyBrVdEj5lFgHuI}
```

**評估**：這是 dev profile 的 fallback，生產環境應透過環境變數覆蓋。JwtUtil 已驗證密鑰長度 ≥32 bytes。確保 `.env` 或部署配置中設定強隨機密鑰即可。

#### 4. [低] WebSocket STOMP 認證不含權限資訊

```java
UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userId, tenantId, List.of());
```

**評估**：STOMP 連線層無權限 authority → 無法在 WebSocket handler 中使用 `@PreAuthorize`。但目前 WebSocket 僅用於推播通知（server → client），使用者不會透過 WebSocket 執行敏感操作，因此風險低。

---

## 三、程式碼品質審查 (Code Quality)

### ✅ 架構優點

#### 1. 防禦性多租戶設計

```java
// TenantFilterAspect — Fail-closed
if (tenantId == null) {
    throw new IllegalStateException(
            "TenantContext is not set. All repository operations require a tenant context.");
}
```

- `TenantScopedRepository` marker interface 明確區分需要租戶過濾的 repository
- 全域實體（User, Tenant）的 repository 不繼承 marker → 不受 AOP 影響
- System context 有明確的 `runInSystemContext()` API → 不容易誤用

#### 2. 統一回應格式

```java
BaseResponse<T> {
    errorCode, errorMsg, errorDetail, timestamp, body
}
```

- `@JsonInclude(NON_NULL)` 避免冗餘欄位
- `@ToString(exclude = "body")` 防止 log 洩漏敏感資料
- `isSuccess()` 方法方便程式碼判斷

#### 3. 結構化安全事件記錄

```
logs/
├── audit.log      (7 天 rolling, 500MB cap)
├── security.log   (30 天 rolling, 1GB cap)
└── application.log (7 天 rolling, 500MB cap)
```

- 三種日誌分離 → 便於 SIEM/SOC 接入
- SecurityEvent enum + SecurityLogger 確保格式一致
- 稽核日誌不依賴 application log level → 不會被意外關閉

#### 4. 原子性速率限制

```lua
local count = redis.call('INCR', KEYS[1])
if count == 1 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
end
return count
```

- Lua script 確保 INCR + EXPIRE 原子性
- Redis 不可用時 graceful degradation（放行）
- 429 回應含 `Retry-After` header
- Key 格式 `rate_limit:{key}:{ip}` 可針對不同端點設定不同限制

#### 5. 異常處理全覆蓋

```
BusinessException → errorCode mapping (4xx / 5xx)
MethodArgumentNotValidException → field + global errors
ConstraintViolationException → @Validated violations
MissingServletRequestParameterException → missing param
HttpMessageNotReadableException → JSON parse error
ResponseStatusException → preserve original status
AccessDeniedException → 403 + security log
Exception → 500 + SQL injection detection
```

- 每種異常對應適當的 HTTP status code
- 不洩漏 stacktrace 到客戶端
- 疑似 SQL injection 觸發 SecurityLogger

#### 6. 資料權限抽象

```java
@DataPermission(deptIdField = "d.deptId", hierarchyPathField = "d.hierarchyPath")
public List<UserDto> listUsers(...) { }
```

- AOP 自動注入 DataScopeContext
- Service 層不需要感知權限邏輯
- Fail-closed: user info / deptId null → impossible condition (deptId = -1)

#### 7. DB Migration 管理

- Flyway 管理所有 schema 變更
- `out-of-order: true` 允許分支並行開發
- 每個模組用 VXX__ prefix 區隔
- Migration 含 seed data（initial roles, permissions, menus）

### ⚠️ 需要改進的項目

#### 8. [中等] BaseLoggerAspect `getClientIp()` 與 RateLimitInterceptor 不一致

同安全審查 #1。兩個元件的 IP 取得邏輯應統一。

**建議**：抽出共用 `IpUtils.getClientIp(HttpServletRequest)` 方法，統一邏輯。

#### 9. [低] `HikariCP maximum-pool-size: 10` 可能不足

```yaml
hikari:
  maximum-pool-size: 10
```

**評估**：對開發環境足夠，但生產環境若有多個 async worker + WebSocket + MQTT，10 個連線可能不足。

**建議**：使用環境變數 `${HIKARI_MAX_POOL:10}` 讓生產環境可調。

#### 10. [低] Redis 無 connection pool 顯式配置

```yaml
data:
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD}
```

**評估**：使用 Lettuce 預設 pool 配置，通常足夠。生產環境可考慮設定 `lettuce.pool.max-active`。

#### 11. [低] `application-dev.yml` 中 DB 密碼 fallback

```yaml
password: ${DB_PASSWORD:Kali1234!}
```

**評估**：這是 dev profile fallback，不影響生產。但若 `.env` 遺失，IDE 自動啟動時可能連到錯誤 DB。確保 `.env.example` 文件提供模板即可。

---

## 四、跨模組一致性審查

### ✅ 一致的模式

| 模式 | 各模組一致性 |
|------|--------------|
| Controller `@Validated` | ✅ 所有模組已加 |
| `@PreAuthorize` 權限控制 | ✅ 寫入操作全部有 |
| `@AuditEvent` 稽核 | ✅ 全部 CRUD 操作 (含 setting 已修正) |
| Repository extends `TenantScopedRepository` | ✅ 所有租戶隔離實體 |
| Entity `@Filter(tenantFilter)` + `TenantAware` | ✅ 一致 |
| `@EntityListeners({TenantEntityListener, AuditingEntityListener})` | ✅ |
| BusinessException + ErrorCode 統一錯誤碼 | ✅ 全模組使用 |
| BaseResponse 統一回應 | ✅ |
| `@JsonInclude(NON_NULL)` 在 DTO | ✅ 大部分已加 |
| Input validation (`@NotBlank`, `@Size`, `@Min`, `@Max`) | ✅ |

### ⚠️ 可提升的一致性

| 項目 | 現況 | 建議 |
|------|------|------|
| API 版本前綴 | `/v1/auth/` + `/v1/noauth/` + `/v1/admin/` | ✅ 一致且語意清楚 |
| Paginated 回應 | 部分用 `PageResponse<T>`, 部分用 `Page<T>` 直接返回 | 建議統一為 `PageResponse` 包裝 |
| Date/Time 格式 | Entity 用 `LocalDateTime`，部分 API 回傳 ISO-8601 | ✅ Jackson 預設處理正確 |

---

## 五、效能審查

### ✅ 效能優點

| 項目 | 實作 |
|------|------|
| 稽核日誌非同步寫入 | `AuditAsyncWriter` 不阻塞主請求 |
| Redis Lua 原子操作 | 速率限制單次 RTT |
| Hibernate @Filter | DB 層過濾，不在 Java 層過濾全量資料 |
| WebSocket STOMP | 即時推播不需 polling |
| Flyway baseline-on-migrate | 開發環境快速遷移 |
| `@Transactional(readOnly = true)` | 讀取操作正確標記 |

### ⚠️ 潛在效能瓶頸

| 項目 | 說明 | 建議 |
|------|------|------|
| TenantFilterAspect 對**每一次** Repository 方法調用都觸發 | `execution(* com.taipei.iot..*.repository..*Repository.*(..))` 匹配非常廣泛 | 已用 `instanceof TenantScopedRepository` 短路 → 影響極小 |
| JWT 解析每次請求 | 無 token 快取 | 30 分鐘過期 → acceptable |
| `TenantEnabledCache` 每次請求檢查 | 需確認是 Redis cache 而非 DB 查詢 | 確認實作為 Redis/memory cache |

---

## 六、可維護性審查

### ✅ 優點

| 項目 | 說明 |
|------|------|
| **模組化清晰** | 每個業務模組獨立 package（controller / service / repository / entity / dto） |
| **ErrorCode 集中管理** | 按模組分段（10xxx auth, 20xxx user, 30xxx rbac...） |
| **AuditEventType enum** | 所有稽核事件集中定義，編譯期檢查 |
| **環境變數驅動** | 敏感配置全部用 `${VAR}` 佔位符 |
| **MapStruct 映射** | Entity ↔ DTO 轉換自動化 |
| **Lombok 減少模板** | `@RequiredArgsConstructor` 代替 @Autowired |
| **分層日誌** | audit / security / application 三份獨立 |
| **密碼策略可配置** | yml 中可調整長度/複雜度/過期天數 |

### ⚠️ 改進建議

| 項目 | 說明 | 建議 |
|------|------|------|
| 無 API 文件生成 | Swagger 生產環境關閉，開發環境也關閉 | 建議開發環境啟用 `springdoc.api-docs.enabled=true` |
| `common/util` 範圍寬泛 | `SecurityContextUtils` / `SecurityLogger` / `JwtClaimKeys` 都在 util | 可按職責分 sub-package |
| 缺少健康檢查端點 | 無 `/actuator/health` 配置 | 生產部署建議加入 actuator |

---

## 七、測試審查

| 指標 | 現況 |
|------|------|
| 總測試數 | 366 |
| 測試結果 | 全通過 (0 failures) |
| 測試類型 | `@WebMvcTest` (Controller) + `@ExtendWith(MockitoExtension)` (Service) |
| SecurityConfig 整合 | Controller 測試 `@Import({SecurityConfig.class, GlobalExceptionHandler.class})` |
| 認證/授權測試 | 每個 Controller 測 200 / 401 / 403 |
| Redis 測試 | RateLimitInterceptor 整合測試 |
| Flyway | test profile 獨立 `application-test.yml` |

### 測試品質評估

**優點**：
- Controller 測試覆蓋認證 (401)、授權 (403)、正常路徑 (200)
- Service 測試覆蓋 happy path + edge cases (not found / validation)
- 使用 `@WithMockUser` + custom security annotation 模擬不同權限

**建議**：
- 缺少 Integration Test（完整 HTTP → DB 流程）— 可考慮用 Testcontainers
- 無 WebSocket 測試 — 建議至少測試 StompAuthInterceptor 拒絕無效 token

---

## 八、部署與 DevOps 相關

| 項目 | 現況 | 建議 |
|------|------|------|
| 環境變數管理 | spring-dotenv + `${VAR}` | ✅ 正確 |
| 日誌 Rolling | 7/30 天 + size cap | ✅ |
| Graceful Shutdown | 未顯式配置 | 建議加 `server.shutdown=graceful` |
| Health Check | 無 actuator | 建議加 `spring-boot-starter-actuator` |
| DB Connection Pool | 固定 10 | 建議可配置化 |
| Redis 斷線 | RateLimitInterceptor 已處理 (放行) | ✅ |
| File Upload Path | `./uploads` (相對路徑) | 生產建議用絕對路徑 |

---

## 九、總體評分

| 維度 | 評分 | 說明 |
|------|------|------|
| **安全性** | **9/10** | 安全 headers 全覆蓋、JWT 驗證嚴格、租戶隔離 fail-closed、速率限制原子操作。扣分：Audit Aspect IP 不一致。 |
| **架構設計** | **9/10** | 分層清晰、AOP 解耦業務與橫切、多租戶設計完整。 |
| **程式碼品質** | **9/10** | 命名規範、註解充分（含安全原因）、無 magic number。 |
| **效能** | **8.5/10** | 非同步稽核、Redis 限流、WebSocket 推播。連線池可更彈性。 |
| **可維護性** | **9/10** | 模組化好、ErrorCode/AuditEvent 集中管理、環境變數驅動。 |
| **測試覆蓋** | **8.5/10** | 366 tests 覆蓋 Controller + Service。缺少 Integration Test。 |
| **整體** | **8.8/10** | 企業級品質，安全意識高，架構成熟。 |

---

## 十、建議優先級摘要

| 優先級 | 項目 | 類型 |
|--------|------|------|
| **P2** | `BaseLoggerAspect.getClientIp()` 統一改用 `request.getRemoteAddr()` | Security | ✅ 已修正 |
| **P4** | HikariCP pool size 改用環境變數 `${HIKARI_MAX_POOL:10}` | Ops |
| **P4** | 加入 `spring-boot-starter-actuator` + health check | Ops |
| **P4** | 加入 `server.shutdown=graceful` | Reliability |
| **P5** | 開發環境啟用 Swagger API 文件 | DX |
| **P5** | 評估 Testcontainers 整合測試 | Quality |
| ✅ **P5** | 統一 Paginated 回應為 `PageResponse` 包裝 | Consistency |
