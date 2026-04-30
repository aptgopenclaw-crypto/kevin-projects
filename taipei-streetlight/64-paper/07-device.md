# 07 — 設備管理模組 (Device Module)

## 1. 模組概述

設備管理模組是台北市路燈管理系統的核心模組，負責管理所有路燈相關設備的生命週期，包括燈桿（POLE）、燈具（LUMINAIRE）、分電箱（PANEL_BOX）、控制器（CONTROLLER）、電力設備（POWER_EQUIPMENT）及附屬設備（ATTACHMENT）。

模組採用 **父子設備樹狀結構**，以燈桿或分電箱作為父設備，燈具、控制器等作為掛載在其上的子元件。支援多座標系統（TWD97、TWD67、WGS84 經緯度）自動轉換、JSONB 動態擴展欄位、設備模板驗證、組合元件置換、以及 CSV/XLSX/ODS 多格式匯出。

所有資料表皆支援多租戶隔離（tenant_id），並透過 Hibernate `@Filter` 機制自動套用租戶篩選。

---

## 2. 資料表結構 (Table Schema)

### 2.1 devices — 設備主表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| device_type | VARCHAR(30) | NOT NULL, ENUM | 設備類型 |
| device_code | VARCHAR(100) | NOT NULL | 設備代碼（租戶內唯一） |
| device_name | VARCHAR(200) | | 設備名稱 |
| twd97_x | DECIMAL(12,3) | | TWD97 X 座標 |
| twd97_y | DECIMAL(12,3) | | TWD97 Y 座標 |
| lng | DECIMAL(11,7) | | WGS84 經度 |
| lat | DECIMAL(10,7) | | WGS84 緯度 |
| elevation | DECIMAL(8,3) | | 海拔高度 |
| twd67_x | DECIMAL(12,3) | | TWD67 X 座標 |
| twd67_y | DECIMAL(12,3) | | TWD67 Y 座標 |
| taipower_coord | VARCHAR(100) | | 台電座標 |
| dept_id | BIGINT | FK | 所屬部門 ID |
| contract_id | BIGINT | FK | 關聯契約 ID |
| property_owner | VARCHAR(200) | | 財產所有人 |
| status | VARCHAR(20) | NOT NULL, ENUM | 設備狀態 |
| installed_at | DATE | | 安裝日期 |
| decommissioned_at | DATE | | 除役日期 |
| parent_device_id | BIGINT | FK (self) | 父設備 ID（樹狀結構） |
| mount_position | VARCHAR(50) | | 掛載位置 |
| connectivity_type | VARCHAR(20) | ENUM | 連線類型 |
| network_config | JSONB | | 網路配置（動態） |
| last_heartbeat_at | TIMESTAMP | | 最後心跳時間 |
| circuit_id | BIGINT | FK | 所屬回路 ID |
| attributes | JSONB | | 設備專有屬性（動態） |
| created_by | VARCHAR(50) | | 建立者 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.2 circuits — 回路表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| panel_box_device_id | BIGINT | FK | 所屬分電箱設備 ID |
| circuit_number | VARCHAR(50) | NOT NULL | 回路編號（租戶內唯一） |
| circuit_name | VARCHAR(200) | | 回路名稱 |
| taipower_account | VARCHAR(50) | | 台電電號 |
| usage_type | VARCHAR(50) | | 用途類型 |
| status | VARCHAR(20) | NOT NULL | 狀態（預設 ACTIVE） |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.3 contracts — 契約表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| contract_code | VARCHAR(100) | NOT NULL | 契約代碼 |
| contract_name | VARCHAR(300) | NOT NULL | 契約名稱 |
| budget_year | INTEGER | | 預算年度 |
| procurement_number | VARCHAR(100) | | 採購編號 |
| contractor_name | VARCHAR(200) | | 承商名稱 |
| contractor_contact | VARCHAR(200) | | 承商聯絡人 |
| asset_category | VARCHAR(50) | | 財產類別 |
| quantity | INTEGER | | 數量 |
| start_date | DATE | | 契約起始日 |
| end_date | DATE | | 契約結束日 |
| acceptance_date | DATE | | 驗收日期 |
| warranty_years | INTEGER | | 保固年數 |
| warranty_expiry | DATE | | 保固到期日 |
| status | VARCHAR(20) | NOT NULL, ENUM | 契約狀態 |
| attributes | JSONB | | 擴展屬性 |
| created_by | VARCHAR(50) | | 建立者 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.4 device_events — 設備事件表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| device_id | BIGINT | NOT NULL, FK | 關聯設備 ID |
| event_type | VARCHAR(30) | NOT NULL, ENUM | 事件類型 |
| event_date | TIMESTAMP | NOT NULL | 事件發生時間 |
| description | TEXT | | 事件描述 |
| attachments | JSONB | | 附件清單 |
| repair_ticket_id | BIGINT | FK | 關聯維修工單 ID |
| replacement_item_id | BIGINT | FK | 關聯換裝明細 ID |
| created_by | VARCHAR(50) | | 建立者 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |

### 2.5 device_templates — 設備模板表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| device_type | VARCHAR(30) | NOT NULL, UNIQUE(tenant_id, device_type) | 設備類型 |
| schema | JSONB | NOT NULL | 屬性 Schema 定義 |
| version | INTEGER | NOT NULL, 預設 1 | Schema 版本號 |
| created_by | VARCHAR(50) | | 建立者 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.6 device_managers — 設備管理員表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| device_id | BIGINT | NOT NULL, FK | 關聯設備 ID |
| user_id | VARCHAR(50) | NOT NULL | 管理員使用者 ID |
| assigned_at | TIMESTAMP | NOT NULL | 指派時間 |
| assigned_by | VARCHAR(50) | | 指派者 |

---

## 3. 實體關聯

```
Device (devices)
  ├── 1:N → Device (parent_device_id, 自關聯，燈桿→燈具/控制器)
  ├── N:1 → Circuit (circuit_id)
  ├── N:1 → Contract (contract_id)
  ├── N:1 → Department (dept_id，外部模組)
  ├── 1:N → DeviceEvent (device_id)
  └── 1:N → DeviceManager (device_id)

Circuit (circuits)
  └── N:1 → Device (panel_box_device_id，分電箱)

DeviceTemplate (device_templates)
  └── 透過 device_type 與 Device.deviceType 邏輯關聯

DeviceEvent (device_events)
  ├── N:1 → Device (device_id)
  ├── N:1 → RepairTicket (repair_ticket_id，跨模組)
  └── N:1 → ReplacementItem (replacement_item_id，跨模組)
```

---

## 4. API 端點摘要

### 4.1 設備 API (`/v1/auth/devices`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/` | DEVICE_VIEW | 設備列表（分頁，支援 deviceType/status/keyword 篩選） |
| GET | `/{id}` | DEVICE_VIEW | 設備明細（含組合元件） |
| POST | `/` | DEVICE_MANAGE | 新增設備 |
| PUT | `/{id}` | DEVICE_MANAGE | 更新設備 |
| DELETE | `/{id}` | DEVICE_MANAGE | 刪除設備（有子元件時禁止） |
| POST | `/{id}/decommission` | DEVICE_MANAGE | 設備除役 |
| GET | `/{id}/events` | DEVICE_VIEW | 設備事件歷程（分頁） |
| GET | `/{id}/components` | DEVICE_VIEW | 查詢子元件（可選含除役） |
| POST | `/{id}/components/replace` | DEVICE_MANAGE | 置換子元件 |
| GET | `/export` | DEVICE_EXPORT | 匯出設備（CSV/XLSX/ODS） |

### 4.2 回路 API (`/v1/auth/circuits`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/` | CIRCUIT_VIEW | 回路列表 |
| GET | `/{id}` | CIRCUIT_VIEW | 回路明細 |
| POST | `/` | CIRCUIT_MANAGE | 新增回路 |
| PUT | `/{id}` | CIRCUIT_MANAGE | 更新回路 |
| DELETE | `/{id}` | CIRCUIT_MANAGE | 刪除回路（有設備時禁止） |

### 4.3 契約 API (`/v1/auth/contracts`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/` | CONTRACT_VIEW | 契約列表（支援 status/keyword） |
| GET | `/{id}` | CONTRACT_VIEW | 契約明細 |
| POST | `/` | CONTRACT_MANAGE | 新增契約 |
| PUT | `/{id}` | CONTRACT_MANAGE | 更新契約 |
| DELETE | `/{id}` | CONTRACT_MANAGE | 刪除契約 |

### 4.4 設備模板 API (`/v1/auth/device-templates`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/{deviceType}/schema` | DEVICE_VIEW | 取得指定類型的 Schema |
| PUT | `/{deviceType}/schema` | DEVICE_TEMPLATE_MANAGE | 更新 Schema |

---

## 5. 業務邏輯

### 5.1 設備代碼唯一性
同一租戶內 `device_code` 不得重複，新增與更新時皆會檢查。

### 5.2 座標自動填充
透過 `CoordinateService.autoFill()` 進行 TWD97、TWD67、WGS84 三套座標系統之間的自動轉換。只要提供任一組座標即可自動計算其餘座標。

### 5.3 樹狀結構與循環參照防護
設備支援 `parent_device_id` 自關聯形成父子樹。系統在設定父設備時會呼叫 `checkCircularReference()` 防止循環參照。

### 5.4 JSONB 大小限制
`attributes` 和 `network_config` 欄位的 JSONB 內容不得超過 10,000 字元。

### 5.5 設備模板驗證
建立/更新設備時，系統會根據 `device_templates` 中該設備類型的 Schema 定義驗證 `attributes` 欄位：
- Schema 不存在時：不驗證（向後相容）
- Schema 存在時：驗證 required 必填欄位、type 類型檢查（number/text/select/checkbox/date）
- 採 Open Schema 設計：額外的 key 保留不報錯

### 5.6 組合元件置換
燈桿或分電箱可進行子元件置換，流程為：
1. 將舊元件狀態設為 `DECOMMISSIONED`
2. 建立新元件，自動繼承父設備的 `parentDeviceId`、`mountPosition`、座標、部門、回路等資訊
3. 記錄三筆設備事件：父設備 REPLACE、舊元件 DECOMMISSION、新元件 INSTALL

### 5.7 刪除保護
- 設備有子元件時不可刪除（DEVICE_HAS_CHILDREN）
- 回路有關聯設備時不可刪除（CIRCUIT_HAS_DEVICES）

### 5.8 資料權限
設備列表查詢透過 `@DataPermission` 與 `DataScopeHelper` 自動套用部門資料範圍過濾。

---

## 6. 資料流

```
[使用者/前端]
    │
    ▼
[DeviceController / CircuitController / ContractController]
    │  ── 權限驗證 (@PreAuthorize) + 審計記錄 (@AuditEvent)
    ▼
[DeviceService / CircuitService / ContractService]
    │  ── 業務邏輯驗證（唯一性、循環參照、JSONB 大小、模板驗證）
    │  ── CoordinateService 座標自動填充
    │  ── DataScopeHelper 資料權限過濾
    ▼
[DeviceRepository / CircuitRepository / ContractRepository]
    │  ── JPA + Hibernate @Filter (tenantFilter)
    ▼
[PostgreSQL — devices / circuits / contracts / device_events / device_templates / device_managers]
```

匯出流程：
```
[DeviceController GET /export]
    → DeviceExportService.queryForExport()  // 含資料權限
    → exportCsv() / exportXlsx() / exportOds()
    → HttpServletResponse OutputStream
```

---

## 7. 列舉定義

### DeviceType — 設備類型
| 值 | 說明 |
|---|---|
| POLE | 燈桿 |
| LUMINAIRE | 燈具 |
| PANEL_BOX | 分電箱 |
| CONTROLLER | 控制器 |
| POWER_EQUIPMENT | 電力設備 |
| ATTACHMENT | 附屬設備 |

### DeviceStatus — 設備狀態
| 值 | 說明 |
|---|---|
| ACTIVE | 啟用中 |
| REPORTED | 已通報異常 |
| UNDER_REPAIR | 維修中 |
| INACTIVE | 停用 |
| DECOMMISSIONED | 已除役 |

### ConnectivityType — 連線類型
| 值 | 說明 |
|---|---|
| NONE | 無連線 |
| DIRECT | 直連 |
| GATEWAY | 經由閘道器 |

### ContractStatus — 契約狀態
| 值 | 說明 |
|---|---|
| ACTIVE | 生效中 |
| EXPIRED | 已到期 |
| TERMINATED | 已終止 |

### DeviceEventType — 設備事件類型
| 值 | 說明 |
|---|---|
| INSTALL | 安裝 |
| REPLACE | 置換 |
| REPAIR | 維修 |
| INSPECT | 巡查 |
| ADOPT | 認養 |
| DECOMMISSION | 除役 |
| MATERIAL_CHANGE | 材料變更 |
