# 10 — 換裝管理模組 (Replacement Module)

## 1. 模組概述

換裝管理模組負責管理路燈設備的換裝派工流程，涵蓋新裝、換裝、遷移、拆除、調整及遮光罩安裝等六種工程類型。模組採用 **工單（ReplacementOrder）+ 明細（ReplacementItem）** 的主從結構，一張工單可包含多個設備的換裝作業。

模組核心流程為：建單 → 派工 → 承商開工 → 自主檢核（實際執行設備置換） → 報竣送審 → 審核通過結案。自主檢核階段會呼叫設備模組的 `replaceComponent()` 完成實際的舊設備除役與新設備建立。

此外，模組包含**號碼牌管理（LightPoleNumber）** 子功能，負責管理燈桿號碼牌與 QR Code 的生成、匯出。號碼牌上的 QR Code 可導向民眾報修頁面，實現「掃碼報修」的使用情境。

---

## 2. 資料表結構 (Table Schema)

### 2.1 replacement_orders — 換裝工單表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| order_number | VARCHAR(50) | NOT NULL | 工單編號（格式：RO-yyyyMMdd-NNN） |
| repair_ticket_id | BIGINT | FK | 來源維修工單 ID |
| contract_id | BIGINT | FK | 關聯契約 ID |
| order_type | VARCHAR(30) | NOT NULL, ENUM | 換裝類型 |
| dispatch_reason | TEXT | | 派工原因 |
| location | TEXT | | 施工地點 |
| expected_quantity | INTEGER | | 預計數量 |
| work_period_start | DATE | | 施工期間起始日 |
| work_period_end | DATE | | 施工期間結束日 |
| assigned_contractor | VARCHAR(200) | | 指派承商 |
| status | VARCHAR(30) | NOT NULL, ENUM | 工單狀態 |
| dept_id | BIGINT | FK | 所屬部門 ID |
| created_by | VARCHAR(50) | | 建立者 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | | 更新時間 |

### 2.2 replacement_items — 換裝明細表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| order_id | BIGINT | NOT NULL, FK | 所屬工單 ID |
| parent_device_id | BIGINT | NOT NULL, FK | 燈桿設備 ID |
| old_device_id | BIGINT | NOT NULL, FK | 舊設備 ID |
| new_device_id | BIGINT | FK | 新設備 ID（自主檢核後填入） |
| before_device_type | VARCHAR(30) | | 換裝前設備類型 |
| before_spec | JSONB | | 換裝前設備規格（快照） |
| after_device_type | VARCHAR(30) | | 換裝後設備類型 |
| after_spec | JSONB | | 換裝後設備規格 |
| material_spec_id | BIGINT | FK | 材料規格 ID |
| approved_material_id | BIGINT | FK | 合格材料 ID |
| status | VARCHAR(20) | NOT NULL, ENUM | 明細狀態 |
| completed_at | TIMESTAMP | | 完成時間 |
| completed_by | VARCHAR(50) | | 完成者 |
| notes | TEXT | | 備註 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |

### 2.3 light_pole_numbers — 號碼牌表

| 欄位名稱 | 資料型態 | 約束 | 說明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| pole_number | VARCHAR(100) | NOT NULL | 號碼牌編號（租戶內唯一） |
| device_id | BIGINT | FK | 關聯設備（燈桿）ID |
| qr_code_url | VARCHAR(500) | | QR Code 內容 URL |
| issued_at | DATE | | 發放日期 |
| status | VARCHAR(20) | NOT NULL, ENUM | 號碼牌狀態 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |

---

## 3. 實體關聯

```
ReplacementOrder (replacement_orders)
  ├── N:1 → RepairTicket (repair_ticket_id，跨模組)
  ├── N:1 → Contract (contract_id，跨模組)
  ├── 1:N → ReplacementItem (order_id)
  ├── 1:N → TicketAttachment (ticket_type=REPLACEMENT_ORDER, ticket_id，跨模組)
  └── 1:1 → WorkflowInstance (透過 WorkflowService)

ReplacementItem (replacement_items)
  ├── N:1 → ReplacementOrder (order_id)
  ├── N:1 → Device (parent_device_id，燈桿)
  ├── N:1 → Device (old_device_id，舊設備)
  ├── N:1 → Device (new_device_id，新設備，自主檢核後)
  ├── N:1 → MaterialSpec (material_spec_id，跨模組)
  └── N:1 → ApprovedMaterial (approved_material_id，跨模組)

LightPoleNumber (light_pole_numbers)
  └── N:1 → Device (device_id，燈桿)
```

---

## 4. API 端點摘要

### 4.1 換裝工單 API (`/v1/auth/replacement/orders`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/` | REPLACEMENT_VIEW | 工單列表（分頁，支援 status/orderType/contractId/keyword/dateFrom/dateTo） |
| GET | `/{id}` | REPLACEMENT_VIEW | 工單明細（含明細清單+工作流當前步驟） |
| POST | `/` | REPLACEMENT_MANAGE | 直接建單 |
| POST | `/from-repair/{repairTicketId}` | REPLACEMENT_MANAGE | 從維修工單建單 |
| PUT | `/{id}` | REPLACEMENT_MANAGE | 更新工單（僅 DRAFT 狀態） |
| POST | `/{id}/dispatch` | REPLACEMENT_MANAGE | 派工至承商 |
| POST | `/{id}/start-work` | REPLACEMENT_MANAGE | 承商開工 |
| POST | `/{id}/self-check` | REPLACEMENT_MANAGE | 廠商自主檢核（執行實際設備置換） |
| POST | `/{id}/submit-review` | REPLACEMENT_MANAGE | 報竣送審 |
| POST | `/{id}/approve` | REPLACEMENT_MANAGE | 審核通過結案 |
| POST | `/{id}/return` | REPLACEMENT_MANAGE | 退回修正 |
| POST | `/{id}/resubmit` | REPLACEMENT_MANAGE | 補件重送 |

### 4.2 換裝明細 API（嵌套於工單路徑下）

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/{orderId}/items` | REPLACEMENT_VIEW | 查詢明細清單 |
| POST | `/{orderId}/items` | REPLACEMENT_MANAGE | 新增明細 |
| PUT | `/{orderId}/items/{itemId}` | REPLACEMENT_MANAGE | 更新明細 |
| DELETE | `/{orderId}/items/{itemId}` | REPLACEMENT_MANAGE | 刪除明細（僅 DRAFT） |

### 4.3 號碼牌 API (`/v1/auth/replacement/pole-numbers`)

| 方法 | 路徑 | 權限 | 說明 |
|---|---|---|---|
| GET | `/` | POLE_NUMBER_MANAGE | 號碼牌列表 |
| POST | `/` | POLE_NUMBER_MANAGE | 生成號碼牌 |
| GET | `/{id}/qr-code` | POLE_NUMBER_MANAGE | 取得單一 QR Code 圖片（PNG） |
| POST | `/qr-codes/batch-pdf` | POLE_NUMBER_MANAGE | 批次匯出 QR Code（PDF，A4 排版 4×5=20 個/頁） |

---

## 5. 業務邏輯

### 5.1 工單建立的雙路徑

**路徑一：從維修工單建立**
- `createFromRepair(repairTicketId, request)`
- 自動繼承維修工單的 contractId 和 deptId
- 適用於維修過程中發現需要換裝的情況

**路徑二：直接建立**
- `createDirect(request)`
- 手動指定所有欄位
- 適用於主動規劃的設備更新專案

兩種路徑建立後皆自動建立工作流實例（`REPLACEMENT_REVIEW` 流程），初始狀態為 `DRAFT`。

### 5.2 工單狀態機
```
DRAFT → DISPATCHED (派工至承商)
  → IN_PROGRESS (承商開工)
    → SELF_CHECKED (廠商自主檢核完成)
      → PENDING_REVIEW (報竣送審)
        → CLOSED (審核通過結案)
        → RETURNED (退回修正)
          → PENDING_REVIEW (補件重送)
```

### 5.3 自主檢核（核心流程）
自主檢核是換裝模組最關鍵的業務流程，在此階段實際執行設備置換：

1. 承商逐一處理每個明細項目（`SelfCheckRequest.items`）
2. 對每個明細：
   - 組裝 `ComponentReplaceRequest`，包含舊設備 ID 和新設備資訊
   - 呼叫 `DeviceService.replaceComponent()` 執行實際置換：
     - 舊設備除役（DECOMMISSIONED）
     - 建立新設備（繼承燈桿座標、部門、回路等）
     - 記錄設備事件（REPLACE / DECOMMISSION / INSTALL）
   - 回填 `new_device_id`、`after_spec`、`completed_at`、`completed_by`
   - 明細狀態設為 `COMPLETED`
3. 工單狀態轉為 `SELF_CHECKED`

### 5.4 換裝明細管理
- 新增明細時驗證舊設備確實屬於指定燈桿
- 自動快照舊設備的 device_type 和 attributes 至 `before_device_type`、`before_spec`
- 若指定 `approved_material_id`，驗證該合格材料狀態必須為 ACTIVE
- 明細僅在工單為 `DRAFT` 或 `IN_PROGRESS` 時可新增/修改
- 刪除僅限 `DRAFT` 狀態

### 5.5 號碼牌與 QR Code
- 號碼牌編號在同一租戶內唯一
- QR Code 內容為前端公開報修頁面 URL + pole 參數：`{FRONTEND_BASE_URL}/public/repair?pole={poleNumber}`
- 單一 QR Code 以 PNG 格式產生（300×300 像素，Error Correction Level M）
- 批次匯出為 A4 PDF，每頁排列 4 列 × 5 行 = 20 個 QR Code，含號碼牌編號標籤
- PDF 使用 Noto Sans TC 字型支援中文顯示

### 5.6 號碼牌與民眾報修的串接
民眾掃描號碼牌 QR Code → 開啟報修頁面（自動帶入 poleNumber）→ 提交報修 → 維修模組透過 `LightPoleNumberRepository.findByPoleNumber()` 查找對應的 tenantId 和 deviceId → 自動關聯設備。

---

## 6. 資料流

### 換裝工單完整生命週期
```
[維修工單 / 手動建立]
    │
    ▼
[ReplacementOrderService.create*()]
    │  ── 建立 ReplacementOrder (status=DRAFT)
    │  ── 建立 WorkflowInstance (REPLACEMENT_REVIEW)
    ▼
[新增換裝明細] ReplacementItemService.addItem()
    │  ── 驗證舊設備歸屬 + 合格材料狀態
    │  ── 快照舊設備規格
    ▼
[派工] dispatch() → status=DISPATCHED
    ▼
[開工] startWork() → status=IN_PROGRESS
    ▼
[自主檢核] selfCheck()
    │  ── 逐一執行 DeviceService.replaceComponent()
    │      ├── 舊設備除役
    │      ├── 建立新設備
    │      └── 記錄設備事件
    │  ── status=SELF_CHECKED
    ▼
[報竣送審] submitReview() → status=PENDING_REVIEW
    ▼
[審核]
    ├── approve() → workflow transition → CLOSED
    └── returnOrder() → status=RETURNED
                            └── resubmit() → status=PENDING_REVIEW
```

### 號碼牌 QR Code 流程
```
[管理員建立號碼牌]
    │
    ▼
[LightPoleNumberService.generate()]
    │  ── 檢查編號唯一性
    │  ── 組合 QR Code URL = FRONTEND_BASE_URL + /public/repair?pole={poleNumber}
    ▼
[匯出 QR Code]
    ├── 單一：getQrCodePng() → PNG 圖片
    └── 批次：exportQrCodesPdf() → PDF (A4, 4×5 排版)
            │
            ▼
        [列印 → 張貼於燈桿]
            │
            ▼
        [民眾掃碼 → 開啟報修頁面 → 維修模組]
```

---

## 7. 列舉定義

### ReplacementOrderType — 換裝類型
| 值 | 說明 |
|---|---|
| NEW_INSTALL | 新裝 |
| REPLACEMENT | 換裝（汰換既有設備） |
| RELOCATION | 遷移 |
| DECOMMISSION | 拆除 |
| ADJUSTMENT | 調整 |
| SHADE_INSTALL | 遮光罩安裝 |

### ReplacementOrderStatus — 工單狀態
| 值 | 說明 |
|---|---|
| DRAFT | 草稿 |
| DISPATCHED | 已派工 |
| IN_PROGRESS | 施工中 |
| SELF_CHECKED | 自主檢核完成 |
| PENDING_REVIEW | 報竣送審 |
| RETURNED | 退回修正 |
| CLOSED | 已結案 |

### ReplacementItemStatus — 明細狀態
| 值 | 說明 |
|---|---|
| PENDING | 待處理 |
| IN_PROGRESS | 進行中 |
| COMPLETED | 已完成 |
| SKIPPED | 已跳過 |

### PoleNumberStatus — 號碼牌狀態
| 值 | 說明 |
|---|---|
| ACTIVE | 使用中 |
| DECOMMISSIONED | 已除役 |
| LOST | 遺失 |
