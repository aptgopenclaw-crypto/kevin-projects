# 08 — 障礙偵測模組 (Fault Module)

## 1. 模組概述

障礙偵測模組負責管理路燈系統中的障礙工單（FaultTicket），並提供關聯障礙分析功能（FaultCorrelation）。當設備發生異常時，可透過民眾通報、巡查發現或系統自動告警等方式建立障礙工單。

模組的核心特色為**被動式關聯障礙偵測**：每當新的障礙工單建立時，系統會自動分析是否存在同回路短時間內大量障礙的情況（如停電導致整條回路斷電），並自動建立 FaultCorrelation 記錄，協助管理人員快速定位根因。

障礙工單經審核確認後，可自動或手動觸發建立維修工單（RepairTicket），串接至維修流程。

---

## 2. 資料表結構 (Table Schema)

### 2.1 fault_tickets — 障礙工單表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| device_id | BIGINT | FK | 關聯設備 ID |
| circuit_id | BIGINT | FK | 關聯回路 ID |
| correlation_id | BIGINT | FK | 關聯障礙群組 ID |
| source | VARCHAR(30) | NOT NULL, ENUM | 障礙來源 |
| status | VARCHAR(20) | NOT NULL, ENUM | 工單狀態 |
| priority | VARCHAR(10) | | 優先等級（預設 NORMAL） |
| description | TEXT | | 障礙描述 |
| reported_by | VARCHAR(50) | | 通報人 |
| reported_at | TIMESTAMP | NOT NULL | 通報時間 |
| resolved_at | TIMESTAMP | | 解決時間 |
| resolved_by | VARCHAR(50) | | 解決者 |
| resolution_note | TEXT | | 解決備註 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.2 fault_correlations — 關聯障礙表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| root_cause_type | VARCHAR(30) | NOT NULL, ENUM | 根因類型 |
| root_cause_id | BIGINT | NOT NULL | 根因對象 ID（如回路 ID、分電箱 ID） |
| affected_count | INTEGER | NOT NULL | 受影響設備數量 |
| status | VARCHAR(20) | NOT NULL | 關聯狀態（DETECTED/CONFIRMED/RESOLVED） |
| detected_at | TIMESTAMP | NOT NULL | 偵測時間 |
| confirmed_at | TIMESTAMP | | 確認時間 |
| resolved_at | TIMESTAMP | | 解決時間 |
| resolution_note | TEXT | | 解決備註 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |

---

## 3. 實體關聯

```
FaultTicket (fault_tickets)
  ├── N:1 → Device (device_id，跨模組)
  ├── N:1 → Circuit (circuit_id，跨模組)
  └── N:1 → FaultCorrelation (correlation_id)

FaultCorrelation (fault_correlations)
  ├── 1:N → FaultTicket (透過 correlation_id)
  └── root_cause_id → 視 root_cause_type 指向 Circuit / Device(PANEL_BOX) / Device(GATEWAY)
```

跨模組關聯：
- `FaultTicket` → `RepairTicket`（維修模組透過 `fault_ticket_id` 反向關聯）
- `FaultTicket` ← `InspectionRecord`（巡查發現異常時自動建立障礙工單）

---

## 4. API 端點摘要

### 障礙工單 API (`/v1/auth/faults`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/` | FAULT_VIEW | 障礙工單列表（分頁，支援 status/keyword 篩選） |
| GET | `/{id}` | FAULT_VIEW | 障礙工單明細 |
| POST | `/` | FAULT_MANAGE | 新增障礙工單 |
| POST | `/{id}/resolve` | FAULT_MANAGE | 標記工單為已解決 |

---

## 5. 業務邏輯

### 5.1 障礙工單建立流程
1. 接收障礙通報（指定設備 ID 和/或回路 ID、來源、優先等級、描述）
2. 設定初始狀態為 `OPEN`，記錄通報者與時間
3. 儲存後**同步觸發關聯障礙偵測** `FaultCorrelationService.detectOnNewTicket()`

### 5.2 關聯障礙偵測（被動式）

偵測在新工單建立後同步執行，目前支援兩個維度：

**維度一：同回路近期大量障礙**
- 條件：同一回路在最近 30 分鐘內（`CIRCUIT_WINDOW_MINUTES = 30`）累計 ≥ 3 筆障礙工單（`CIRCUIT_THRESHOLD = 3`，排除已合併的工單）
- 動作：建立 `FaultCorrelation`，root_cause_type = `CIRCUIT`，root_cause_id = 回路 ID
- 僅在工單具有 `circuit_id` 時偵測

**維度二：Gateway 關聯障礙**
- 條件：設備的連線類型為 `GATEWAY`，且有父設備
- 目前為預留實作（記錄 debug log），未來會合併至既有 Gateway 關聯群組

### 5.3 障礙解決
- 將狀態設為 `RESOLVED`
- 記錄解決時間、解決者、解決備註

### 5.4 巡查自動建立障礙工單
巡查模組（InspectionService）在巡查結果為 `NEED_REPAIR` 時，會呼叫 `FaultTicketService.createFromInspection()` 自動建立來源為 `PATROL` 的障礙工單。

### 5.5 與維修模組的串接
維修模組的 `RepairTicketService.createFromFault()` 可根據障礙工單 ID 自動建立維修工單，繼承設備、回路、描述等資訊。

---

## 6. 資料流

### 障礙通報主流程
```
[民眾通報 / 巡查發現 / 系統自動告警]
    │
    ▼
[FaultTicketController POST /v1/auth/faults]
    │  ── @PreAuthorize('FAULT_MANAGE')
    ▼
[FaultTicketService.create()]
    │  ── 建立 FaultTicket (status=OPEN)
    │  ── 呼叫 FaultCorrelationService.detectOnNewTicket()
    │      ├── 檢查同回路 30 分鐘內障礙數量 → 超過閾值則建立 FaultCorrelation
    │      └── 檢查 Gateway 關聯（預留）
    ▼
[FaultTicketRepository → PostgreSQL fault_tickets]
```

### 巡查自動建立障礙工單
```
[InspectionService.createRecord()]
    │  ── 巡查結果 = NEED_REPAIR
    ▼
[FaultTicketService.createFromInspection()]
    │  ── source = PATROL, status = OPEN
    │  ── 觸發關聯偵測
    ▼
[InspectionRecord.faultTicketId = 新建工單 ID]
```

### 障礙工單 → 維修工單
```
[FaultTicket (OPEN/IN_PROGRESS)]
    │  ── 審核通過
    ▼
[RepairTicketService.createFromFault(faultTicketId)]
    │  ── 建立 RepairTicket，繼承設備/回路/描述
    ▼
[維修流程啟動]
```

---

## 7. 列舉定義

### FaultTicketSource — 障礙來源
| 值 | 說明 |
|---|---|
| CITIZEN_REPORT | 民眾通報 |
| PATROL | 巡查發現 |
| AUTO_ALERT | 系統自動告警 |

### FaultTicketStatus — 障礙工單狀態
| 值 | 說明 |
|---|---|
| OPEN | 開立中 |
| IN_PROGRESS | 處理中 |
| RESOLVED | 已解決 |
| MERGED | 已合併（納入關聯群組） |

### RootCauseType — 根因類型
| 值 | 說明 |
|---|---|
| CIRCUIT | 回路障礙（如整條回路斷電） |
| PANEL_BOX | 分電箱障礙 |
| GATEWAY | 閘道器障礙（通訊中斷） |
| POWER_OUTAGE | 停電事件 |
