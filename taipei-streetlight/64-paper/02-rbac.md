# RBAC 模組技術文件

> 套件路徑：`com.taipei.iot.rbac`
> 系統：台北市路燈管理系統（IoT Streetlight Management）

---

## 1. 模組概述

RBAC（Role-Based Access Control）模組負責系統的角色權限管理與選單控制，包含：

- **選單管理（Menu）**：樹狀選單結構，支援目錄/頁面/按鈕三種類型，透過 `permission_code` 與權限綁定
- **權限管理（Permission）**：定義系統中的細粒度操作權限，以群組（group_name）分類
- **角色權限指派（RolePermission）**：建立角色與權限的多對多關聯，支援租戶範圍（tenant scope）
- **角色管理**：角色的 CRUD 操作、啟停用、可指派角色清單（依據操作者的 DataScope 過濾）

---

## 2. 資料表結構

### 2.1 menus（選單表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| menu_id | BIGINT | PK, 自增 | 選單 ID |
| parent_id | BIGINT | | 父選單 ID（NULL 為根節點） |
| name | VARCHAR(100) | NOT NULL | 選單名稱 |
| menu_type | VARCHAR(20) | NOT NULL | 選單類型（DIRECTORY / PAGE / BUTTON） |
| route_name | VARCHAR(100) | | 前端路由名稱 |
| route_path | VARCHAR(200) | | 前端路由路徑 |
| component | VARCHAR(200) | | 前端元件路徑 |
| permission_code | VARCHAR(100) | | 對應權限代碼 |
| icon | VARCHAR(50) | | 圖示名稱 |
| sort_order | INT | 預設 0 | 排序序號 |
| visible | BOOLEAN | 預設 true | 是否可見 |
| keep_alive | BOOLEAN | 預設 false | 是否快取元件 |
| redirect | VARCHAR(200) | | 重導向路徑 |
| create_time | DATETIME | NOT NULL, 不可更新 | 建立時間 |
| update_time | DATETIME | | 更新時間 |

### 2.2 permissions（權限表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| permission_id | VARCHAR(50) | PK | 權限 ID |
| code | VARCHAR(100) | NOT NULL, UNIQUE | 權限代碼（如 `user:create`, `menu:delete`） |
| name | VARCHAR(200) | NOT NULL | 權限名稱 |
| group_name | VARCHAR(100) | | 權限群組名稱（用於分類顯示） |
| sort_order | INT | | 排序序號 |

### 2.3 role_permissions（角色-權限關聯表）

| 欄位名稱 | 資料型別 | 約束 | 說明 |
|---|---|---|---|
| role_id | VARCHAR(50) | PK（複合）, NOT NULL | 角色 ID |
| permission_id | VARCHAR(50) | PK（複合）, NOT NULL | 權限 ID |
| tenant_id | VARCHAR(50) | | 租戶 ID（NULL 表示全域權限） |

> 使用 `@IdClass(RolePermissionId.class)` 複合主鍵。

---

## 3. 實體關聯

```
roles (1) ──────< (N) role_permissions (N) >────── (1) permissions

menus（樹狀自關聯）:
  menus.parent_id ──> menus.menu_id

menus.permission_code ···> permissions.code（邏輯關聯，非 FK）
```

- `role_permissions` 為角色與權限的多對多關聯表
- `tenant_id` 欄位允許 NULL，NULL 表示全域範圍的權限指派；非 NULL 表示僅在特定租戶下有效
- `menus` 透過 `permission_code` 與 `permissions.code` 進行邏輯關聯，決定使用者是否可見該選單項

---

## 4. API 端點摘要

### 4.1 選單管理

| 方法 | 路徑 | 說明 | 稽核 |
|---|---|---|---|
| GET | `/v1/auth/menus/tree` | 取得完整選單樹 | — |
| GET | `/v1/auth/menus/my` | 取得當前使用者可見選單（依角色權限過濾） | — |
| POST | `/v1/auth/menus` | 建立選單 | CREATE_MENU |
| PUT | `/v1/auth/menus` | 更新選單 | — |
| DELETE | `/v1/auth/menus/{menuId}` | 刪除選單 | DELETE_MENU |
| PATCH | `/v1/auth/menus/{menuId}/visible` | 切換選單可見性 | TOGGLE_VISIBLE |

### 4.2 權限管理

| 方法 | 路徑 | 說明 |
|---|---|---|
| GET | `/v1/auth/permissions` | 列出所有權限（依群組/排序） |

### 4.3 角色管理

| 方法 | 路徑 | 說明 |
|---|---|---|
| GET | `/v1/auth/roles` | 列出所有角色 |
| GET | `/v1/auth/roles/assignable` | 列出當前使用者可指派的角色 |
| POST | `/v1/auth/roles` | 建立角色 |
| PUT | `/v1/auth/roles/{roleId}` | 更新角色 |
| PATCH | `/v1/auth/roles/{roleId}/enabled` | 啟用/停用角色 |
| GET | `/v1/auth/roles/{roleId}/permissions` | 取得角色的權限清單 |
| PUT | `/v1/auth/roles/{roleId}/permissions` | 指派角色權限（全量替換） |

---

## 5. 業務邏輯描述

### 5.1 使用者選單載入

1. 從 `Authentication` 取得使用者的角色 ID 列表
2. **超級管理員**（`ROLE_SUPER_ADMIN`）：回傳所有 `visible = true` 的選單
3. **一般使用者**：
   a. 查詢 `role_permissions`（含租戶範圍過濾）→ 取得 permission ID 列表
   b. 查詢 `permissions` → 取得 permission code 列表
   c. 查詢 `menus`（`permission_code IN codes AND visible = true`）→ 取得有權選單
   d. 自動補入有可見子節點的 `DIRECTORY` 類型父選單
   e. 組裝為樹狀結構回傳

### 5.2 角色權限指派

- `assignPermissions()`：先刪除該角色所有現有權限（`deleteByRoleId`），再批次新增
- 新增的權限 `tenant_id` 設為 NULL（全域範圍）
- 回傳更新後的權限清單

### 5.3 可指派角色過濾

- 依據操作者的 `DataScope` 決定可指派的角色：
  - `ALL` scope 操作者 → 可指派所有啟用角色
  - `THIS_LEVEL` / `THIS_LEVEL_AND_BELOW` → 只能指派 `dataScope != ALL` 的角色
- 防止低權限使用者提升他人為高權限角色

### 5.4 選單刪除保護

- 刪除前檢查是否有子選單（`existsByParentId`），有子選單則拒絕刪除（`MENU_HAS_CHILDREN`）

### 5.5 角色建立

- 角色 ID 格式：`ROLE_` + 12 位大寫 UUID 片段（如 `ROLE_A1B2C3D4E5F6`）
- 自訂角色 `builtIn = false`
- 角色代碼（code）需唯一，重複則拋出 `ROLE_CODE_DUPLICATE`

---

## 6. 資料流圖

### 6.1 使用者選單載入流程

```
前端                      MenuController                MenuService
 │                            │                            │
 │── GET /menus/my ──────────>│                            │
 │                            │── getMyMenus(roles, tid) ─>│
 │                            │                            │── SUPER_ADMIN?
 │                            │                            │   └── YES → 回傳所有 visible 選單
 │                            │                            │
 │                            │                            │── 查詢 role_permissions（租戶範圍）
 │                            │                            │── 查詢 permissions → code 列表
 │                            │                            │── 查詢 menus（code 匹配 + visible）
 │                            │                            │── 補入 DIRECTORY 父節點
 │                            │                            │── 組裝樹狀結構
 │                            │                            │
 │<── List<UserMenuDto> ──────│<── 樹狀選單 ────────────────│
```

### 6.2 角色權限指派流程

```
管理前端                   RoleController               RoleService
 │                            │                            │
 │── PUT /roles/{id}/perms ──>│                            │
 │   body: {permissionIds}    │── assignPermissions() ────>│
 │                            │                            │── 驗證角色存在
 │                            │                            │── deleteByRoleId() (清除舊權限)
 │                            │                            │── saveAll() (批次新增)
 │                            │                            │── 查詢並回傳更新後權限
 │<── RolePermissionListDto ──│<────────────────────────────│
```

---

## 7. 列舉值定義

### 7.1 menu_type（選單類型）

| 值 | 說明 |
|---|---|
| `DIRECTORY` | 目錄（容器節點，不對應實際頁面） |
| `PAGE` | 頁面（對應前端路由元件） |
| `BUTTON` | 按鈕（對應頁面內的操作權限） |

### 7.2 data_scope（資料範圍）

| 值 | 說明 |
|---|---|
| `ALL` | 可存取所有資料，可指派所有角色 |
| `THIS_LEVEL` | 僅限本層級部門 |
| `THIS_LEVEL_AND_BELOW` | 本層級及下屬部門 |

### 7.3 AuditEventType（RBAC 相關稽核事件）

| 值 | 說明 |
|---|---|
| `CREATE_MENU` | 建立選單 |
| `DELETE_MENU` | 刪除選單 |
| `TOGGLE_VISIBLE` | 切換選單可見性 |

### 7.4 ErrorCode（RBAC 相關錯誤碼）

| 代碼 | 說明 |
|---|---|
| `MENU_NOT_FOUND` | 選單不存在 |
| `MENU_HAS_CHILDREN` | 選單含有子項目，無法刪除 |
| `ROLE_NOT_FOUND` | 角色不存在 |
| `ROLE_CODE_DUPLICATE` | 角色代碼重複 |
| `PERMISSION_DENIED` | 權限不足 |
