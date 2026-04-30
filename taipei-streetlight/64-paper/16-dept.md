# 16. Dept 部門模組

## 1. 模組概述

`com.taipei.iot.dept` 實作部門管理功能，採用**樹狀結構**（parent-child + hierarchy_path）管理組織架構，並提供**資料權限控制（DataPermission）** AOP 機制，根據使用者的 `dataScope` 限縮可存取的資料範圍。

核心元件：
- **DeptInfoEntity**：部門實體，支援樹狀結構及租戶隔離
- **DeptController / DeptService**：部門 CRUD 及樹狀查詢
- **@DataPermission + DataPermissionAspect**：資料權限 AOP 註解
- **DataScopeContext / DataScopeFilter**：ThreadLocal 資料範圍過濾條件
- **DataScopeHelper**：輔助計算當前使用者可見部門範圍
- **DataScopeEnum**：資料範圍枚舉（ALL / THIS_LEVEL / THIS_LEVEL_AND_BELOW）

## 2. 資料表結構

### dept_info 表

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| `dept_id` | `BIGINT` | PK, AUTO_INCREMENT | 部門 ID |
| `tenant_id` | `VARCHAR(50)` | NOT NULL | 租戶 ID |
| `pid` | `BIGINT` | | 父部門 ID（NULL 為根節點） |
| `dept_name` | `VARCHAR(100)` | NOT NULL | 部門名稱 |
| `dept_sort` | `INT` | 預設 0 | 排序權重 |
| `status` | `SMALLINT` | 預設 1 | 狀態（1=啟用） |
| `hierarchy_path` | `VARCHAR(500)` | | 階層路徑，如 `/1/3/7/` |
| `create_by` | `VARCHAR(50)` | | 建立者 |
| `update_by` | `VARCHAR(50)` | | 更新者 |
| `create_time` | `TIMESTAMP` | NOT NULL | 建立時間 |
| `update_time` | `TIMESTAMP` | | 更新時間 |

**hierarchy_path 說明**：  
根節點格式 `/1/`，子節點格式 `/1/3/`，孫節點 `/1/3/7/`。用於 `THIS_LEVEL_AND_BELOW` 的 `LIKE 'prefix%'` 快速查詢。

## 3. 元件關聯/架構

```
DeptController
  └── DeptService
        ├── DeptInfoRepository (TenantScopedRepository)
        ├── UserTenantMappingRepository (檢查部門下是否有使用者)
        └── DataScopeHelper
              └── DeptInfoRepository

@DataPermission (註解)
  └── DataPermissionAspect (AOP)
        ├── SecurityContextUtils.getUserInfo()
        ├── DataScopeEnum.fromString()
        ├── DeptInfoRepository (查詢 hierarchyPath)
        └── DataScopeContext.set(DataScopeFilter)
              └── 業務 Repository 讀取 DataScopeContext 加入查詢條件
```

## 4. API 端點

基礎路徑：`/v1/auth/dept`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| `GET` | `/list` | ADMIN / SUPER_ADMIN / DEPT_ADMIN / DEPT_LIST | 取得部門樹 |
| `GET` | `/options` | authenticated | 部門下拉選項（全部） |
| `GET` | `/scope-options` | authenticated | 依 DataScope 限縮的部門選項 |
| `GET` | `/{deptId}` | authenticated | 取得單一部門 |
| `POST` | `/` | SUPER_ADMIN / DEPT_CREATE | 新增部門 |
| `PUT` | `/` | SUPER_ADMIN / DEPT_UPDATE | 更新部門 |
| `DELETE` | `/{deptId}` | SUPER_ADMIN / DEPT_DELETE | 刪除部門 |

### 請求/回應 DTO

**CreateDeptRequest**：
| 欄位 | 必填 | 說明 |
|------|------|------|
| `deptName` | ✓ | 部門名稱 |
| `pid` | | 父部門 ID |
| `deptSort` | | 排序（預設 0） |

**UpdateDeptRequest**：
| 欄位 | 必填 | 說明 |
|------|------|------|
| `deptId` | ✓ | 部門 ID |
| `deptName` | | 部門名稱 |
| `deptSort` | | 排序 |
| `status` | | 狀態 |

## 5. 業務邏輯/機制說明

### 5.1 樹狀結構建構
- 從 DB 載入所有 status=1 的部門，依 `deptSort` 排序
- 以 `pid` 分組建構 parent-children Map
- `pid == null` 為根節點，遞迴組裝子節點
- `buildScopedOptionTree()`：限縮範圍時，父節點不在清單中的視為根節點

### 5.2 新增部門流程
1. 檢查同父級下部門名稱不重複
2. 驗證父部門存在（若 pid 非 null）
3. 建立 Entity 並 save（取得 auto-generated dept_id）
4. 計算 `hierarchy_path`：`parentPath + deptId + "/"`
5. 二次 save 更新 hierarchy_path

### 5.3 刪除部門檢查
1. 部門存在
2. 無子部門（`existsByPid`）
3. 無使用者（查詢 `user_tenant_mapping`）

### 5.4 資料權限機制（@DataPermission）

**使用方式**：在 Service 方法上標註 `@DataPermission`，AOP 自動注入資料過濾條件到 `DataScopeContext`。

**DataScopeEnum 三種範圍**：

| 範圍 | 行為 |
|------|------|
| `ALL` | 不加任何限制 |
| `THIS_LEVEL` | 只能存取 `deptId = 使用者部門ID` 的資料 |
| `THIS_LEVEL_AND_BELOW` | 使用 `hierarchyPath LIKE 'prefix%'` 查詢本部門及所有下級部門 |

**DataScopeFilter 類型**：

| FilterType | 用途 |
|------------|------|
| `NONE` | 不過濾 |
| `EXACT` | 精確匹配 `deptId = ?` |
| `HIERARCHY_PREFIX` | 前綴匹配 `hierarchyPath LIKE ?%` |

**嚴格模式（Fail-closed）**：
- 使用者資訊為 null → 注入 `deptId = -1`（不可能的值 → 空結果）
- deptId 為 null → 注入 `deptId = -1`
- hierarchyPath 查不到 → 注入 `deptId = -1`

### 5.5 DataScopeHelper
提供兩個便利方法：
- `getVisibleDeptIds()`：回傳可見部門 ID 清單（empty = 不限制）
- `isDeptInScope(targetDeptId)`：判斷目標部門是否在範圍內

## 6. 資料流

### 部門樹查詢流程
```
GET /v1/auth/dept/list
  → DeptController.getDeptTree()
    → DeptService.getDeptTree()
      → DeptInfoRepository.findAllByStatusOrderByDeptSortAsc(1)
        → TenantFilterAspect 自動加 tenant_id 過濾
      → buildTree(): pid 分組 → 遞迴組裝 → List<DeptDto>
```

### 資料權限過濾流程
```
@DataPermission 標註的 Service 方法被呼叫
  → DataPermissionAspect.enforce()
    → SecurityContextUtils.getUserInfo()
    → 判斷 DataScopeEnum
      → ALL: 不設定 filter
      → THIS_LEVEL: DataScopeContext.set(EXACT, deptId)
      → THIS_LEVEL_AND_BELOW:
          → 查詢使用者部門的 hierarchyPath
          → DataScopeContext.set(HIERARCHY_PREFIX, path)
    → pjp.proceed() (執行原方法)
      → Repository 查詢時讀取 DataScopeContext.get()
      → 加入 WHERE 條件
    → DataScopeContext.clear() (finally)
```

## 7. ErrorCode / Enum 定義

### 部門相關 ErrorCode

| 代碼 | HTTP | 常數名稱 | 說明 |
|------|------|---------|------|
| `40001` | 404 | `DEPT_NOT_FOUND` | 部門不存在 |
| `40002` | 400 | `DEPT_ALREADY_EXISTS` | 部門名稱已存在（同父級下） |
| `40003` | 400 | `DEPT_HAS_CHILDREN` | 部門下有子部門，無法刪除 |
| `40004` | 400 | `DEPT_HAS_USERS` | 部門下有使用者，無法刪除 |

### DataScopeEnum

| 值 | 說明 |
|----|------|
| `ALL` | 不限制，可看所有資料 |
| `THIS_LEVEL` | 僅限本部門 |
| `THIS_LEVEL_AND_BELOW` | 本部門及所有下級部門 |

`DataScopeEnum.fromString(null)` 及無效值均回傳 `ALL`（安全預設）。

### DataScopeFilter.FilterType

| 值 | 說明 |
|----|------|
| `NONE` | 不過濾 |
| `EXACT` | 精確匹配欄位值 |
| `HIERARCHY_PREFIX` | hierarchy_path 前綴匹配 |
