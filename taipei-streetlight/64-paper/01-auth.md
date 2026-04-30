# Auth 模組技術文件

> 套件路徑：`com.taipei.iot.auth`
> 系統：台北市路燈管理系統（IoT Streetlight Management）

---

## 1. 模組概述

Auth 模組負責系統的身份驗證與授權核心功能，包含：

- **使用者登入**：支援多租戶（Multi-Tenant）登入流程，含單租戶直接登入與多租戶選擇
- **驗證碼機制**：同時支援傳統圖片驗證碼（Redis 儲存）與 Cloudflare Turnstile 無感驗證
- **JWT Token 管理**：Access Token / Refresh Token / Temporary Token 的簽發與刷新
- **密碼管理**：變更密碼、忘記密碼（Email 重設連結）、密碼重設
- **帳號安全**：登入失敗計數、自動鎖定與解鎖、速率限制（Rate Limit）
- **超級管理員**：Super Admin 可跨租戶操作，登入時可選擇任意啟用中的租戶
- **稽核日誌**：所有登入/登出/租戶切換事件皆記錄至 `user_event_log`

---

## 2. 資料表結構

### 2.1 users（使用者主表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| user_id | VARCHAR(50) | PK | 使用者 ID |
| email | VARCHAR(200) | NOT NULL, UNIQUE | 電子郵件（登入帳號） |
| password_hash | VARCHAR(255) | NOT NULL | 密碼雜湊值（BCrypt） |
| display_name | VARCHAR(200) | NOT NULL | 顯示名稱 |
| phone | VARCHAR(50) | | 電話號碼 |
| enabled | BOOLEAN | NOT NULL, 預設 true | 帳號是否啟用 |
| locked | BOOLEAN | NOT NULL, 預設 false | 帳號是否鎖定 |
| locked_at | DATETIME | | 鎖定時間 |
| login_fail_count | INT | NOT NULL, 預設 0 | 連續登入失敗次數 |
| is_super_admin | BOOLEAN | NOT NULL, 預設 false | 是否為超級管理員 |
| last_login_at | DATETIME | | 最後登入時間 |
| deleted | BOOLEAN | NOT NULL, 預設 false | 軟刪除標記 |
| deleted_at | DATETIME | | 刪除時間 |
| create_time | DATETIME | NOT NULL, 不可更新 | 建立時間（自動填入） |
| update_time | DATETIME | | 更新時間（自動填入） |
| notify_email_flag | BOOLEAN | NOT NULL, 預設 true | 是否接收 Email 通知 |
| notify_sms_flag | BOOLEAN | NOT NULL, 預設 true | 是否接收簡訊通知 |

### 2.2 roles（角色表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| role_id | VARCHAR(50) | PK | 角色 ID |
| code | VARCHAR(50) | NOT NULL, UNIQUE | 角色代碼（如 ADMIN, OPERATOR） |
| name | VARCHAR(100) | NOT NULL | 角色名稱 |
| description | VARCHAR(500) | | 角色描述 |
| built_in | BOOLEAN | NOT NULL, 預設 true | 是否為內建角色 |
| enabled | BOOLEAN | NOT NULL, 預設 true | 是否啟用 |
| data_scope | VARCHAR(30) | 預設 "ALL" | 資料範圍（ALL / DEPT / SELF） |
| create_time | DATETIME | NOT NULL, 不可更新 | 建立時間 |
| update_time | DATETIME | | 更新時間 |

### 2.3 user_tenant_mapping（使用者-租戶對應表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, 自增 | 主鍵 |
| user_id | VARCHAR(50) | NOT NULL, FK → users | 使用者 ID |
| tenant_id | VARCHAR(50) | NOT NULL, FK → tenants | 租戶 ID |
| role_id | VARCHAR(50) | NOT NULL, FK → roles | 角色 ID |
| dept_id | BIGINT | | 部門 ID |
| default_project_id | VARCHAR(50) | | 預設專案 ID |
| enabled | BOOLEAN | NOT NULL, 預設 true | 此對應是否啟用 |
| create_time | DATETIME | NOT NULL, 不可更新 | 建立時間 |
| update_time | DATETIME | | 更新時間 |

> 套用 Hibernate `@Filter(name = "tenantFilter")` 進行租戶資料隔離。

### 2.4 user_reset_password_token（密碼重設令牌表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| token_id | VARCHAR(100) | PK | 令牌 ID（UUID） |
| user_id | VARCHAR(50) | NOT NULL | 使用者 ID |
| token | VARCHAR(255) | NOT NULL, UNIQUE | 重設令牌值 |
| expired_at | DATETIME | NOT NULL | 過期時間（建立後 30 分鐘） |
| used | BOOLEAN | NOT NULL, 預設 false | 是否已使用 |
| create_time | DATETIME | NOT NULL, 不可更新 | 建立時間（@PrePersist） |

### 2.5 change_password_log（密碼變更日誌表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, 自增 | 主鍵 |
| user_id | VARCHAR(50) | NOT NULL | 使用者 ID |
| change_type | VARCHAR(50) | NOT NULL | 變更類型（USER / RESET / SELF_CHANGE） |
| ip_address | VARCHAR(50) | | 來源 IP |
| create_time | DATETIME | NOT NULL, 不可更新 | 建立時間（@PrePersist） |

---

## 3. 實體關聯

```
users (1) ──────< (N) user_tenant_mapping (N) >────── (1) tenants
                        │
                        └── (N) >────── (1) roles

users (1) ──────< (N) user_reset_password_token
users (1) ──────< (N) change_password_log
```

- 一個使用者可對應多個租戶（透過 `user_tenant_mapping`）
- 每個對應綁定一個角色（role_id）與可選的部門（dept_id）
- `UserTenantMappingEntity` 以 `@ManyToOne(LAZY)` 關聯 `UserEntity`、`TenantEntity`、`RoleEntity`

---

## 4. API 端點摘要

### 4.1 公開端點（NoAuth）

| 方法 | 路徑 | 說明 | 速率限制 |
|---|---|---|---|
| GET | `/v1/noauth/turnstile/config` | 取得 Turnstile 設定（是否啟用 + site key） | — |
| POST | `/v1/noauth/captcha` | 產生圖片驗證碼 | 20 次/分鐘/IP |
| POST | `/v1/noauth/login` | 使用者登入 | 10 次/分鐘/IP |
| POST | `/v1/noauth/token/refresh` | 刷新 Access Token（從 Cookie 讀取 refresh_token） | 30 次/分鐘/IP |
| POST | `/v1/noauth/user/forgot-password` | 忘記密碼（寄送重設信件） | 5 次/5分鐘/IP |
| PUT | `/v1/noauth/user/reset-password` | 重設密碼（使用 token） | 5 次/5分鐘/IP |

### 4.2 需認證端點（Auth）

| 方法 | 路徑 | 說明 |
|---|---|---|
| POST | `/v1/auth/select-tenant` | 多租戶登入後選擇租戶 |
| POST | `/v1/auth/switch-tenant` | 切換當前租戶 |
| POST | `/v1/auth/logout` | 登出 |
| POST | `/v1/auth/idle-logout` | 閒置逾時登出 |
| GET | `/v1/auth/user/info` | 取得當前使用者資訊（含權限、可用租戶列表） |

---

## 5. 業務邏輯描述

### 5.1 登入流程

1. **驗證碼校驗**：優先使用 Turnstile（若前端傳入 `turnstileToken`），否則退回使用圖片驗證碼（`captchaKey` + `captcha`）。兩者皆未提供則拒絕。
2. **查找使用者**：以 email 查詢 `users` 表，找不到回傳 `USER_NOT_FOUND`。
3. **帳號狀態檢查**：
   - `enabled = false` → 拒絕登入（`ACCOUNT_DISABLED`）
   - `locked = true` → 檢查鎖定是否過期（預設 10 分鐘），過期則自動解鎖並重置失敗計數
4. **密碼驗證**：使用 BCrypt 比對。失敗則累加 `login_fail_count`，達到上限（預設 5 次）則鎖定帳號。
5. **成功登入**：重置失敗計數，更新 `last_login_at`。
6. **Token 簽發策略**：
   - **超級管理員**：簽發 Temporary Token + 回傳所有啟用中租戶列表（`needsSelection = true`）
   - **單租戶使用者**：直接簽發完整 JWT（Access + Refresh Token）
   - **多租戶使用者**：簽發 Temporary Token + 回傳該使用者的租戶列表

### 5.2 租戶選擇/切換

- `selectTenant`：在多租戶登入的第二步，使用者選擇租戶後簽發完整 JWT
- `switchTenant`：已登入狀態下切換到另一個租戶，重新簽發 JWT
- 超級管理員可選擇任意啟用中的租戶（不需要 mapping 記錄）
- 一般使用者需有對應的啟用中 `user_tenant_mapping` 記錄

### 5.3 Token 刷新

- 從 HttpOnly Cookie 讀取 `refresh_token`
- 驗證 token 類型必須為 `"refresh"`（防止 Access Token 冒用）
- 沿用原 refresh token 中的 `tenantId`，避免靜默切換租戶

### 5.4 密碼重設

- `forgotPassword`：靜默成功（防止帳號枚舉攻擊），產生 UUID token 存入 DB，寄送 HTML 格式重設信件，令牌有效期 30 分鐘
- `resetPassword`：驗證 token 有效性（未使用 + 未過期），執行密碼複雜度驗證 + 歷史密碼檢查，更新密碼並記錄至 `password_history` 與 `change_password_log`

### 5.5 驗證碼機制

- **圖片驗證碼**：4 位元英數混合，儲存於 Redis（TTL 預設 300 秒），一次性使用，圖片含貝茲曲線干擾、隨機旋轉文字、噪點等抗 OCR 措施
- **Cloudflare Turnstile**：前端嵌入 widget，後端呼叫 `siteverify` API 驗證 token

### 5.6 安全機制

- 所有安全事件透過 `SecurityLogger` 記錄
- 登入事件記錄至 `user_event_log`（含 IP、User-Agent）
- Refresh Token 使用 HttpOnly + Secure + SameSite Cookie
- 跨租戶查詢使用 `TenantContext.setSystemContext()` 繞過租戶過濾器，並在 finally 中恢復原始 context

---

## 6. 資料流圖

### 6.1 登入流程

```
前端                          AuthController              AuthServiceImpl
 │                                │                            │
 │── POST /v1/noauth/login ──────>│                            │
 │                                │── login(req, httpReq) ────>│
 │                                │                            │── verifyCaptcha()
 │                                │                            │   ├── Turnstile? → TurnstileService.verify()
 │                                │                            │   └── 圖片驗證? → CaptchaService.verify() → Redis
 │                                │                            │
 │                                │                            │── findByEmail() → UserRepository
 │                                │                            │── 檢查 enabled / locked
 │                                │                            │── passwordEncoder.matches()
 │                                │                            │── 查詢 user_tenant_mapping（System Context）
 │                                │                            │
 │                                │                            │── [Super Admin] → Temporary Token + 全租戶列表
 │                                │                            │── [單租戶] → resolvePermissions() → 完整 JWT
 │                                │                            │── [多租戶] → Temporary Token + 租戶列表
 │                                │                            │
 │                                │<── LoginResult ────────────│
 │                                │── setRefreshTokenCookie()  │
 │<── BaseResponse<LoginResult> ──│                            │
```

### 6.2 密碼重設流程

```
前端                          AuthController              AuthServiceImpl
 │                                │                            │
 │── POST forgot-password ───────>│── forgotPassword() ───────>│
 │                                │                            │── findByEmail()
 │                                │                            │── 產生 UUID token → DB
 │                                │                            │── PasswordResetMailService.send()
 │                                │                            │       └── 寄送 HTML 信件（含重設連結）
 │<── success ────────────────────│                            │
 │                                │                            │
 │── PUT reset-password ─────────>│── resetPassword() ────────>│
 │                                │                            │── 查詢 token（驗證未用/未過期）
 │                                │                            │── passwordValidator.validate()
 │                                │                            │── passwordValidator.checkNotRecentlyUsed()
 │                                │                            │── 更新密碼 → users
 │                                │                            │── 記錄 → password_history
 │                                │                            │── 標記 token 已使用
 │                                │                            │── 記錄 → change_password_log
 │<── success ────────────────────│                            │
```

---

## 7. 列舉值定義

### 7.1 change_type（密碼變更類型）

| 值 | 說明 |
|---|---|
| `USER` | 使用者透過 Auth 模組變更密碼 |
| `RESET` | 透過忘記密碼流程重設 |
| `SELF_CHANGE` | 使用者透過 User 模組自助變更 |

### 7.2 data_scope（角色資料範圍）

| 值 | 說明 |
|---|---|
| `ALL` | 可存取所有資料 |
| `DEPT` | 僅可存取所屬部門資料 |
| `SELF` | 僅可存取自己的資料 |

### 7.3 AuditEventType（稽核事件類型，Auth 相關）

| 值 | 說明 |
|---|---|
| `LOGIN_SUCCESS` | 登入成功 |
| `LOGIN_FAIL` | 登入失敗 |
| `LOGOUT` | 手動登出 |
| `IDLE_TIMEOUT_LOGOUT` | 閒置逾時登出 |
| `TENANT_SWITCH` | 租戶切換 |

### 7.4 SecurityEvent（安全事件日誌）

| 值 | 說明 |
|---|---|
| `LOGIN_FAILED` | 登入失敗 |
| `CAPTCHA_FAILED` | 驗證碼校驗失敗 |
| `PASSWORD_RESET_REQUEST` | 密碼重設請求 |

### 7.5 ErrorCode（Auth 相關錯誤碼）

| 代碼 | 說明 |
|---|---|
| `USER_NOT_FOUND` | 使用者不存在 |
| `ACCOUNT_DISABLED` | 帳號已停用 |
| `ACCOUNT_LOCKED` | 帳號已鎖定 |
| `LOGIN_FAIL` | 登入失敗（密碼錯誤） |
| `CAPTCHA_INVALID` | 驗證碼無效 |
| `TENANT_ACCESS_DENIED` | 無權存取該租戶 |
| `TENANT_NOT_FOUND` | 租戶不存在 |
| `REFRESH_TOKEN_INVALID` | Refresh Token 無效 |
| `ACCESS_TOKEN_INVALID` | Access Token 無效 |
| `RESET_PASSWORD_INVALID_TOKEN` | 密碼重設令牌無效或已過期 |
