# 09 — 維修管理模組 (Repair Module)

## 1. 模組概述

維修管理模組負責管理路燈維修的完整工作流程，從報修受理、收案、派工、施工、完工回報到結案。模組包含四大子功能：

1. **維修工單 (RepairTicket)**：核心工單，支援三種建立路徑（障礙工單轉入、管理端手動立案、民眾公開報修）
2. **派工管理 (RepairDispatch)**：將維修工單指派給承商或維修人員
3. **巡查管理 (InspectionTask / InspectionRecord)**：定期或一次性巡查任務與紀錄，異常時自動建立障礙工單
4. **工單附件 (TicketAttachment)**：支援多媒體檔案上傳、圖片消毒、病毒掃描

模組與工作流引擎（WorkflowService）深度整合，每次狀態轉換皆同步推進工作流程。同時提供**民眾匿名報修入口**，支援驗證碼驗證、頻率限制、個資聲明同意。

---

## 2. 資料表結構 (Table Schema)

### 2.1 repair_tickets — 維修工單表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| ticket_number | VARCHAR(50) | NOT NULL | 工單編號（格式：RT-yyyyMMdd-NNN） |
| fault_ticket_id | BIGINT | FK | 來源障礙工單 ID |
| device_id | BIGINT | FK | 關聯設備 ID |
| circuit_id | BIGINT | FK | 關聯回路 ID |
| contract_id | BIGINT | FK | 關聯契約 ID |
| source | VARCHAR(30) | NOT NULL, ENUM | 報修來源 |
| reporter_name | VARCHAR(100) | | 報修人姓名 |
| reporter_phone | VARCHAR(50) | | 報修人電話 |
| reporter_email | VARCHAR(200) | | 報修人 Email |
| report_address | TEXT | | 報修地點 |
| report_description | TEXT | | 報修描述 |
| reported_at | TIMESTAMP | NOT NULL | 報修時間 |
| fault_category | VARCHAR(50) | | 故障類別 |
| fault_cause | VARCHAR(50) | | 故障原因 |
| repair_description | TEXT | | 維修描述（完工時填寫） |
| completed_at | TIMESTAMP | | 完工時間 |
| status | VARCHAR(30) | NOT NULL, ENUM | 工單狀態 |
| priority | VARCHAR(10) | ENUM | 優先等級 |
| dept_id | BIGINT | FK | 所屬部門 ID |
| created_by | VARCHAR(50) | | 建立者 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.2 repair_dispatches — 派工記錄表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| repair_ticket_id | BIGINT | NOT NULL, FK | 關聯維修工單 ID |
| contract_id | BIGINT | FK | 契約 ID |
| assigned_to | BIGINT | FK | 指派對象（使用者 ID） |
| assigned_org | VARCHAR(200) | | 指派單位名稱 |
| dispatch_note | TEXT | | 派工備註 |
| dispatched_at | TIMESTAMP | NOT NULL | 派工時間 |
| dispatched_by | BIGINT | NOT NULL | 派工者 ID |
| due_date | DATE | | 預計完成日 |
| status | VARCHAR(20) | NOT NULL, ENUM | 派工狀態 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |

### 2.3 inspection_tasks — 巡查任務表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| task_name | VARCHAR(200) | NOT NULL | 任務名稱 |
| task_type | VARCHAR(20) | NOT NULL, ENUM | 任務類型 |
| schedule_cron | VARCHAR(100) | | Cron 排程表達式 |
| start_date | DATE | | 開始日期 |
| end_date | DATE | | 結束日期 |
| area_scope | JSONB | | 巡查區域範圍定義 |
| dept_id | BIGINT | FK | 所屬部門 ID |
| assigned_to | BIGINT | FK | 指派巡查員 |
| status | VARCHAR(20) | NOT NULL, ENUM | 任務狀態 |
| created_by | VARCHAR(50) | | 建立者 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.4 inspection_records — 巡查紀錄表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| task_id | BIGINT | NOT NULL, FK | 關聯巡查任務 ID |
| inspector_id | BIGINT | NOT NULL | 巡查員 ID |
| inspection_date | TIMESTAMP | NOT NULL | 巡查日期時間 |
| device_id | BIGINT | FK | 巡查設備 ID |
| result | VARCHAR(20) | NOT NULL, ENUM | 巡查結果 |
| notes | TEXT | | 巡查備註 |
| attachments | JSONB | | 附件清單 |
| fault_ticket_id | BIGINT | FK | 自動建立的障礙工單 ID |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |

### 2.5 ticket_attachments — 工單附件表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| ticket_type | VARCHAR(30) | NOT NULL, ENUM | 工單類型（多態 FK） |
| ticket_id | BIGINT | NOT NULL | 工單 ID |
| file_type | VARCHAR(20) | NOT NULL | 檔案類型（PHOTO/VIDEO/AUDIO/DOCUMENT） |
| file_url | VARCHAR(500) | NOT NULL | 檔案儲存路徑 |
| file_name | VARCHAR(300) | | 原始檔名 |
| file_size | BIGINT | | 檔案大小（bytes） |
| description | VARCHAR(500) | | 附件說明 |
| gps_lat | DECIMAL(10,7) | | GPS 緯度 |
| gps_lng | DECIMAL(11,7) | | GPS 經度 |
| taken_at | TIMESTAMP | | 拍攝時間 |
| phase | VARCHAR(20) | ENUM | 拍攝階段 |
| scan_status | VARCHAR(20) | ENUM | 病毒掃描狀態 |
| uploaded_by | VARCHAR(50) | | 上傳者 |
| uploaded_at | TIMESTAMP | NOT NULL | 上傳時間 |

---

## 3. 實體關聯

```
RepairTicket (repair_tickets)
  ├── N:1 → FaultTicket (fault_ticket_id，跨模組)
  ├── N:1 → Device (device_id，跨模組)
  ├── N:1 → Circuit (circuit_id，跨模組)
  ├── N:1 → Contract (contract_id，跨模組)
  ├── 1:N → RepairDispatch (repair_ticket_id)
  ├── 1:N → TicketAttachment (ticket_type=REPAIR_TICKET, ticket_id)
  └── 1:1 → WorkflowInstance (透過 WorkflowService)

RepairDispatch (repair_dispatches)
  └── N:1 → RepairTicket (repair_ticket_id)

InspectionTask (inspection_tasks)
  └── 1:N → InspectionRecord (task_id)

InspectionRecord (inspection_records)
  ├── N:1 → InspectionTask (task_id)
  ├── N:1 → Device (device_id，跨模組)
  └── 1:1 → FaultTicket (fault_ticket_id，自動建立)

TicketAttachment (ticket_attachments)
  └── 多態 FK → RepairTicket / FaultTicket / ReplacementOrder (透過 ticket_type + ticket_id)
```

---

## 4. API 端點摘要

### 4.1 維修工單 API (`/v1/auth/repair/tickets`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/` | REPAIR_VIEW | 工單列表（分頁，支援 status/source/priority/deptId/keyword） |
| GET | `/{id}` | REPAIR_VIEW | 工單明細 |
| POST | `/` | REPAIR_MANAGE | 手動立案 |
| PUT | `/{id}` | REPAIR_MANAGE | 更新工單 |
| POST | `/{id}/accept` | REPAIR_MANAGE | 收案 |
| POST | `/{id}/dispatch` | REPAIR_DISPATCH | 派工 |
| POST | `/{id}/complete` | REPAIR_MANAGE | 完工回報 |
| POST | `/{id}/transfer` | REPAIR_MANAGE | 改分轉送 |
| GET | `/{id}/dispatches` | REPAIR_VIEW | 查詢派工記錄 |

### 4.2 民眾公開報修 API (`/v1/noauth/public/repair`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| POST | `/` | 無需登入 | 民眾提交報修（含驗證碼，限 3 次/5 分鐘） |
| GET | `/{ticketNo}/status` | 無需登入 | 查詢報修進度（需電話號碼驗證，限 10 次/分鐘） |

### 4.3 巡查管理 API (`/v1/auth/inspection`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/tasks` | INSPECTION_VIEW | 巡查任務列表 |
| GET | `/tasks/{id}` | INSPECTION_VIEW | 巡查任務明細 |
| POST | `/tasks` | INSPECTION_MANAGE | 建立巡查任務 |
| PUT | `/tasks/{id}` | INSPECTION_MANAGE | 更新巡查任務 |
| DELETE | `/tasks/{id}` | INSPECTION_MANAGE | 停用巡查任務 |
| GET | `/tasks/{taskId}/records` | INSPECTION_VIEW | 巡查紀錄列表 |
| POST | `/records` | INSPECTION_MANAGE | 新增巡查紀錄 |
| GET | `/records/{id}` | INSPECTION_VIEW | 巡查紀錄明細 |

### 4.4 工單附件 API (`/v1/auth/repair`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/tickets/{ticketId}/attachments` | REPAIR_VIEW | 查詢工單附件列表 |
| POST | `/tickets/{ticketId}/attachments` | REPAIR_MANAGE | 上傳附件（含圖片消毒+病毒掃描） |
| GET | `/attachments/{id}/download` | REPAIR_VIEW | 下載附件 |

---

## 5. 業務邏輯

### 5.1 維修工單建立的三種路徑

**路徑 A — 障礙工單轉入**
- `RepairTicketService.createFromFault(faultTicketId)`
- 從障礙工單繼承設備、回路、描述資訊
- 來源設為 `FAULT_TICKET`

**路徑 B — 管理端手動立案**
- `RepairTicketService.createDirect(request)`
- 支援來源：EXTERNAL_1999（1999 市民專線）、PHONE（電話）、PATROL（巡查）等
- 可指定設備、回路、契約、部門、優先等級

**路徑 C — 民眾公開報修**
- `RepairTicketService.createPublicTicket(request)`
- 匿名操作，不需登入
- 透過號碼牌編號（QR Code 掃描帶入）自動比對設備和租戶
- 需驗證碼 + 個資使用聲明同意
- 建立者記為 `CITIZEN`

所有路徑建立後皆自動建立工作流實例（`REPAIR_DISPATCH` 流程）。

### 5.2 工單狀態機
```
PENDING → ACCEPTED (收案)
  → DISPATCHED (派工)
    → IN_PROGRESS (施工中)
      → COMPLETION_REPORTED (完工回報)
        → PENDING_REVIEW (審查中)
          → CLOSED (結案)
          → RETURNED (退回修正)
    → TRANSFERRED (改分轉送) → 可重新派工
```

### 5.3 派工邏輯
- 僅 `ACCEPTED` 或 `TRANSFERRED` 狀態的工單可派工
- 派工時建立 `RepairDispatch` 記錄，並更新工單狀態為 `DISPATCHED`
- 同步推進工作流程

### 5.4 完工回報
- 僅 `IN_PROGRESS` 狀態可提交完工
- 必須填寫維修描述（repair_description）
- 記錄完工時間，狀態轉為 `COMPLETION_REPORTED`

### 5.5 巡查與自動建障
- 巡查紀錄結果為 `NEED_REPAIR` 且有指定設備時，自動呼叫 `FaultTicketService.createFromInspection()` 建立障礙工單
- 將新建的 fault_ticket_id 回寫至 InspectionRecord

### 5.6 附件安全機制
上傳附件時依序執行：
1. **副檔名白名單 + Magic bytes 驗證** — 防止偽裝檔案
2. **圖片消毒（ImageSanitizer）** — 僅圖片類型，清除 EXIF 及潛在惡意內容
3. **病毒掃描（ClamAV）** — 偵測到病毒則刪除檔案並拋出例外；ClamAV 不可用時標記為 `PENDING`

### 5.7 民眾報修進度查詢
- 需同時提供工單編號和報修人手機號碼
- 系統繞過租戶過濾進行查詢
- 回傳狀態標籤（中文）而非原始 enum 值

---

## 6. 資料流

### 維修工單主流程
```
[報修來源（障礙轉入 / 手動立案 / 民眾網頁）]
    │
    ▼
[RepairTicketService.create*()]
    │  ── 建立 RepairTicket (status=PENDING)
    │  ── 建立 WorkflowInstance (REPAIR_DISPATCH)
    ▼
[收案] RepairTicketService.accept()
    │  ── status → ACCEPTED
    │  ── workflow transition → ACCEPTED
    ▼
[派工] RepairDispatchService.dispatch()
    │  ── 建立 RepairDispatch
    │  ── status → DISPATCHED
    │  ── workflow transition → DISPATCHED
    ▼
[施工中] → status = IN_PROGRESS
    ▼
[完工回報] RepairTicketService.reportCompletion()
    │  ── 填寫維修描述、故障原因
    │  ── status → COMPLETION_REPORTED
    ▼
[審查] → PENDING_REVIEW → CLOSED / RETURNED
```

### 民眾報修流程
```
[民眾掃描 QR Code → 報修網頁]
    │
    ▼
[PublicRepairController POST /v1/noauth/public/repair]
    │  ── 驗證碼檢查 (CaptchaService)
    │  ── 個資聲明同意檢查
    │  ── 頻率限制 (3次/5分鐘)
    ▼
[RepairTicketService.createPublicTicket()]
    │  ── 透過 poleNumber 查找 LightPoleNumber → 取得 tenantId + deviceId
    │  ── 建立工單 + 工作流
    ▼
[回傳工單編號 → 民眾可用編號+手機號碼查詢進度]
```

---

## 7. 列舉定義

### RepairTicketStatus — 維修工單狀態
| 值 | 中文標籤 | 說明 |
|---|---|---|
| PENDING | 待處理 | 初始狀態 |
| ACCEPTED | 已收案 | 收案確認 |
| DISPATCHED | 已派工 | 已指派承商 |
| IN_PROGRESS | 施工中 | 承商施工中 |
| COMPLETION_REPORTED | 已完工 | 承商回報完工 |
| PENDING_REVIEW | 審查中 | 等待驗收審查 |
| RETURNED | 退回修正 | 審查不通過 |
| TRANSFERRED | 改分轉送 | 轉派其他單位 |
| TRACKING | 追蹤中 | 持續追蹤 |
| CLOSED | 已結案 | 結案歸檔 |

### RepairTicketSource — 報修來源
| 值 | 說明 |
|---|---|
| FAULT_TICKET | 障礙工單轉入 |
| CITIZEN_WEB | 民眾網頁報修 |
| EXTERNAL_1999 | 1999 市民專線 |
| PATROL | 巡查發現 |
| PHONE | 電話報修 |

### RepairTicketPriority — 優先等級
| 值 | 說明 |
|---|---|
| LOW | 低 |
| NORMAL | 一般 |
| HIGH | 高 |
| URGENT | 緊急 |

### RepairDispatchStatus — 派工狀態
| 值 | 說明 |
|---|---|
| DISPATCHED | 已派工 |
| IN_PROGRESS | 施工中 |
| COMPLETED | 已完成 |
| CANCELLED | 已取消 |

### InspectionTaskType — 巡查任務類型
| 值 | 說明 |
|---|---|
| ONE_TIME | 一次性任務 |
| RECURRING | 週期性任務（搭配 Cron 排程） |

### InspectionTaskStatus — 巡查任務狀態
| 值 | 說明 |
|---|---|
| ACTIVE | 啟用中 |
| INACTIVE | 已停用 |

### InspectionResult — 巡查結果
| 值 | 說明 |
|---|---|
| NORMAL | 正常 |
| ABNORMAL | 異常（不需立即維修） |
| NEED_REPAIR | 需維修（自動建立障礙工單） |

### TicketType — 附件工單類型（多態 FK）
| 值 | 說明 |
|---|---|
| FAULT_TICKET | 障礙工單附件 |
| REPAIR_TICKET | 維修工單附件 |
| REPLACEMENT_ORDER | 換裝工單附件 |

### AttachmentPhase — 拍攝階段
| 值 | 說明 |
|---|---|
| BEFORE | 施工前 |
| DURING | 施工中 |
| AFTER | 施工後 |
| REPORT | 報告用 |

### ScanStatus — 病毒掃描狀態
| 值 | 說明 |
|---|---|
| PENDING | 待掃描 |
| CLEAN | 安全 |
| INFECTED | 已感染（檔案會被刪除） |
