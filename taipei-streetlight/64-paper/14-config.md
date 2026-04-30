# 14. Config 組態模組

## 1. 模組概述

`com.taipei.iot.config` 提供 Spring Boot 應用程式的全域組態設定，涵蓋安全、序列化、資料庫審計、快取、API 文件及 Web MVC 等橫切面配置。此模組不包含業務邏輯，純粹是基礎設施配置。

包含以下配置類別：
- **SecurityConfig**：Spring Security 過濾鏈、JWT 整合、安全 Headers
- **JpaAuditConfig**：JPA 審計（`@CreatedBy` / `@LastModifiedBy`）
- **RedisConfig**：Redis 序列化設定
- **JacksonConfig**：JSON 序列化格式
- **OpenApiConfig**：Swagger / OpenAPI 文件
- **WebMvcConfig**：CORS、攔截器註冊

## 2. 資料表結構

本模組無資料表。

## 3. 元件關聯/架構

```
SecurityConfig
  ├── JwtAuthenticationFilter (來自 auth 模組)
  ├── SecurityLogger + SecurityEvent (來自 common)
  ├── BaseResponse + ErrorCode (來自 common)
  └── BCryptPasswordEncoder

WebMvcConfig
  ├── RateLimitInterceptor (來自 common)
  ├── TenantInterceptor (來自 tenant)
  └── CORS 設定 (cors.allowed-origins)

JpaAuditConfig
  └── SecurityContextHolder → AuditorAware<String>

RedisConfig
  └── Jackson2JsonRedisSerializer (共用 ObjectMapper)

JacksonConfig
  └── JavaTimeModule + LocalDateTime 格式化

OpenApiConfig
  └── Bearer JWT 認證 scheme
```

## 4. API 端點

本模組不定義 API，但 `SecurityConfig` 定義所有 API 的存取控制規則：

### 公開端點（permitAll）
| 路徑 | 說明 |
|------|------|
| `/v1/noauth/**` | 未認證 API（登入、註冊等） |
| `/v3/api-docs/**`, `/swagger-ui/**` | API 文件 |
| `/ws/**` | WebSocket handshake |

### 認證端點（authenticated）
| 路徑 | 說明 |
|------|------|
| `/v1/auth/user/my` | 取得自己的使用者資訊 |
| `/v1/auth/user/change-password` | 修改密碼 |
| `GET /v1/auth/menus/my` | 使用者可見選單 |
| `GET /v1/auth/dept/options` | 部門下拉選項 |
| `GET /v1/auth/dept/scope-options` | 範圍限縮部門選項 |
| `GET /v1/log/**` | 日誌摘要 |

### 角色限制端點
| 路徑 | 所需角色/權限 |
|------|-------------|
| `/v1/auth/users/**` | ADMIN / SUPER_ADMIN / DEPT_ADMIN / USER_LIST |
| `GET /v1/auth/roles/**` | ADMIN / SUPER_ADMIN / DEPT_ADMIN |
| `POST/PUT/DELETE /v1/auth/menus/**` | SUPER_ADMIN |
| `/v1/auth/audit/**` | ADMIN / SUPER_ADMIN / AUDIT_LIST |
| `GET /v1/auth/dept/list` | ADMIN / SUPER_ADMIN / DEPT_ADMIN / DEPT_LIST |

## 5. 業務邏輯/機制說明

### 5.1 SecurityConfig 安全配置
- **無狀態 Session**：`SessionCreationPolicy.STATELESS`（JWT 架構）
- **CSRF 停用**：REST API 不需要 CSRF token
- **JWT Filter**：在 `UsernamePasswordAuthenticationFilter` 之前加入
- **安全 Headers**：
  - **HSTS**：1 年，含子域名（防 HTTPS 降級）
  - **CSP**：`default-src 'self'`（防 XSS 注入）
  - **Referrer-Policy**：`strict-origin-when-cross-origin`（防 URL 資訊洩漏）
  - **Permissions-Policy**：禁用相機、麥克風、定位、支付（防惡意 API 呼叫）
  - **X-Frame-Options**：DENY（防 Clickjacking）
  - **X-Content-Type-Options**：nosniff（防 MIME Sniffing）

### 5.2 異常處理
- **401 AuthenticationEntryPoint**：回傳 `BaseResponse.fail(ACCESS_TOKEN_INVALID)`
- **403 AccessDeniedHandler**：記錄 `SecurityEvent.ACCESS_DENIED` + 回傳 `BaseResponse.fail(PERMISSION_DENIED)`

### 5.3 WebMvcConfig 攔截器順序
1. **RateLimitInterceptor**（速率限制）→ 必須先執行
2. **TenantInterceptor**（租戶上下文）
3. 攔截範圍：`/v1/**`（Swagger、actuator 不攔截）

### 5.4 JacksonConfig
- 日期格式：`yyyy-MM-dd HH:mm:ss`（非 timestamp）
- 排除 null 欄位：`JsonInclude.Include.NON_NULL`
- JavaTimeModule 支援 `LocalDateTime`

### 5.5 RedisConfig
- 條件啟用：需設定 `spring.data.redis.host`
- 使用共用 ObjectMapper（不含 `@class` 欄位），避免 class path 變更導致反序列化失敗
- Key 序列化：`StringRedisSerializer`
- Value 序列化：`Jackson2JsonRedisSerializer`

### 5.6 JpaAuditConfig
- `@EnableJpaAuditing` 啟用審計
- `AuditorAware<String>` 從 `SecurityContextHolder` 取得當前使用者名稱
- 未認證或 anonymous → `Optional.empty()`（保留既有值）

### 5.7 OpenApiConfig
- 條件啟用：`springdoc.api-docs.enabled=true`
- Bearer JWT 認證 scheme
- 標題：`taipei-iot-001 API`

## 6. 資料流

### HTTP 請求處理流程
```
HTTP Request
  → Spring Security FilterChain
    → CORS (委派 WebMvcConfig)
    → Security Headers 注入
    → JwtAuthenticationFilter (JWT 驗證)
    → authorizeHttpRequests (路徑權限匹配)
  → MVC Interceptors
    → RateLimitInterceptor (@RateLimit 檢查)
    → TenantInterceptor (設定 TenantContext)
  → Controller
  → Jackson 序列化回應 (yyyy-MM-dd HH:mm:ss, NON_NULL)
```

### CORS 流程
```
Preflight OPTIONS
  → Spring Security .cors(withDefaults()) 放行
  → WebMvcConfig.addCorsMappings() 處理
    → allowedOrigins: ${cors.allowed-origins}
    → allowCredentials: true
    → maxAge: 3600s
```

## 7. ErrorCode / Enum 定義

本模組不定義新的 ErrorCode 或 Enum，但使用以下共用定義：
- `ErrorCode.ACCESS_TOKEN_INVALID` → 401 回應
- `ErrorCode.PERMISSION_DENIED` → 403 回應
- `SecurityEvent.ACCESS_DENIED` → 安全日誌
