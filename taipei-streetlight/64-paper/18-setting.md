# 18. Setting 系統設定模組

## 1. 模組概述

`com.taipei.iot.setting` 提供租戶級的系統設定功能，採用 **key-value store** 模式儲存組態項目。每個租戶有獨立的設定值，透過 Hibernate tenantFilter 實現租戶隔離。

核心元件：
- **SystemSettingEntity**：設定實體（租戶隔離）
- **SystemSettingRepository**：資料存取（TenantScopedRepository）
- **SystemSettingService**：設定 CRUD + 特定設定的便利方法
- **SystemSettingController**：REST API
- **SettingKey**：預定義設定 key 枚舉（含預設值）

## 2. 資料表結構

### system_settings 表

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `tenant_id` | `VARCHAR(50)` | NOT NULL | 租戶 ID |
| `setting_key` | `VARCHAR(100)` | NOT NULL | 設定鍵 |
| `setting_value` | `VARCHAR(500)` | NOT NULL | 設定值 |
| `description` | `VARCHAR(500)` | | 說明文字 |
| `created_at` | `TIMESTAMP` | NOT NULL | 建立時間 |
| `updated_at` | `TIMESTAMP` | | 更新時間 |

## 3. 元件關聯/架構

```
SystemSettingController
  └── SystemSettingService
        └── SystemSettingRepository (TenantScopedRepository)
              └── Hibernate tenantFilter 自動過濾 tenant_id

SettingKey (枚舉)
  └── 提供預設值，當 DB 查無資料時 fallback
```

## 4. API 端點

基礎路徑：`/v1/auth/system-settings`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| `GET` | `/` | ADMIN / SUPER_ADMIN / SYSTEM_SETTINGS_VIEW | 列出所有設定 |
| `PUT` | `/{key}` | ADMIN / SUPER_ADMIN / SYSTEM_SETTINGS_MANAGE | 更新指定設定 |
| `GET` | `/idle-timeout` | authenticated | 取得閒置超時分鐘數 |
| `PUT` | `/idle-timeout` | ADMIN / SUPER_ADMIN / SYSTEM_SETTINGS_MANAGE | 更新閒置超時（1~480 分鐘） |

### 請求參數

**PUT /{key}**：
| 參數 | 來源 | 驗證 | 說明 |
|------|------|------|------|
| `key` | path | @NotBlank | 設定鍵 |
| `value` | query | @NotBlank | 設定值 |

**PUT /idle-timeout**：
| 參數 | 來源 | 驗證 | 說明 |
|------|------|------|------|
| `minutes` | query | @Min(1) @Max(480) | 閒置超時分鐘數 |

## 5. 業務邏輯/機制說明

### 5.1 Key-Value Store 模式
- 每個租戶獨立的設定空間（透過 tenantFilter 隔離）
- 設定項透過 `setting_key` 查詢，值存為字串
- 更新時手動設定 `updatedAt`

### 5.2 預定義設定鍵（SettingKey 枚舉）

| Key | 預設值 | 說明 |
|-----|--------|------|
| `idle_timeout_minutes` | `15` | 前端閒置超時分鐘數 |
| `frontend_base_url` | `http://localhost:5173` | 前端基礎 URL |

### 5.3 閒置超時設定
- `getIdleTimeoutMinutes()`：查 DB，查無時回傳 SettingKey 預設值（15 分鐘）
- `updateIdleTimeoutMinutes()`：驗證範圍 1~480 分鐘
- 前端透過 `GET /idle-timeout`（不需管理權限）取得超時設定

### 5.4 通用設定查詢
- `getSetting(key)`：供內部模組使用，查無時拋 `IllegalStateException`
- `findAllSettings()`：回傳所有設定的 DTO 列表
- `updateSetting(key, value)`：查無 key 時拋 `IllegalStateException`

## 6. 資料流

### 取得閒置超時
```
GET /v1/auth/system-settings/idle-timeout
  → SystemSettingService.getIdleTimeoutMinutes()
    → SystemSettingRepository.findBySettingKey("idle_timeout_minutes")
      → TenantFilterAspect 自動加 tenant_id 過濾
    → 有值 → parseInt(settingValue)
    → 無值 → SettingKey.IDLE_TIMEOUT_MINUTES.defaultValue → 15
```

### 更新設定
```
PUT /v1/auth/system-settings/{key}?value=xxx
  → SystemSettingService.updateSetting(key, value)
    → SystemSettingRepository.findBySettingKey(key)
      → 租戶過濾 → 找到 Entity
    → entity.setSettingValue(value)
    → entity.setUpdatedAt(now)
    → save()
    → 回傳 SystemSettingDto
```

## 7. ErrorCode / Enum 定義

### SettingKey 枚舉

| 常數 | key 值 | 預設值 | 說明 |
|------|--------|--------|------|
| `IDLE_TIMEOUT_MINUTES` | `idle_timeout_minutes` | `15` | 閒置超時分鐘數 |
| `FRONTEND_BASE_URL` | `frontend_base_url` | `http://localhost:5173` | 前端基礎 URL |

### 異常處理
本模組未定義專屬 ErrorCode，使用 Java 原生異常：
- `IllegalStateException("Setting not found: " + key)` → 設定鍵不存在時拋出
- 由 `GlobalExceptionHandler` 統一處理為 HTTP 500
