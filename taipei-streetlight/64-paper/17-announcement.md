# 17. Announcement 公告模組

## 1. 模組概述

`com.taipei.iot.announcement` 提供系統公告管理功能，支援公告的建立、編輯、刪除、發佈排程、過期控制、部門定向投放及已讀追蹤。公告具備租戶隔離，並根據使用者的 DataScope 控制管理權限。

核心元件：
- **Announcement**：公告主體實體（租戶隔離）
- **AnnouncementDept**：公告-部門關聯表（scope=DEPT 時的目標部門）
- **AnnouncementRead**：已讀紀錄
- **AnnouncementService**：公告 CRUD + 查詢邏輯
- **AnnouncementReadService**：已讀/未讀管理
- **AnnouncementController**：REST API

## 2. 資料表結構

### announcements 表

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 公告 ID |
| `tenant_id` | `VARCHAR(50)` | NOT NULL | 租戶 ID |
| `title` | `VARCHAR(200)` | NOT NULL | 標題 |
| `content` | `TEXT` | NOT NULL | 內容 |
| `status` | `VARCHAR(20)` | NOT NULL | 狀態（DRAFT / PUBLISHED） |
| `scope` | `VARCHAR(20)` | NOT NULL | 受眾範圍（ALL / DEPT） |
| `pinned` | `BOOLEAN` | NOT NULL | 是否置頂 |
| `publish_at` | `TIMESTAMP` | | 發佈時間 |
| `expire_at` | `TIMESTAMP` | | 過期時間（NULL=永不過期） |
| `created_by` | `VARCHAR(50)` | | 建立者 userId |
| `created_by_name` | `VARCHAR(100)` | | 建立者顯示名稱 |
| `created_at` | `TIMESTAMP` | NOT NULL | 建立時間 |
| `updated_at` | `TIMESTAMP` | | 更新時間 |

### announcement_depts 表（關聯表）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| `announcement_id` | `BIGINT` | PK（複合） | 公告 ID |
| `dept_id` | `BIGINT` | PK（複合） | 目標部門 ID |

### announcement_reads 表

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `announcement_id` | `BIGINT` | NOT NULL, UNIQUE(announcement_id, user_id) | 公告 ID |
| `user_id` | `VARCHAR(50)` | NOT NULL | 使用者 ID |
| `read_at` | `TIMESTAMP` | NOT NULL | 已讀時間 |

## 3. 元件關聯/架構

```
AnnouncementController
  ├── AnnouncementService
  │     ├── AnnouncementRepository (TenantScopedRepository)
  │     ├── AnnouncementDeptRepository
  │     ├── AnnouncementReadRepository
  │     ├── DeptInfoRepository (解析部門名稱)
  │     ├── UserRepository (解析使用者顯示名稱)
  │     └── SecurityContextUtils / DataScopeEnum
  └── AnnouncementReadService
        ├── AnnouncementRepository
        ├── AnnouncementReadRepository
        └── SecurityContextUtils / TenantContext
```

## 4. API 端點

基礎路徑：`/v1/auth/announcements`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| `GET` | `/` | authenticated | 查詢公告（`admin=true` 切換管理模式） |
| `GET` | `/{id}` | authenticated | 取得單筆公告詳情 |
| `GET` | `/unread-count` | authenticated | 取得未讀公告數量 |
| `POST` | `/{id}/read` | authenticated | 標記單則公告為已讀 |
| `POST` | `/read-all` | authenticated | 全部標為已讀 |
| `POST` | `/` | ADMIN / SUPER_ADMIN / DEPT_ADMIN / ANNOUNCEMENT_MANAGE | 新增公告 |
| `PUT` | `/{id}` | 同上 | 編輯公告 |
| `DELETE` | `/{id}` | 同上 | 刪除公告 |

### 查詢參數（GET /）

| 參數 | 預設 | 說明 |
|------|------|------|
| `admin` | `false` | 是否為管理模式 |
| `statusFilter` | `ALL` | 狀態篩選（ALL / DRAFT / PUBLISHED / EXPIRED） |
| `keyword` | | 標題關鍵字搜尋 |
| `page` | `0` | 頁碼 |
| `size` | `10` | 每頁筆數 |

## 5. 業務邏輯/機制說明

### 5.1 公告可見性規則（前台）
一則公告對使用者可見需同時滿足：
1. `status = PUBLISHED`
2. `publish_at <= now()`
3. `expire_at IS NULL OR expire_at > now()`
4. `scope = ALL` **或** 使用者部門在 `announcement_depts` 中

### 5.2 管理頁面查詢（依 DataScope）
- **ADMIN（DataScope=ALL）**：看全部公告
- **DEPT_ADMIN（DataScope≠ALL）**：看自己建立的 + 受眾包含自己部門的

### 5.3 新增公告
- DEPT_ADMIN 強制 `scope=DEPT`、`targetDeptIds=[自己部門]`
- ADMIN 可選 scope=ALL 或 scope=DEPT（DEPT 時必須選至少一個部門）
- `publishAt` 為 null 時預設為 `now()`
- 建立者的 displayName 從 UserEntity 查詢並存入

### 5.4 編輯/刪除權限
- ADMIN（DataScope=ALL）：可編輯/刪除任何公告
- DEPT_ADMIN：只能編輯/刪除**自己建立的**公告
- 編輯時重建 `announcement_depts` 關聯表

### 5.5 已讀機制
- **標記已讀**：`INSERT ... ON CONFLICT (announcement_id, user_id) DO NOTHING`（冪等操作）
- **全部標為已讀**：一條 SQL 將所有可見但未讀的公告批次 INSERT
- **未讀計數**：JPQL COUNT 查詢，排除已在 `announcement_reads` 中的公告

### 5.6 排序邏輯
預設排序：置頂優先 → 發佈時間降序
```java
Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("publishAt"))
```

### 5.7 批次載入優化
列表查詢時避免 N+1：
- 批次載入 `announcement_depts`（`findByAnnouncementIdIn`）
- 批次載入已讀狀態（`findByAnnouncementIdInAndUserId`）
- 批次解析部門名稱（`resolveDeptNameMap`）

## 6. 資料流

### 前台查詢流程
```
GET /v1/auth/announcements
  → AnnouncementService.listVisible()
    → SecurityContextUtils.getUserInfo() → deptId
    → AnnouncementRepository.findVisibleAnnouncements(deptId, now, pageable)
      → Hibernate tenantFilter 自動過濾 tenant_id
      → JPQL: status=PUBLISHED AND publishAt<=now AND !expired AND (scope=ALL OR dept match)
    → toResponseList(): 批次載入 depts + reads + deptNames
    → 標記 isRead 狀態
```

### 全部標為已讀流程
```
POST /v1/auth/announcements/read-all
  → AnnouncementReadService.markAllAsRead()
    → Native SQL:
        INSERT INTO announcement_reads
        SELECT a.id, :userId, now()
        FROM announcements a
        WHERE tenant_id = :tenantId
          AND status = 'PUBLISHED' AND publishAt <= now
          AND (expire_at IS NULL OR expire_at > now)
          AND (scope='ALL' OR dept match)
          AND NOT EXISTS (已讀紀錄)
        ON CONFLICT DO NOTHING
```

## 7. ErrorCode / Enum 定義

### 公告相關 ErrorCode

| 代碼 | HTTP | 常數名稱 | 說明 |
|------|------|---------|------|
| `50001` | 404 | `ANNOUNCEMENT_NOT_FOUND` | 公告不存在 |

### AnnouncementStatus 枚舉

| 值 | 說明 |
|----|------|
| `DRAFT` | 草稿 |
| `PUBLISHED` | 已發佈 |

### AnnouncementScope 枚舉

| 值 | 說明 |
|----|------|
| `ALL` | 全場域可見 |
| `DEPT` | 指定部門可見 |
