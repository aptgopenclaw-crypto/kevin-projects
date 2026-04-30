# 15. Tenant 多租戶模組

## 1. 模組概述

`com.taipei.iot.tenant` 實作系統的多租戶（Multi-Tenancy）架構，採用**共享資料庫、共享 Schema、行級隔離**策略。所有租戶的資料存在同一張表中，透過 `tenant_id` 欄位區分，並利用 Hibernate `@Filter` 自動注入 WHERE 條件實現透明的資料隔離。

核心元件：
- **TenantContext**：ThreadLocal 容器，存放當前執行緒的租戶 ID
- **TenantInterceptor**：MVC 攔截器，設定/清除 TenantContext
- **TenantFilterAspect**：AOP Aspect，在 Repository 方法前啟用 Hibernate Filter
- **TenantEntityListener**：JPA Entity Listener，寫入防護（PrePersist/PreUpdate/PreRemove）
- **TenantAware**：標記介面，標示需要租戶過濾的 Entity
- **TenantScopedRepository**：標記介面，標示需要租戶過濾的 Repository
- **TenantEntity**：租戶主檔實體
- **TenantProperties**：組態屬性（single/multi 模式）

## 2. 資料表結構

### tenant 表

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| `tenant_id` | `VARCHAR(50)` | PK | 租戶 ID |
| `tenant_code` | `VARCHAR(50)` | UNIQUE, NOT NULL | 租戶代碼 |
| `tenant_name` | `VARCHAR(200)` | NOT NULL | 租戶名稱 |
| `deployment_mode` | `VARCHAR(20)` | NOT NULL | 部署模式 |
| `config` | `JSONB` | | 租戶組態（JSON） |
| `enabled` | `BOOLEAN` | NOT NULL | 是否啟用 |
| `create_time` | `TIMESTAMP` | NOT NULL | 建立時間 |
| `update_time` | `TIMESTAMP` | | 更新時間 |

## 3. 元件關聯/架構

```
┌─────────────────────────────────────────────────┐
│                 HTTP 請求層                       │
│  JwtAuthenticationFilter → TenantInterceptor     │
│  (multi 模式: JWT 帶 tenantId)  (single 模式: 固定) │
│              ↓ 設定 TenantContext                  │
├─────────────────────────────────────────────────┤
│                 AOP 層                            │
│  TenantFilterAspect                              │
│  → 攔截所有 Repository 方法                        │
│  → 只處理 TenantScopedRepository 實作者            │
│  → 啟用 Hibernate tenantFilter                    │
├─────────────────────────────────────────────────┤
│                 JPA 層                            │
│  TenantEntityListener                            │
│  → @PrePersist: 自動填入 tenantId                 │
│  → @PreUpdate:  驗證不跨租戶修改                    │
│  → @PreRemove:  驗證不跨租戶刪除                    │
├─────────────────────────────────────────────────┤
│                 Hibernate Filter                  │
│  @FilterDef("tenantFilter", tenantId=String)     │
│  @Filter(condition = "tenant_id = :tenantId")    │
└─────────────────────────────────────────────────┘
```

### 介面實作關係
- **TenantAware**：Entity 實作 → `DeptInfoEntity`, `Announcement`, `SystemSettingEntity` 等
- **TenantScopedRepository**：Repository 實作 → `DeptInfoRepository`, `AnnouncementRepository` 等
- 全域 Entity（`UserEntity`, `TenantEntity`）的 Repository 不實作 `TenantScopedRepository`

## 4. API 端點

本模組無直接 API 端點。`TenantRepository` 提供內部查詢：

| 方法 | 說明 |
|------|------|
| `findByTenantCode(String)` | 依代碼查詢租戶 |
| `findByEnabledTrue()` | 查詢所有啟用的租戶 |

## 5. 業務邏輯/機制說明

### 5.1 兩種運行模式

| 模式 | 組態 | 行為 |
|------|------|------|
| `single` | `tenant.mode=single` | TenantInterceptor 強制覆寫為 `tenant.default-id`（預設 `DEFAULT`） |
| `multi` | `tenant.mode=multi` | 由 JwtAuthenticationFilter 從 JWT 取得 tenantId 設定到 TenantContext |

### 5.2 讀取保護（TenantFilterAspect）
- **攔截點**：`execution(* com.taipei.iot..*.repository..*Repository.*(..))`
- **判斷邏輯**：
  1. 非 `TenantScopedRepository` → 直接放行
  2. `TenantContext.isSystemContext()` → 跳過（排程任務允許跨租戶）
  3. `TenantContext.getCurrentTenantId() == null` → 拋出 `IllegalStateException`（**Fail-closed**）
  4. 正常情況 → 啟用 Hibernate Filter，注入 `tenantId` 參數

### 5.3 寫入保護（TenantEntityListener）

**為什麼需要寫入保護？**  
Hibernate `@Filter` 只保護 SELECT。攻擊者若猜到其他租戶的 Entity ID，可透過 `save()` / `delete()` 進行跨租戶操作。

| 事件 | 行為 |
|------|------|
| `@PrePersist` | 自動填入 `tenantId`（若為 null 且非 SYSTEM context） |
| `@PreUpdate` | 驗證 Entity.tenantId == TenantContext.tenantId，不符拋 `SecurityException` |
| `@PreRemove` | 同 PreUpdate |

**跳過條件**：
- System Context（排程、auth 流程）
- TenantContext 未設定（非 HTTP 路徑，如啟動初始化）

### 5.4 系統上下文（System Context）
- `TenantContext.setSystemContext()` 設定 `SYSTEM` 標記
- 排程任務、非同步任務使用，允許跨租戶操作
- `SYSTEM` 標記不會被帶入 Entity 的 tenantId（PrePersist 判斷）

### 5.5 生命週期管理
- `TenantInterceptor.afterCompletion()` 清除 `TenantContext`，防止 ThreadLocal 洩漏

## 6. 資料流

### HTTP 請求的租戶上下文流程
```
HTTP Request (帶 JWT)
  → JwtAuthenticationFilter
    → 解析 JWT 中的 tenantId
    → TenantContext.setCurrentTenantId(tenantId)  [multi 模式]
  → TenantInterceptor.preHandle()
    → single 模式: 覆寫為 defaultId
    → multi 模式: 不動作（使用 JWT 設定的值）
  → Service → Repository
    → TenantFilterAspect.enableTenantFilter()
      → entityManager.unwrap(Session)
      → session.enableFilter("tenantFilter").setParameter("tenantId", ...)
    → Hibernate 自動加 WHERE tenant_id = ?
  → TenantInterceptor.afterCompletion()
    → TenantContext.clear()
```

### 寫入操作的防護流程
```
Service.save(entity)
  → TenantEntityListener.prePersist()
    → entity.tenantId == null? → 自動填入 TenantContext.tenantId
  
Service.update(entity)
  → TenantEntityListener.preUpdate()
    → entity.tenantId != contextTenantId?
      → 拋出 SecurityException + 記錄 SECURITY log

Service.delete(entity)
  → TenantEntityListener.preRemove()
    → 同 preUpdate 驗證邏輯
```

## 7. ErrorCode / Enum 定義

### 租戶相關 ErrorCode（定義在 common.ErrorCode）

| 代碼 | HTTP | 說明 |
|------|------|------|
| `10020` | 404 | TENANT_NOT_FOUND - 場域不存在 |
| `10021` | 403 | TENANT_ACCESS_DENIED - 無此場域存取權限 |
| `10022` | 403 | TENANT_SELECTION_REQUIRED - 需先選擇場域 |

### TenantProperties 組態

| 屬性 | 預設值 | 說明 |
|------|--------|------|
| `tenant.mode` | `single` | `single` = 固定單一租戶 / `multi` = 多租戶 |
| `tenant.default-id` | `DEFAULT` | single 模式下的固定 tenant ID |
