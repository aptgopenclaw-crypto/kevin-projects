# User 模組技術文件

> 套件路徑：`com.taipei.iot.user`
> 系統：台北市路燈管理系統（IoT Streetlight Management）

---

## 1. 模組概述

User 模組負責使用者帳號的管理與自助操作，包含：

- **管理端操作（UserAdminService）**：帳號建立、更新、停用、軟刪除、租戶角色指派/移除、分頁查詢
- **自助操作（UserSelfService）**：使用者自行更新個人資料、變更密碼
- **密碼驗證（PasswordValidator）**：密碼複雜度驗證、歷史密碼檢查
- **稽核日誌（UserAuditService）**：所有使用者操作記錄至 `user_info_log`
- **資料範圍控制（DataScope）**：依據操作者的部門權限範圍過濾可見使用者

---

## 2. 資料表結構

### 2.1 password_history（密碼歷史表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, 自增 | 主鍵 |
| user_id | VARCHAR(50) | NOT NULL | 使用者 ID |
| password_hash | VARCHAR(255) | NOT NULL | 密碼雜湊值 |
| create_time | DATETIME | NOT NULL, 不可更新 | 建立時間（@CreatedDate） |

### 2.2 user_info_log（使用者操作日誌表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, 自增 | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶 ID（租戶過濾欄位） |
| action_type | VARCHAR(20) | NOT NULL | 操作類型 |
| action_user_id | VARCHAR(50) | NOT NULL | 執行操作的使用者 ID |
| target_user_id | VARCHAR(50) | NOT NULL | 被操作的目標使用者 ID |
| email | VARCHAR(200) | | 電子郵件 |
| display_name | VARCHAR(200) | | 顯示名稱 |
| role_code | VARCHAR(50) | | 角色代碼 |
| dept_id | VARCHAR(50) | | 部門 ID |
| detail | VARCHAR(1000) | | 操作詳情 |
| create_time | DATETIME | NOT NULL, 不可更新 | 建立時間（@CreatedDate） |

> 套用 Hibernate `@Filter(name = "tenantFilter")` 進行租戶資料隔離。
> 實作 `TenantAware` 介面，由 `TenantEntityListener` 自動填入 tenant_id。

### 2.3 相關表（定義於 auth 模組，user 模組會讀寫）

- `users`（UserEntity）— 使用者主表
- `user_tenant_mapping`（UserTenantMappingEntity）— 使用者租戶對應
- `change_password_log`（ChangePasswordLogEntity）— 密碼變更日誌

---

## 3. 實體關聯

```
users (1) ──────< (N) password_history
users (1) ──────< (N) user_info_log（透過 target_user_id）
users (1) ──────< (N) user_tenant_mapping

user_info_log.action_user_id ───> users.user_id（操作者）
user_info_log.target_user_id ───> users.user_id（目標）
```

---

## 4. API 端點摘要

### 4.1 管理端（UserAdminController）

| 方法 | 路徑 | 說明 | 稽核 | 權限 |
|---|---|---|---|---|
| GET | `/v1/auth/users` | 分頁查詢使用者列表（支援關鍵字搜尋） | — | — |
| POST | `/v1/auth/users` | 建立使用者 | CREATE_USER | — |
| PUT | `/v1/auth/users/{userId}` | 更新使用者資料 | UPDATE_USER | — |
| DELETE | `/v1/auth/users/{userId}` | 停用使用者 | DISABLE_USER | — |
| PATCH | `/v1/auth/users/{userId}/soft-delete` | 軟刪除使用者 | SOFT_DELETE_USER | — |
| GET | `/v1/auth/users/{userId}/tenant-roles` | 取得使用者的租戶角色列表 | — | SUPER_ADMIN |
| POST | `/v1/auth/users/{userId}/tenant-roles` | 新增租戶角色對應 | — | SUPER_ADMIN |
| DELETE | `/v1/auth/users/{userId}/tenant-roles/{mappingId}` | 移除租戶角色對應 | — | SUPER_ADMIN |

### 4.2 自助端（UserSelfController）

| 方法 | 路徑 | 說明 |
|---|---|---|
| PUT | `/v1/auth/user/my` | 更新自己的個人資料 |
| POST | `/v1/auth/user/change-password` | 自助變更密碼 |

---

## 5. 業務邏輯描述

### 5.1 使用者列表查詢（DataScope 過濾）

1. 取得當前租戶 ID 與操作者的 DataScope
2. 依 DataScope 決定查詢策略：
   - **System Context**：查詢所有使用者（無租戶/部門限制）
   - **ALL scope**：查詢當前租戶下所有使用者
   - **部門範圍限制**：透過 `DataScopeHelper.getVisibleDeptIds()` 取得可見部門 ID 列表，僅查詢這些部門下的使用者
3. 支援關鍵字搜尋（keyword）
4. 回傳分頁結果（`PageResponse`）

### 5.2 建立使用者

1. **DataScope 檢核**：操作者只能在自己部門範圍內建立帳號（`dataScopeHelper.isDeptInScope()`）
2. **角色指派檢核**：操作者只能指派自己權限範圍內的角色（`roleService.isRoleAssignable()`）
3. **Email 唯一性檢查**
4. **密碼複雜度驗證**（`passwordValidator.validate()`）
5. 建立 `UserEntity`（UUID 作為 user_id）
6. 建立 `UserTenantMappingEntity`（綁定租戶 + 角色 + 部門）
7. 記錄初始密碼至 `password_history`
8. 記錄稽核日誌

### 5.3 停用與軟刪除

- **停用（disableUser）**：設定 `enabled = false`，帳號無法登入但資料保留
- **軟刪除（softDeleteUser）**：設定 `deleted = true`、`deleted_at = now()`、`enabled = false`
- 兩者皆禁止操作者對自己執行（防止自鎖）

### 5.4 租戶角色管理（僅 SUPER_ADMIN）

- **新增租戶角色**（`addTenantRole`）：為使用者新增一筆 `user_tenant_mapping`，需檢查該使用者在目標租戶是否已有對應
- **移除租戶角色**（`removeTenantRole`）：刪除指定的 mapping 記錄，需驗證 mapping 的 user_id 匹配
- **查詢租戶角色**（`getUserTenantMappings`）：列出使用者所有租戶對應（System Context 下可見全部，否則僅當前租戶）

### 5.5 自助密碼變更

1. 密碼複雜度驗證（`passwordValidator.validate()`）
2. 歷史密碼檢查（`checkNotRecentlyUsed()`）：比對最近 5 筆密碼歷史
3. 更新密碼
4. 記錄至 `password_history` 與 `change_password_log`（類型 `SELF_CHANGE`）
5. 記錄稽核日誌

### 5.6 密碼驗證規則

| 規則 | 預設值 | 設定鍵 |
|---|---|---|
| 最小長度 | 8 | `user.password.min-length` |
| 需包含大寫字母 | true | `user.password.require-uppercase` |
| 需包含小寫字母 | true | `user.password.require-lowercase` |
| 需包含數字 | true | `user.password.require-digit` |
| 歷史密碼檢查筆數 | 5 | `user.password.history-count` |

---

## 6. 資料流圖

### 6.1 建立使用者流程

```
管理前端                   UserAdminController          UserAdminService
 │                            │                            │
 │── POST /v1/auth/users ────>│                            │
 │   body: CreateUserRequest  │── createUser(admin, req) ─>│
 │                            │                            │── dataScopeHelper.isDeptInScope()
 │                            │                            │── roleService.isRoleAssignable()
 │                            │                            │── 檢查 email 唯一性
 │                            │                            │── passwordValidator.validate()
 │                            │                            │── 建立 UserEntity → users
 │                            │                            │── 建立 UserTenantMapping → user_tenant_mapping
 │                            │                            │── 記錄 → password_history
 │                            │                            │── userAuditService.logAction() → user_info_log
 │                            │                            │
 │<── UserListItemDto ────────│<────────────────────────────│
```

### 6.2 自助變更密碼流程

```
前端                      UserSelfController           UserSelfService
 │                            │                            │
 │── POST /change-password ──>│                            │
 │   body: {newPassword}      │── changePassword(uid,req) >│
 │                            │                            │── passwordValidator.validate()
 │                            │                            │── passwordValidator.checkNotRecentlyUsed()
 │                            │                            │   └── 查詢 password_history（最近 5 筆）
 │                            │                            │   └── BCrypt 逐筆比對
 │                            │                            │── 更新密碼 → users
 │                            │                            │── 記錄 → password_history
 │                            │                            │── 記錄 → change_password_log (SELF_CHANGE)
 │                            │                            │── userAuditService.logAction()
 │<── success ────────────────│<────────────────────────────│
```

### 6.3 使用者列表查詢（DataScope 過濾）

```
管理前端                   UserAdminController          UserAdminService
 │                            │                            │
 │── GET /v1/auth/users ─────>│                            │
 │   ?page=0&size=20&keyword  │── listUsers(p, s, kw) ───>│
 │                            │                            │── TenantContext.getCurrentTenantId()
 │                            │                            │── DataScopeHelper.getVisibleDeptIds()
 │                            │                            │
 │                            │                            │── [System] → findAllActive(kw)
 │                            │                            │── [ALL scope] → findActiveByTenantId(tid, kw)
 │                            │                            │── [Dept scope] → findActiveByTenantIdAndDeptIdIn(...)
 │                            │                            │
 │                            │                            │── mapping → UserListItemDto
 │<── PageResponse ───────────│<────────────────────────────│
```

---

## 7. 列舉值定義

### 7.1 action_type（使用者操作日誌類型）

| 值 | 說明 |
|---|---|
| `CREATE` | 建立帳號 |
| `UPDATE` | 更新帳號資料 |
| `DISABLE` | 停用帳號 |
| `DELETE` | 軟刪除帳號 |
| `ADD_TENANT` | 新增租戶角色對應 |
| `REMOVE_TENANT` | 移除租戶角色對應 |

### 7.2 change_type（密碼變更類型，跨模組共用）

| 值 | 說明 |
|---|---|
| `USER` | Auth 模組的密碼變更 |
| `RESET` | 忘記密碼流程重設 |
| `SELF_CHANGE` | User 模組自助變更 |

### 7.3 AuditEventType（User 相關稽核事件）

| 值 | 說明 |
|---|---|
| `CREATE_USER` | 建立使用者 |
| `UPDATE_USER` | 更新使用者 |
| `DISABLE_USER` | 停用使用者 |
| `SOFT_DELETE_USER` | 軟刪除使用者 |

### 7.4 ErrorCode（User 相關錯誤碼）

| 代碼 | 說明 |
|---|---|
| `USER_NOT_FOUND` | 使用者不存在 |
| `USER_ALREADY_EXISTS` | Email 已被使用或租戶對應已存在 |
| `PERMISSION_DENIED` | 權限不足（部門/角色範圍外操作） |
| `MAPPING_NOT_FOUND` | 租戶角色對應不存在 |
| `ROLE_NOT_FOUND` | 角色不存在 |
| `RESET_PASSWORD_ERROR` | 密碼不符合複雜度規則 |
| `PASSWORD_RECENTLY_USED` | 密碼與最近使用的密碼重複 |
