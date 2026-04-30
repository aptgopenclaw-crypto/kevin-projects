# TS-01 系統管理 — Test Specification

> **對應 SA**：SA-01-system-mgmt (FN-01-001 ~ FN-01-055)  
> **對應 SD**：SD-01-system-mgmt  
> **Test Classes**：12 classes, 99 test methods  
> **最新驗證**：2026-04-24 · 全部 PASS

---

## 使用方式

本文件是 **AI 實作 prompt 的驗收標準**。請 AI 實作或修改某個 FN 時，附上對應段落：

```
請實作 FN-01-051 新增部門
【SA】SA-01 §部門管理
【SD】SD-01 §3 POST /v1/auth/admin/depts
【TC】（貼 FN-01-051 的 Test Cases 表）
請確保所有 TC PASS。
```

---

## 1. 帳號管理 (FN-01-001 ~ FN-01-009)

### FN-01-001 帳號列表查詢

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/admin/users`  
**Service**: `UserAdminService.listUsers()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 需要 `USER_MANAGE` 權限
- Data Scope 過濾：ALL 看全部、THIS_LEVEL 看本部門、THIS_LEVEL_AND_BELOW 看本+下級
- 多租戶隔離：普通管理員只看本租戶，SUPER_ADMIN 看所有租戶
- 支援分頁 / keyword 搜尋 / 排序

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-001-01 | Happy | 管理員查詢列表 | ADMIN, perm=USER_MANAGE | GET /users | 200 | HTTP 200 | ✅ UserAdminControllerTest.listUsers_adminRole |
| TC-01-001-02 | Error | 無權限查詢 | VIEWER, 無 USER_MANAGE | GET /users | 403 | HTTP 403 | ✅ UserAdminControllerTest.listUsers_viewerRole |
| TC-01-001-03 | Error | 未登入 | 無 token | GET /users | 401 | HTTP 401 | ✅ UserAdminControllerTest.listUsers_noAuth |
| TC-01-001-04 | Happy | 租戶隔離—普通管理員 | ADMIN, tenantId=1 | GET /users | 僅看到 tenant 1 用戶 | 結果集 tenantId 一致 | ✅ UserAdminServiceTest.listUsers_adminSeesOwnTenant |
| TC-01-001-05 | Happy | 超級管理員看全部 | SUPER_ADMIN | GET /users | 跨租戶結果 | 結果含多租戶 | ✅ UserAdminServiceTest.listUsers_superAdminSeesAll |
| TC-01-001-06 | Happy | DataScope—本部門 | deptAdmin, THIS_LEVEL | GET /users | 僅本部門用戶 | deptId 篩選 | ✅ UserAdminServiceTest.listUsers_deptAdmin_onlySeesOwnDept |
| TC-01-001-07 | Happy | DataScope—多部門 | deptAdmin, 管轄多部門 | GET /users | 多部門用戶 | 多 deptId | ✅ UserAdminServiceTest.listUsers_deptAdmin_seesMultipleDepts |

---

### FN-01-002 新增帳號

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `POST /v1/auth/admin/users`  
**Service**: `UserAdminService.createUser()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 需要 `USER_MANAGE` 權限
- Email 不可重複（同租戶內）
- 密碼需符合強度規則
- Data Scope 限制：部門管理員只能在管轄部門新增用戶

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-002-01 | Happy | 正常新增 | perm=USER_MANAGE | valid user data | 200, user created | user.id 非 null | ✅ UserAdminServiceTest.createUser_success |
| TC-01-002-02 | Error | Email 重複 | email 已存在 | duplicate email | `USER_ALREADY_EXISTS` | errorCode | ✅ UserAdminServiceTest.createUser_emailDuplicate |
| TC-01-002-03 | Error | 密碼太弱 | — | password="123" | `RESET_PASSWORD_ERROR` | errorCode | ✅ UserAdminServiceTest.createUser_weakPassword |
| TC-01-002-04 | Error | 部門越權 | deptAdmin, target dept 不在管轄 | out-of-scope deptId | `PERMISSION_DENIED` | errorCode | ✅ UserAdminServiceTest.createUser_deptOutOfScope |
| TC-01-002-05 | Happy | 部門範圍內 | deptAdmin, target dept 在管轄 | in-scope deptId | 200 | 成功建立 | ✅ UserAdminServiceTest.createUser_deptInScope |

---

### FN-01-003 編輯帳號

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/users/{id}`  
**Service**: `UserAdminService.updateUser()` / `UserSelfService.updateProfile()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 管理員可編輯他人帳號（需 USER_MANAGE）
- 用戶可編輯自己的 profile（name, phone 等）

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-003-01 | Happy | 管理員編輯帳號 | perm=USER_MANAGE | PUT /users/{id} | 200 | 欄位更新 | ✅ UserAdminServiceTest.updateUser_success |
| TC-01-003-02 | Happy | 編輯個人資料 | 已登入 | PUT /auth/profile | 200 | name 更新 | ✅ UserSelfServiceTest.updateOwnProfile |

---

### FN-01-004 停用帳號

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/users/{id}/disable`  
**Service**: `UserAdminService.disableUser()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 不可停用自己的帳號

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-004-01 | Happy | 停用帳號 | perm=USER_MANAGE | PUT /users/{id}/disable | 200, enabled=false | enabled 欄位 | ✅ UserAdminServiceTest.disableUser_success |
| TC-01-004-02 | Error | 停用自己 | 操作者 = 目標 | PUT /users/{self}/disable | `PERMISSION_DENIED` | errorCode | ✅ UserAdminServiceTest.disableUser_self |

---

### FN-01-005 復用帳號

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/users/{id}/enable`  
**Service**: `UserAdminService.enableUser()` | **SRS**: SRS-02-001 | **Spec**: §2-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-005-01 | Happy | 復用帳號 | enabled=false | PUT /users/{id}/enable | 200, enabled=true | enabled 欄位 | ⬜ 待補 |

---

### FN-01-006 重設帳號密碼（管理員）

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/users/{id}/reset-password`  
**Service**: `UserAdminService.resetPassword()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 產生臨時密碼或設定指定密碼
- 密碼需符合強度規則

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-006-01 | Happy | 管理員重設密碼 | perm=USER_MANAGE | PUT /users/{id}/reset-password | 200 | 密碼更新 | ⬜ 待補 |
| TC-01-006-02 | Error | 無權限 | 無 USER_MANAGE | PUT /users/{id}/reset-password | 403 | HTTP 403 | ⬜ 待補 |

---

### FN-01-007 指派角色

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/users/{id}/roles`  
**Service**: `UserAdminService.addTenantRole()` / `removeTenantRole()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 一個用戶在同租戶可有多角色
- 不可重複指派同角色
- 移除不存在的 mapping → MAPPING_NOT_FOUND

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-007-01 | Happy | 指派角色 | perm=USER_MANAGE | addTenantRole(userId, roleId) | 200 | mapping 建立 | ✅ UserAdminServiceTest.addTenantRole_success |
| TC-01-007-02 | Error | 重複指派 | role 已指派 | addTenantRole(duplicate) | `USER_ALREADY_EXISTS` | errorCode | ✅ UserAdminServiceTest.addTenantRole_duplicate |
| TC-01-007-03 | Happy | 移除角色 | mapping 存在 | removeTenantRole | 200 | mapping 刪除 | ✅ UserAdminServiceTest.removeTenantRole_success |
| TC-01-007-04 | Error | 移除不存在 mapping | 無此 mapping | removeTenantRole | `MAPPING_NOT_FOUND` | errorCode | ✅ UserAdminServiceTest.removeTenantRole_notFound |

---

### FN-01-008 查看帳號詳情

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/admin/users/{id}`  
**Service**: `UserAdminService.getUserById()` | **SRS**: SRS-02-001 | **Spec**: §2-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-008-01 | Happy | 查看帳號詳情 | perm=USER_MANAGE, user exists | GET /users/{id} | 200, user detail | 含 roles, dept | ⬜ 待補 |
| TC-01-008-02 | Error | 用戶不存在 | invalid id | GET /users/{id} | 404 | HTTP 404 | ⬜ 待補 |

---

### FN-01-009 匯出帳號清單

**SA**: SA-01 §帳號管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/admin/users/export`  
**Service**: `UserAdminService.exportUsers()` | **SRS**: SRS-02-001 | **Spec**: §2-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-009-01 | Happy | 匯出成功 | perm=USER_MANAGE | GET /users/export | 200, Excel file | Content-Type | ⬜ 待實作 |
| TC-01-009-02 | Error | 無權限 | 無 USER_MANAGE | GET /users/export | 403 | HTTP 403 | ⬜ 待實作 |

---

## 2. 角色管理 (FN-01-010 ~ FN-01-017)

### FN-01-010 角色列表查詢

**SA**: SA-01 §角色管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/admin/roles`  
**Service**: `RoleService.listRoles()` | **SRS**: SRS-02-002 | **Spec**: §2-2

**商業規則**：
- 系統內建 6 個角色（SUPER_ADMIN, ADMIN, DEPT_ADMIN, OPERATOR, VIEWER, CONTRACTOR）

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-010-01 | Happy | 含內建角色 | ADMIN | GET /roles | 200, ≥ 6 roles | list.size() ≥ 6 | ✅ RoleServiceTest.listRoles_6BuiltIn |

---

### FN-01-011 新增角色

**SA**: SA-01 §角色管理 | **SD**: SD-01 §3 | **API**: `POST /v1/auth/admin/roles`  
**Service**: `RoleService.createRole()` | **SRS**: SRS-02-002 | **Spec**: §2-2

**商業規則**：
- roleCode 不可重複

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-011-01 | Happy | 新增自訂角色 | perm=ROLE_MANAGE | code="CUSTOM" | 200, role created | role.id 非 null | ✅ RoleServiceTest.createRole |
| TC-01-011-02 | Error | 角色碼重複 | code 已存在 | duplicate code | `ROLE_CODE_DUPLICATE` | errorCode | ✅ RoleServiceTest.createRole_duplicateCode |

---

### FN-01-012 編輯角色

**SA**: SA-01 §角色管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/roles/{id}`  
**Service**: `RoleService.updateRole()` | **SRS**: SRS-02-002 | **Spec**: §2-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-012-01 | Happy | 更新角色名稱 | role exists | PUT /roles/{id} | 200 | name 更新 | ✅ RoleServiceTest.updateRole |
| TC-01-012-02 | Error | 角色不存在 | invalid id | PUT /roles/{id} | not found error | errorCode | ✅ RoleServiceTest.updateRole_notFound |

---

### FN-01-013 刪除角色

**SA**: SA-01 §角色管理 | **SD**: SD-01 §3 | **API**: `DELETE /v1/auth/admin/roles/{id}`  
**Service**: `RoleService.toggleEnabled()` | **SRS**: SRS-02-002 | **Spec**: §2-2

**商業規則**：
- 軟刪除：enabled=false

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-013-01 | Happy | 停用角色 | role exists | toggleEnabled(false) | enabled=false | 狀態更新 | ✅ RoleServiceTest.toggleEnabled |

---

### FN-01-014 指派角色權限

**SA**: SA-01 §角色管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/roles/{id}/permissions`  
**Service**: `RoleService.assignPermissions()` | **SRS**: SRS-02-002 | **Spec**: §2-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-014-01 | Happy | 指派權限 | perm=ROLE_MANAGE | permissionIds=[1,2,3] | 200 | 權限更新 | ✅ RoleServiceTest.assignPermissions |

---

### FN-01-015 查看角色權限

**SA**: SA-01 §角色管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/admin/roles/{id}/permissions`  
**Service**: `RoleService.getRolePermissions()` | **SRS**: SRS-02-002 | **Spec**: §2-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-015-01 | Happy | 查看角色權限 | role exists | GET /roles/{id}/permissions | 200, permission list | list 非空 | ✅ RoleServiceTest.getRolePermissions |
| TC-01-015-02 | Error | 角色不存在 | invalid id | GET /roles/{id}/permissions | not found | errorCode | ✅ RoleServiceTest.getRolePermissions_notFound |

---

### FN-01-016 移動角色層級

**SA**: SA-01 §角色管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/roles/{id}/sort`  
**Service**: `RoleService.updateSort()` | **SRS**: SRS-02-002 | **Spec**: §2-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-016-01 | Happy | 調整排序 | perm=ROLE_MANAGE | sortOrder=5 | 200 | sortOrder 更新 | ⬜ 待實作 |

---

### FN-01-017 複製角色權限

**SA**: SA-01 §角色管理 | **SD**: SD-01 §3 | **API**: `POST /v1/auth/admin/roles/{id}/copy`  
**Service**: `RoleService.copyRole()` | **SRS**: SRS-02-002 | **Spec**: §2-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-017-01 | Happy | 複製角色 | role exists | POST /roles/{id}/copy | 201, new role | 權限一致 | ⬜ 待實作 |

---

## 3. 選單管理 (FN-01-018 ~ FN-01-021)

### FN-01-018 選單樹查詢

**SA**: SA-01 §選單管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/admin/menus/tree`  
**Service**: `MenuService.getMenuTree()` / `getMyMenus()` | **SRS**: SRS-02-002 | **Spec**: §2-2

**商業規則**：
- 完整選單樹：SUPER_ADMIN 可見所有
- 個人選單：依角色過濾

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-018-01 | Happy | 完整選單樹 | SUPER_ADMIN | GET /menus/tree | 200, full tree | 含所有 menu | ✅ MenuServiceTest.getMenuTree |
| TC-01-018-02 | Happy | ADMIN 可見選單 | ADMIN role | GET /menus/my | 過濾後選單 | 只含 ADMIN 可見 | ✅ MenuServiceTest.getMyMenus_adminRole |
| TC-01-018-03 | Happy | VIEWER 可見選單 | VIEWER role | GET /menus/my | 唯讀選單 | 只含 VIEWER 可見 | ✅ MenuServiceTest.getMyMenus_viewerRole |
| TC-01-018-04 | API | 取得我的選單 | authenticated | GET /menus/my | 200 | HTTP 200 | ✅ MenuControllerTest.getMyMenus |

---

### FN-01-019 新增選單

**SA**: SA-01 §選單管理 | **SD**: SD-01 §3 | **API**: `POST /v1/auth/admin/menus`  
**Service**: `MenuService.createMenu()` | **SRS**: SRS-02-002 | **Spec**: §2-2

**商業規則**：
- 僅 SUPER_ADMIN 可新增選單

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-019-01 | Happy | 超管新增 | SUPER_ADMIN | POST /menus | 200 | menu created | ✅ MenuControllerTest.createMenu_superAdmin + MenuServiceTest.createMenu |
| TC-01-019-02 | Error | 非超管 → 403 | ADMIN role | POST /menus | 403 | HTTP 403 | ✅ MenuControllerTest.createMenu_adminRole |

---

### FN-01-020 編輯選單

**SA**: SA-01 §選單管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/menus/{id}`  
**Service**: `MenuService.toggleVisible()` | **SRS**: SRS-02-002 | **Spec**: §2-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-020-01 | Happy | 切換顯示 | menu exists | toggleVisible | 200, visible 翻轉 | visible 欄位 | ✅ MenuServiceTest.toggleVisible |
| TC-01-020-02 | Error | 選單不存在 | invalid id | toggleVisible | not found | errorCode | ✅ MenuServiceTest.toggleVisible_notFound |

---

### FN-01-021 刪除選單

**SA**: SA-01 §選單管理 | **SD**: SD-01 §3 | **API**: `DELETE /v1/auth/admin/menus/{id}`  
**Service**: `MenuService.deleteMenu()` | **SRS**: SRS-02-002 | **Spec**: §2-2

**商業規則**：
- 有子選單不可刪

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-021-01 | Error | 有子選單 | children exist | DELETE /menus/{id} | `MENU_HAS_CHILDREN` | errorCode | ✅ MenuServiceTest.deleteMenu_withChildren |
| TC-01-021-02 | Happy | 無子選單刪除 | no children | DELETE /menus/{id} | 200 | 已刪除 | ✅ MenuServiceTest.deleteMenu_noChildren |
| TC-01-021-03 | Error | 選單不存在 | invalid id | DELETE /menus/{id} | not found | errorCode | ✅ MenuServiceTest.deleteMenu_notFound |

---

## 4. 認證與密碼 (FN-01-022 ~ FN-01-033)

> **跨模組參照**：FN-01-022~028 / 036 與 TS-00 (FN-00-001~013) 高度重疊。
> 以下僅列 SA-01 獨有的 TC；共用 TC 請參照 TS-00。

### FN-01-022 取得驗證碼

→ **參照 TS-00 FN-00-005** (TC-00-005-01/02)

---

### FN-01-023 帳密登入

→ **參照 TS-00 FN-00-001** (TC-00-001-01 ~ 10)

---

### FN-01-024 臺北通 QRCode 登入

**SA**: SA-01 §登入認證 | **SD**: SD-01 §3 | **API**: `POST /v1/public/auth/taipei-pass`  
**Service**: `AuthService.loginByTaipeiPass()` | **SRS**: SRS-02-003 | **Spec**: §2-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-024-01 | Happy | QRCode 登入成功 | 臺北通帳號已綁定 | taipei-pass token | 200, JWT | accessToken 非 null | ⬜ 待實作 |
| TC-01-024-02 | Error | 未綁定帳號 | 無對應用戶 | taipei-pass token | 404 | USER_NOT_FOUND | ⬜ 待實作 |

---

### FN-01-025 Taipeion 登入

**SA**: SA-01 §登入認證 | **SD**: SD-01 §3 | **API**: `POST /v1/public/auth/taipeion`  
**Service**: `AuthService.loginByTaipeion()` | **SRS**: SRS-02-003 | **Spec**: §2-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-025-01 | Happy | Taipeion 登入成功 | 帳號已綁定 | taipeion token | 200, JWT | accessToken 非 null | ⬜ 待實作 |
| TC-01-025-02 | Error | 未綁定帳號 | 無對應用戶 | taipeion token | 404 | USER_NOT_FOUND | ⬜ 待實作 |

---

### FN-01-026 Token 刷新

→ **參照 TS-00 FN-00-003** (TC-00-003-01 ~ 03)

---

### FN-01-027 登出

→ **參照 TS-00 FN-00-004** (TC-00-004-01/02)

---

### FN-01-028 密碼強度驗證

→ **參照 TS-00 FN-00-006** (TC-00-006-01 ~ 03)

---

### FN-01-029 變更密碼

**SA**: SA-01 §密碼管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/password`  
**Service**: `UserSelfService.changePassword()` | **SRS**: SRS-02-004 | **Spec**: §2-4

**商業規則**：
- 密碼需符合強度規則（≥8 字元，含大小寫+數字+特殊字元）
- 近 N 次使用過的密碼不可重複

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-029-01 | Happy | 正常變更 | 已登入 | oldPwd + newPwd (合規) | 200 | 密碼已更新 | ✅ UserSelfServiceTest.changePassword_success |
| TC-01-029-02 | Error | 格式不合 | — | weak password | `RESET_PASSWORD_ERROR` | errorCode | ✅ UserSelfServiceTest.changePassword_invalidFormat |
| TC-01-029-03 | Error | 近期使用 | newPwd 在密碼歷程 | 重複密碼 | `PASSWORD_RECENTLY_USED` | errorCode | ✅ UserSelfServiceTest.changePassword_recentlyUsed |

---

### FN-01-030 定期換密碼提醒

**SA**: SA-01 §密碼管理 | **SD**: SD-01 §3 | **API**: (登入時觸發)  
**Service**: `AuthService.login()` 內部檢查 | **SRS**: SRS-02-004 | **Spec**: §2-4

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-030-01 | Happy | 密碼逾期提醒 | lastPasswordChange > 90d | login | 200, passwordExpired=true | flag 值 | ⬜ 待實作 |

---

### FN-01-031 申請密碼重設

**SA**: SA-01 §密碼管理 | **SD**: SD-01 §3 | **API**: `POST /v1/public/auth/forgot-password`  
**Service**: `AuthService.forgotPassword()` | **SRS**: SRS-02-005 | **Spec**: §2-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-031-01 | Happy | 寄出重設信 | email 存在 | email | 200, 信件已寄 | 無例外 | ⬜ 待實作 |
| TC-01-031-02 | Edge | Email 不存在 | 無此用戶 | unknown email | 200 (不洩漏) | 不拋錯 | ⬜ 待實作 |

---

### FN-01-032 驗證重設 Token

**SA**: SA-01 §密碼管理 | **SD**: SD-01 §3 | **API**: `GET /v1/public/auth/reset-password/verify`  
**Service**: `AuthService.verifyResetToken()` | **SRS**: SRS-02-005 | **Spec**: §2-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-032-01 | Happy | Token 有效 | valid reset token | token param | 200 | valid=true | ⬜ 待實作 |
| TC-01-032-02 | Error | Token 過期 | expired token | token param | 400 | errorCode | ⬜ 待實作 |

---

### FN-01-033 執行密碼重設

**SA**: SA-01 §密碼管理 | **SD**: SD-01 §3 | **API**: `POST /v1/public/auth/reset-password`  
**Service**: `AuthService.resetPasswordByToken()` | **SRS**: SRS-02-005 | **Spec**: §2-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-033-01 | Happy | 重設成功 | valid token | token + strong password | 200 | 密碼已更新 | ⬜ 待實作 |
| TC-01-033-02 | Error | 密碼太弱 | valid token | weak password | 400, `RESET_PASSWORD_ERROR` | errorCode | ⬜ 待實作 |

---

## 5. 閒置與會話 (FN-01-034 ~ FN-01-035)

### FN-01-034 閒置偵測

**SA**: SA-01 §閒置偵測 | **SD**: SD-01 §4 | **API**: (前端邏輯)  
**SRS**: SRS-02-006 | **Spec**: §2-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-034-01 | Happy | 閒置超時登出 | idle > timeout 設定 | 無操作 | 自動登出 + 導向登入頁 | 前端行為 | ⬜ 前端測試 |

---

### FN-01-035 閒置時間設定

**SA**: SA-01 §閒置偵測 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/settings/idle-timeout`  
**Service**: `SystemSettingService.getIdleTimeout()` / `updateIdleTimeout()` | **SRS**: SRS-02-006 | **Spec**: §2-6

**商業規則**：
- 預設 30 分鐘
- 僅 ADMIN 可修改
- 不存在 → 回傳預設值

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-035-01 | Happy | 查詢閒置時間 | setting 存在 | GET /settings/idle-timeout | 200, value | ✅ SystemSettingControllerTest.getIdleTimeout + SystemSettingServiceTest.getIdleTimeout_found |
| TC-01-035-02 | Happy | 預設值 | setting 不存在 | GET /settings/idle-timeout | 200, default=30 | 預設值 | ✅ SystemSettingServiceTest.getIdleTimeout_notFound_default |
| TC-01-035-03 | Happy | 更新閒置時間 | ADMIN | PUT /settings/idle-timeout | 200 | 值更新 | ✅ SystemSettingControllerTest.updateIdleTimeout_admin + SystemSettingServiceTest.updateIdleTimeout_found |
| TC-01-035-04 | Edge | 更新不存在 key | key 不在 DB | PUT /settings/idle-timeout | 建立新紀錄 | upsert | ✅ SystemSettingServiceTest.updateIdleTimeout_notFound |
| TC-01-035-05 | Error | 無權限更新 | VIEWER | PUT /settings/idle-timeout | 403 | HTTP 403 | ✅ SystemSettingControllerTest.updateIdleTimeout_viewer |
| TC-01-035-06 | Error | 未登入 | no token | GET or PUT idle-timeout | 401 | HTTP 401 | ✅ SystemSettingControllerTest.getIdleTimeout_noToken + updateIdleTimeout_noToken |

---

## 6. 稽核紀錄 (FN-01-036 ~ FN-01-037)

### FN-01-036 登入登出紀錄查詢

→ **參照 TS-00 FN-00-010 ~ FN-00-013** (審計日誌相關 TC)

---

### FN-01-037 登入登出紀錄匯出

**SA**: SA-01 §稽核紀錄 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/audit/export`  
**Service**: `AuditService.export()` | **SRS**: SRS-02-007 | **Spec**: §2-7

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-037-01 | Happy | 匯出紀錄 | perm=AUDIT_VIEW | GET /audit/export | 200, Excel file | Content-Type | ⬜ 待補 |

---

## 7. 公告管理 (FN-01-038 ~ FN-01-044)

> **實作狀態**：已實作（V29 migration, AnnouncementController 8 endpoints）但**無測試**。

### FN-01-038 公告列表查詢

**SA**: SA-01 §公告管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/announcements`  
**Service**: `AnnouncementService.list()` | **SRS**: SRS-02-009 | **Spec**: §2-9

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-038-01 | Happy | 查詢列表 | perm=ANNOUNCEMENT_MANAGE | GET /announcements | 200, list | 含 DRAFT + PUBLISHED | ⬜ 待補 |
| TC-01-038-02 | Error | 未登入 | no token | GET /announcements | 401 | HTTP 401 | ⬜ 待補 |

---

### FN-01-039 新增公告

**SA**: SA-01 §公告管理 | **SD**: SD-01 §3 | **API**: `POST /v1/auth/announcements`  
**Service**: `AnnouncementService.create()` | **SRS**: SRS-02-009 | **Spec**: §2-9

**商業規則**：
- 新增為 DRAFT 狀態
- scope: ALL (全機關) 或 DEPT (指定部門)

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-039-01 | Happy | 新增草稿（全機關） | perm=ANNOUNCEMENT_MANAGE | scope=ALL | 201, status=DRAFT | status 欄位 | ⬜ 待補 |
| TC-01-039-02 | Happy | 新增草稿（指定部門） | perm=ANNOUNCEMENT_MANAGE | scope=DEPT, deptIds | 201, 關聯 announcement_depts | deptIds 正確 | ⬜ 待補 |
| TC-01-039-03 | Error | 無權限 | 無 ANNOUNCEMENT_MANAGE | POST /announcements | 403 | HTTP 403 | ⬜ 待補 |

---

### FN-01-040 編輯公告

**SA**: SA-01 §公告管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/announcements/{id}`  
**Service**: `AnnouncementService.update()` | **SRS**: SRS-02-009 | **Spec**: §2-9

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-040-01 | Happy | 編輯草稿 | status=DRAFT | PUT /announcements/{id} | 200 | 欄位更新 | ⬜ 待補 |
| TC-01-040-02 | Error | 公告不存在 | invalid id | PUT /announcements/{id} | ANNOUNCEMENT_NOT_FOUND (50001) | errorCode | ⬜ 待補 |

---

### FN-01-041 發佈公告

**SA**: SA-01 §公告管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/announcements/{id}/publish`  
**Service**: `AnnouncementService.publish()` | **SRS**: SRS-02-009 | **Spec**: §2-9

**商業規則**：
- 只有 DRAFT → PUBLISHED

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-041-01 | Happy | 發佈草稿 | status=DRAFT | PUT /announcements/{id}/publish | 200, PUBLISHED | status 更新 | ⬜ 待補 |
| TC-01-041-02 | Error | 重複發佈 | status=PUBLISHED | PUT /announcements/{id}/publish | error | 狀態檢查 | ⬜ 待補 |

---

### FN-01-042 刪除公告

**SA**: SA-01 §公告管理 | **SD**: SD-01 §3 | **API**: `DELETE /v1/auth/announcements/{id}`  
**Service**: `AnnouncementService.delete()` | **SRS**: SRS-02-009 | **Spec**: §2-9

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-042-01 | Happy | 刪除公告 | perm=ANNOUNCEMENT_MANAGE | DELETE /announcements/{id} | 200 | 已刪除 | ⬜ 待補 |
| TC-01-042-02 | Error | 不存在 | invalid id | DELETE /announcements/{id} | ANNOUNCEMENT_NOT_FOUND | errorCode | ⬜ 待補 |

---

### FN-01-043 公告欄（使用者端）

**SA**: SA-01 §公告管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/announcements/board`  
**Service**: `AnnouncementService.getBoard()` | **SRS**: SRS-02-009 | **Spec**: §2-9

**商業規則**：
- 只回傳 PUBLISHED 且在 scope 內的公告
- 顯示已讀/未讀狀態

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-043-01 | Happy | 查看公告欄 | authenticated | GET /announcements/board | 200, PUBLISHED only | 不含 DRAFT | ⬜ 待補 |

---

### FN-01-044 標記公告已讀

**SA**: SA-01 §公告管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/announcements/{id}/read`  
**Service**: `AnnouncementReadService.markRead()` | **SRS**: SRS-02-009 | **Spec**: §2-9

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-044-01 | Happy | 標記已讀 | 未讀公告 | PUT /announcements/{id}/read | 200 | announcement_reads 紀錄 | ⬜ 待補 |
| TC-01-044-02 | Edge | 冪等重複標記 | 已讀 | PUT /announcements/{id}/read | 200 (冪等) | 不重複建立 | ⬜ 待補 |

---

## 8. 通知中心 (FN-01-045 ~ FN-01-049)

> **實作狀態**：尚未實作。

### FN-01-045 通知列表查詢

**SA**: SA-01 §通知中心 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/notifications`  
**Service**: `NotificationService.list()` | **SRS**: SRS-02-010 | **Spec**: §2-10

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-045-01 | Happy | 查詢通知 | authenticated | GET /notifications | 200, list | 分頁 + 排序 | ⬜ 待實作 |

---

### FN-01-046 未讀通知數量

**SA**: SA-01 §通知中心 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/notifications/unread-count`  
**Service**: `NotificationService.unreadCount()` | **SRS**: SRS-02-010 | **Spec**: §2-10

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-046-01 | Happy | 未讀數量 | 3 unread | GET /notifications/unread-count | 200, count=3 | count 值 | ⬜ 待實作 |

---

### FN-01-047 標記通知已讀

**SA**: SA-01 §通知中心 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/notifications/{id}/read`  
**Service**: `NotificationService.markRead()` | **SRS**: SRS-02-010 | **Spec**: §2-10

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-047-01 | Happy | 標記已讀 | unread notification | PUT /notifications/{id}/read | 200, read=true | read 欄位 | ⬜ 待實作 |

---

### FN-01-048 待辦案件列表

**SA**: SA-01 §通知中心 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/notifications/todos`  
**Service**: `NotificationService.getTodos()` | **SRS**: SRS-02-010 | **Spec**: §2-10

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-048-01 | Happy | 待辦列表 | 有待辦案件 | GET /notifications/todos | 200, list | 含 workflow pending | ⬜ 待實作 |

---

### FN-01-049 即時通知推送

**SA**: SA-01 §通知中心 | **SD**: SD-01 §4 | **API**: `WebSocket /ws/notifications`  
**Service**: WebSocket handler | **SRS**: SRS-02-010 | **Spec**: §2-10

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-049-01 | Happy | 即時推送 | WebSocket connected | 系統事件觸發 | 收到通知 message | message payload | ⬜ 待實作 |

---

## 9. 部門管理 (FN-01-050 ~ FN-01-053)

### FN-01-050 部門樹查詢

**SA**: SA-01 §部門管理 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/admin/depts/tree`  
**Service**: `DeptService.getDeptTree()` / `getDeptOptions()` / `getDeptById()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 樹狀結構回傳（parent-children）
- 支援 options 模式（id + name only）

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-050-01 | Happy | 部門樹 | ADMIN, depts exist | GET /depts/tree | 200, tree structure | children 嵌套 | ✅ DeptControllerTest.getDeptTree + DeptServiceTest.getDeptTree |
| TC-01-050-02 | Edge | 空部門樹 | no depts | GET /depts/tree | 200, [] | 空陣列 | ✅ DeptServiceTest.getDeptTree_empty |
| TC-01-050-03 | Happy | 部門選項 | ADMIN | GET /depts/options | 200, flat list | id+name only | ✅ DeptControllerTest.getDeptOptions + DeptServiceTest.getDeptOptions |
| TC-01-050-04 | Happy | 查詢單一部門 | dept exists | GET /depts/{id} | 200, dept detail | 含 hierarchyPath | ✅ DeptControllerTest.getDeptById + DeptServiceTest.getDeptById |
| TC-01-050-05 | Error | 部門不存在 | invalid id | GET /depts/{id} | 404 | HTTP 404 | ✅ DeptControllerTest.getDeptById_notFound + DeptServiceTest.getDeptById_notFound |
| TC-01-050-06 | Error | 未登入 | no token | GET /depts/tree | 401 | HTTP 401 | ✅ DeptControllerTest.getDeptTree_noToken |

---

### FN-01-051 新增部門

**SA**: SA-01 §部門管理 | **SD**: SD-01 §3 | **API**: `POST /v1/auth/admin/depts`  
**Service**: `DeptService.createDept()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 同級部門名稱不可重複
- 子部門自動計算 hierarchyPath

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-051-01 | Happy | 新增根部門 | ADMIN | POST /depts (parentId=null) | 200, root dept | hierarchyPath=/{id}/ | ✅ DeptControllerTest.createDept_admin + DeptServiceTest.createDept_root |
| TC-01-051-02 | Happy | 新增子部門 | parent exists | POST /depts (parentId=1) | 200, child dept | hierarchyPath 含 parent | ✅ DeptServiceTest.createDept_child_hierarchyPath |
| TC-01-051-03 | Error | 名稱重複 | name 已存在於同級 | POST /depts | DEPT_NAME_DUPLICATE | errorCode | ✅ DeptServiceTest.createDept_duplicateName |
| TC-01-051-04 | Error | 無權限 | VIEWER | POST /depts | 403 | HTTP 403 | ✅ DeptControllerTest.createDept_viewer |

---

### FN-01-052 編輯部門

**SA**: SA-01 §部門管理 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/depts/{id}`  
**Service**: `DeptService.updateDept()` | **SRS**: SRS-02-001 | **Spec**: §2-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-052-01 | Happy | 更新部門 | dept exists | PUT /depts/{id} | 200 | name 更新 | ✅ DeptControllerTest.updateDept + DeptServiceTest.updateDept |
| TC-01-052-02 | Error | 名稱重複 | name taken in same level | PUT /depts/{id} | DEPT_NAME_DUPLICATE | errorCode | ✅ DeptServiceTest.updateDept_duplicateName |

---

### FN-01-053 刪除部門

**SA**: SA-01 §部門管理 | **SD**: SD-01 §3 | **API**: `DELETE /v1/auth/admin/depts/{id}`  
**Service**: `DeptService.deleteDept()` | **SRS**: SRS-02-001 | **Spec**: §2-1

**商業規則**：
- 有子部門不可刪
- 有關聯用戶不可刪

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-053-01 | Happy | 刪除空部門 | no children, no users | DELETE /depts/{id} | 200 | 已刪除 | ✅ DeptControllerTest.deleteDept + DeptServiceTest.deleteDept_noChildrenNoUsers |
| TC-01-053-02 | Error | 有子部門 | children exist | DELETE /depts/{id} | error | 不可刪 | ✅ DeptServiceTest.deleteDept_hasChildren |
| TC-01-053-03 | Error | 有關聯用戶 | users in dept | DELETE /depts/{id} | error | 不可刪 | ✅ DeptServiceTest.deleteDept_hasUsers |
| TC-01-053-04 | Error | 未登入 | no token | DELETE /depts/{id} | 401 | HTTP 401 | ✅ DeptControllerTest.deleteDept_noToken |

---

## 10. 系統參數 (FN-01-054 ~ FN-01-055)

### FN-01-054 查詢系統參數

**SA**: SA-01 §系統參數 | **SD**: SD-01 §3 | **API**: `GET /v1/auth/admin/settings`  
**Service**: `SystemSettingService.findAllSettings()` | **SRS**: SRS-02-006 | **Spec**: §2-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-054-01 | Happy | 管理員查詢 | ADMIN | GET /settings | 200, list | 含所有設定 | ✅ SystemSettingControllerTest.listSettings_admin + SystemSettingServiceTest.findAllSettings |
| TC-01-054-02 | Error | 無權限 | VIEWER | GET /settings | 403 | HTTP 403 | ✅ SystemSettingControllerTest.listSettings_viewer |

---

### FN-01-055 更新系統參數

**SA**: SA-01 §系統參數 | **SD**: SD-01 §3 | **API**: `PUT /v1/auth/admin/settings/{key}`  
**Service**: `SystemSettingService.updateSetting()` | **SRS**: SRS-02-006 | **Spec**: §2-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-055-01 | Happy | 更新參數 | ADMIN, key exists | PUT /settings/{key} | 200 | 值更新 | ✅ SystemSettingControllerTest.updateSetting_admin + SystemSettingServiceTest.updateSetting_found |
| TC-01-055-02 | Error | 無權限 | VIEWER | PUT /settings/{key} | 403 | HTTP 403 | ✅ SystemSettingControllerTest.updateSetting_viewer |
| TC-01-055-03 | Error | 參數不存在 | invalid key | PUT /settings/{key} | not found | errorCode | ✅ SystemSettingServiceTest.updateSetting_notFound |

---

## 11. Data Scope 引擎 (跨切面)

> **跨功能**：Data Scope 機制支撐 FN-01-001（帳號列表）及所有需 Data Permission 的查詢。

### DataScopeHelper

**SA**: SA-01 §Data Scope | **SD**: SD-01 §2 | **API**: (內部)  
**Service**: `DataScopeHelper` | **SRS**: SRS-02-001

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-DS-01 | Happy | ALL scope → 全部 deptId | scope=ALL | getVisibleDeptIds | 所有 deptId | list 非空 | ✅ DataScopeHelperTest.getVisibleDeptIds_allScope |
| TC-01-DS-02 | Happy | THIS_LEVEL → 本部門 | scope=THIS_LEVEL | getVisibleDeptIds | 僅自身 deptId | size==1 | ✅ DataScopeHelperTest.thisLevel |
| TC-01-DS-03 | Happy | THIS_LEVEL_AND_BELOW | scope=THIS_LEVEL_AND_BELOW | getVisibleDeptIds | 本+下級 | hierarchy | ✅ DataScopeHelperTest.thisLevelAndBelow |
| TC-01-DS-04 | Edge | null deptId | user.deptId=null | getVisibleDeptIds | [] | empty | ✅ DataScopeHelperTest.nullDeptId |
| TC-01-DS-05 | Edge | null user | userInfo=null | getVisibleDeptIds | [] | empty | ✅ DataScopeHelperTest.nullUser |
| TC-01-DS-06 | Happy | isDeptInScope — ALL | scope=ALL, any dept | isDeptInScope | true | 永遠 true | ✅ DataScopeHelperTest.isDeptInScope_allScope |

### DataPermissionAspect

**SA**: SA-01 §Data Scope | **SD**: SD-01 §2 | **API**: (AOP)  
**Service**: `DataPermissionAspect` | **SRS**: SRS-02-001

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-01-DP-01 | Happy | ALL → 不加過濾 | scope=ALL | AOP 攔截 | 無 SQL WHERE 過濾 | filter 未套用 | ✅ DataPermissionAspectTest.enforce_ALL_noFilter |
| TC-01-DP-02 | Happy | THIS_LEVEL → 精確過濾 | scope=THIS_LEVEL | AOP 攔截 | WHERE dept_id = ? | 過濾條件 | ✅ DataPermissionAspectTest.THIS_LEVEL_exactFilter |
| TC-01-DP-03 | Happy | BELOW → hierarchy prefix | scope=THIS_LEVEL_AND_BELOW | AOP 攔截 | WHERE hierarchy_path LIKE '/{id}/%' | prefix 查詢 | ✅ DataPermissionAspectTest.THIS_LEVEL_AND_BELOW_hierarchyPrefix |
| TC-01-DP-04 | Edge | finally 清理 context | 任何 scope | AOP 結束 | ThreadLocal 清除 | context cleared | ✅ DataPermissionAspectTest.clearContextInFinally |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ✅ 已有自動化 | 76 |
| ⬜ 待補（已實作 FN） | 18 |
| ⬜ 待實作（未實作 FN） | 21 |
| ↗ 參照 TS-00 | 6 FN |
| **總 TC 數** | **115** |

### 待補測試優先序

| 優先 | FN | 待建 TC | 原因 |
|------|-----|--------|------|
| 1 | FN-01-038~044 | 15 TC | 公告模組已完整實作，完全無測試 |
| 2 | FN-01-005/006/008 | 5 TC | 帳號管理核心功能已實作，缺少邊界測試 |
| 3 | FN-01-037 | 1 TC | 稽核匯出已實作 |
| 4 | FN-01-024/025 | 4 TC | 外部登入（臺北通/Taipeion）需整合測試 |
| 5 | FN-01-030~033 | 6 TC | 密碼重設流程 |
| 6 | FN-01-045~049 | 5 TC | 通知中心（Phase 3+） |
