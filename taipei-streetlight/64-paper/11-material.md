# 11. 材料管理模組 (Material Module)

## 1. 模組概述

材料管理模組負責台北市路燈管理系統中所有實體材料的全生命週期管理，涵蓋材料規格定義、廠商管理、倉庫管理、採購訂單、驗收入庫、領料出庫、庫存調整、報廢處置及審驗材料管理等功能。

本模組採用多租戶（Multi-Tenant）架構，所有核心實體均實作 `TenantAware` 介面，透過 Hibernate `@Filter` 機制自動隔離租戶資料。系統內建事件驅動的低庫存預警機制，每日 08:00 自動檢查安全庫存並發布 `LowStockAlertEvent`。

**套件路徑：** `com.taipei.iot.material`

**子套件結構：**
- `entity` — JPA 實體
- `dto` — 請求/回應 DTO
- `enums` — 列舉定義
- `service` — 業務邏輯層
- `controller` — REST API 控制層
- `repository` — 資料存取層
- `event` — 事件（LowStockAlertEvent）

---

## 2. 資料表結構

### 2.1 material_specs（材料規格）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| spec_code | VARCHAR(100) | NOT NULL | 規格代碼（租戶內唯一） |
| spec_name | VARCHAR(300) | NOT NULL | 規格名稱 |
| category | VARCHAR(50) | NOT NULL | 材料類別（MaterialCategory） |
| unit | VARCHAR(20) | NOT NULL | 單位，預設 "PCS" |
| attributes | JSONB | | 擴充屬性（JSON） |
| status | VARCHAR(20) | NOT NULL | 狀態（MaterialStatus） |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |
| updated_at | TIMESTAMP | | 最後更新時間 |

### 2.2 warehouses（倉庫）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| warehouse_code | VARCHAR(50) | NOT NULL | 庫別代碼 |
| warehouse_name | VARCHAR(200) | NOT NULL | 庫別名稱 |
| location | VARCHAR(500) | | 位置描述 |
| status | VARCHAR(20) | NOT NULL | 狀態（WarehouseStatus） |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |
| updated_at | TIMESTAMP | | 最後更新時間 |

### 2.3 suppliers（廠商）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| supplier_code | VARCHAR(100) | NOT NULL | 廠商代碼 |
| supplier_name | VARCHAR(300) | NOT NULL | 廠商名稱 |
| contact_name | VARCHAR(100) | | 聯絡人姓名 |
| contact_phone | VARCHAR(50) | | 聯絡電話 |
| contact_email | VARCHAR(200) | | 聯絡信箱 |
| address | TEXT | | 地址 |
| status | VARCHAR(20) | NOT NULL | 狀態（SupplierStatus） |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |
| updated_at | TIMESTAMP | | 最後更新時間 |

### 2.4 inventory（庫存）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| warehouse_id | BIGINT | NOT NULL, FK → warehouses | 所屬倉庫 |
| material_spec_id | BIGINT | NOT NULL, FK → material_specs | 材料規格 |
| quantity_on_hand | INT | NOT NULL | 現有庫存量 |
| safety_stock | INT | NOT NULL | 安全庫存量 |
| updated_at | TIMESTAMP | | 最後更新時間 |

### 2.5 purchase_orders（採購訂單）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| po_number | VARCHAR(100) | NOT NULL | 訂單編號（格式：PO-yyyyMMdd-NNN） |
| supplier_id | BIGINT | FK → suppliers | 廠商 |
| contract_id | BIGINT | | 關聯合約 ID |
| order_date | DATE | NOT NULL | 訂單日期 |
| status | VARCHAR(20) | NOT NULL | 訂單狀態（PurchaseOrderStatus） |
| total_amount | DECIMAL(12,2) | | 總金額 |
| notes | TEXT | | 備註 |
| created_by | VARCHAR(50) | | 建立者 |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |
| updated_at | TIMESTAMP | | 最後更新時間 |

### 2.6 purchase_items（採購明細）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| po_id | BIGINT | NOT NULL, FK → purchase_orders | 所屬訂單 |
| material_spec_id | BIGINT | NOT NULL, FK → material_specs | 材料規格 |
| quantity | INT | NOT NULL | 數量 |
| unit_price | DECIMAL(10,2) | | 單價 |
| notes | TEXT | | 備註 |

### 2.7 receiving_records（驗收紀錄）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| po_id | BIGINT | FK → purchase_orders | 關聯採購單（可為 null） |
| warehouse_id | BIGINT | NOT NULL | 入庫倉庫 |
| material_spec_id | BIGINT | NOT NULL | 材料規格 |
| quantity | INT | NOT NULL | 驗收數量 |
| received_date | DATE | NOT NULL | 驗收日期 |
| delivery_note | VARCHAR(200) | | 送貨單號 |
| received_by | VARCHAR(50) | | 驗收人 |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |

### 2.8 issue_requests（領料申請）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| request_number | VARCHAR(100) | NOT NULL | 申請單號（格式：IR-yyyyMMdd-NNN） |
| repair_ticket_id | BIGINT | | 關聯維修工單 ID |
| replacement_order_id | BIGINT | | 關聯換裝工單 ID |
| requested_by | VARCHAR(50) | NOT NULL | 申請人 |
| status | VARCHAR(20) | NOT NULL | 狀態（IssueRequestStatus） |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |
| updated_at | TIMESTAMP | | 最後更新時間 |

### 2.9 issue_records（領料紀錄）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| request_id | BIGINT | NOT NULL, FK → issue_requests | 關聯領料申請 |
| inventory_id | BIGINT | NOT NULL | 扣庫庫存記錄 |
| material_spec_id | BIGINT | NOT NULL | 材料規格 |
| quantity | INT | NOT NULL | 領出數量 |
| issued_by | VARCHAR(50) | | 發料人 |
| issued_at | TIMESTAMP | NOT NULL | 發料時間 |

### 2.10 disposal_records（報廢/退庫紀錄）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| material_spec_id | BIGINT | NOT NULL | 材料規格 |
| quantity | INT | NOT NULL | 數量 |
| disposal_type | VARCHAR(20) | NOT NULL | 處置類型（DisposalType） |
| reason | TEXT | | 原因 |
| disposed_by | VARCHAR(50) | | 處理人 |
| disposed_at | TIMESTAMP | NOT NULL | 處理時間 |

### 2.11 inventory_adjustments（庫存調整）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| inventory_id | BIGINT | NOT NULL | 調整的庫存記錄 |
| adjustment_type | VARCHAR(20) | NOT NULL | 調整類型（AdjustmentType） |
| quantity_change | INT | NOT NULL | 數量變化（正/負） |
| reason | TEXT | | 調整原因 |
| adjusted_by | VARCHAR(50) | | 調整人 |
| adjusted_at | TIMESTAMP | NOT NULL | 調整時間 |

### 2.12 approved_materials（審驗材料）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主鍵 |
| tenant_id | VARCHAR(50) | NOT NULL | 租戶識別碼 |
| material_spec_id | BIGINT | NOT NULL, FK → material_specs | 材料規格 |
| contract_id | BIGINT | | 關聯合約 ID |
| material_number | VARCHAR(100) | NOT NULL | 材料編號（租戶內唯一） |
| approval_date | DATE | NOT NULL | 審驗日期 |
| batch_number | VARCHAR(100) | | 批次號 |
| brand | VARCHAR(200) | | 品牌 |
| model | VARCHAR(200) | | 型號 |
| spec_details | JSONB | | 規格細節（JSON） |
| status | VARCHAR(20) | NOT NULL | 狀態（ApprovedMaterialStatus） |
| created_at | TIMESTAMP | NOT NULL, 不可更新 | 建立時間 |

---

## 3. 實體關聯

```
MaterialSpec (1) ──< (N) Inventory
Warehouse    (1) ──< (N) Inventory
Supplier     (1) ──< (N) PurchaseOrder
PurchaseOrder(1) ──< (N) PurchaseItem
PurchaseItem (N) >── (1) MaterialSpec
PurchaseOrder(1) ──< (N) ReceivingRecord
IssueRequest (1) ──< (N) IssueRecord
MaterialSpec (1) ──< (N) ApprovedMaterial
```

**跨模組關聯：**
- `PurchaseOrder.contractId` → 合約模組 `Contract`
- `IssueRequest.repairTicketId` → 維修模組 `RepairTicket`
- `IssueRequest.replacementOrderId` → 換裝模組 `ReplacementOrder`
- `ApprovedMaterial.contractId` → 合約模組 `Contract`

---

## 4. API 端點摘要

### 4.1 材料規格 `/v1/auth/material/specs`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | MATERIAL_VIEW | 分頁查詢（可篩選 category, status, keyword） |
| GET | `/{id}` | MATERIAL_VIEW | 取得單筆 |
| POST | `/` | MATERIAL_MANAGE | 新增規格 |
| PUT | `/{id}` | MATERIAL_MANAGE | 更新規格 |

### 4.2 倉庫 `/v1/auth/material/warehouses`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | MATERIAL_VIEW | 分頁查詢（可篩選 status, keyword） |
| GET | `/active` | MATERIAL_VIEW | 取得啟用中倉庫清單 |
| POST | `/` | MATERIAL_MANAGE | 新增倉庫 |
| PUT | `/{id}` | MATERIAL_MANAGE | 更新倉庫 |
| DELETE | `/{id}` | MATERIAL_MANAGE | 停用倉庫（軟刪除） |

### 4.3 廠商 `/v1/auth/material/suppliers`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | MATERIAL_VIEW | 分頁查詢（可篩選 status, keyword） |
| GET | `/active` | MATERIAL_VIEW | 取得啟用中廠商清單 |
| POST | `/` | MATERIAL_MANAGE | 新增廠商 |
| PUT | `/{id}` | MATERIAL_MANAGE | 更新廠商 |

### 4.4 庫存 `/v1/auth/material/inventory`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | INVENTORY_VIEW | 分頁查詢（可篩選 warehouseId, category, keyword, belowSafetyStock） |
| GET | `/summary` | INVENTORY_VIEW | 依類別匯總庫存 |
| GET | `/alerts` | INVENTORY_VIEW | 取得低於安全庫存的項目 |

### 4.5 庫存調整 `/v1/auth/material/adjustments`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | INVENTORY_VIEW | 查詢調整紀錄（可篩選 type） |
| POST | `/count` | INVENTORY_MANAGE | 盤點（設定實際數量） |
| POST | `/transfer` | INVENTORY_MANAGE | 調撥（跨倉庫移轉） |
| POST | `/correction` | INVENTORY_MANAGE | 修正（增/減庫存） |

### 4.6 採購訂單 `/v1/auth/material/purchase-orders`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | MATERIAL_VIEW | 分頁查詢（可篩選 status, keyword） |
| GET | `/{id}` | MATERIAL_VIEW | 取得單筆（含明細） |
| POST | `/` | MATERIAL_MANAGE | 新增訂單（含明細） |
| PUT | `/{id}` | MATERIAL_MANAGE | 更新訂單（僅 DRAFT 狀態） |
| POST | `/{id}/submit` | MATERIAL_MANAGE | 送審 |

### 4.7 驗收 `/v1/auth/material/receiving`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | MATERIAL_VIEW | 查詢驗收紀錄（可篩選 poId） |
| POST | `/` | MATERIAL_MANAGE | 驗收入庫 |

### 4.8 領料 `/v1/auth/material/issue-requests`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | MATERIAL_VIEW | 分頁查詢（可篩選 status, keyword） |
| POST | `/` | MATERIAL_MANAGE | 建立領料申請 |
| POST | `/{id}/approve` | MATERIAL_MANAGE | 核准 |
| POST | `/{id}/reject` | MATERIAL_MANAGE | 駁回 |
| POST | `/{id}/issue` | MATERIAL_MANAGE | 執行發料（批次扣庫） |

### 4.9 報廢/退庫 `/v1/auth/material/disposals`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | MATERIAL_VIEW | 分頁查詢 |
| POST | `/` | MATERIAL_MANAGE | 新增處置紀錄 |

### 4.10 審驗材料 `/v1/auth/material/approved-materials`

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| GET | `/` | MATERIAL_VIEW | 分頁查詢（可篩選 status, materialSpecId, keyword） |
| GET | `/{id}` | MATERIAL_VIEW | 取得單筆 |
| POST | `/` | MATERIAL_MANAGE | 新增 |
| PUT | `/{id}` | MATERIAL_MANAGE | 更新 |
| POST | `/import` | MATERIAL_MANAGE | CSV 批次匯入 |

---

## 5. 業務邏輯

### 5.1 採購流程
1. 建立採購單（狀態 `DRAFT`），自動計算 `totalAmount = Σ(unitPrice × quantity)`
2. 僅 `DRAFT` 狀態可編輯或送審
3. 送審後狀態變為 `SUBMITTED`
4. 驗收時若關聯了 PO，自動將狀態轉為 `RECEIVING`

### 5.2 驗收入庫（UPSERT 機制）
- 驗收時以 `(tenant_id, warehouse_id, material_spec_id)` 查找庫存記錄
- 若不存在則新建（初始 quantityOnHand=0, safetyStock=0）
- 將驗收數量加到 `quantityOnHand`

### 5.3 領料流程
1. 建立領料申請（狀態 `PENDING`），自動生成單號 `IR-yyyyMMdd-NNN`
2. 核准 → `APPROVED`；駁回 → `REJECTED`
3. 執行發料時驗證 `APPROVED` 狀態，逐項檢查庫存充足性
4. 扣減庫存後狀態變為 `ISSUED`
5. 可由換裝模組自動觸發建立（`createFromReplacement`）

### 5.4 庫存調整
- **盤點（COUNT）：** 設定實際數量，記錄差異值
- **調撥（TRANSFER）：** 來源倉庫扣減，目標倉庫增加（UPSERT），產生兩筆調整紀錄
- **修正（CORRECTION）：** 直接增減庫存數量

### 5.5 低庫存預警
- 排程任務：每日 08:00（`cron: 0 0 8 * * *`）
- 跨租戶查詢所有 `quantityOnHand < safetyStock` 的庫存
- 對每筆觸發 `LowStockAlertEvent`（含 tenantId, specName, warehouseName, 現有量, 安全量）

### 5.6 審驗材料 CSV 匯入
- 以 `material_number` 判斷是否重複（同租戶內），重複則跳過
- 以 `spec_code` 反查 `MaterialSpec`，不存在則記錄錯誤
- 回傳匯入結果：成功數、跳過數、錯誤清單

### 5.7 審計追蹤
以下操作透過 `@AuditEvent` 記錄審計日誌：
- `IMPORT_APPROVED_MATERIAL` — 批次匯入審驗材料
- `DISPOSE_MATERIAL` — 報廢/退庫
- `ADJUST_INVENTORY` — 庫存調整（盤點/調撥/修正）
- `ISSUE_MATERIAL` — 發料
- `RECEIVE_MATERIAL` — 驗收入庫
- `CREATE_PURCHASE_ORDER` — 建立採購單

---

## 6. 資料流

### 6.1 採購到入庫流程

```
建立採購單(DRAFT)
    │
    ▼
送審(SUBMITTED) ──→ [外部核准] ──→ (APPROVED)
    │
    ▼
驗收入庫(ReceivingService.receive)
    ├─→ 建立 ReceivingRecord
    ├─→ UPSERT Inventory（增加 quantityOnHand）
    └─→ 更新 PO 狀態為 RECEIVING
```

### 6.2 領料出庫流程

```
[維修工單/換裝工單] ──或── [手動建立]
    │
    ▼
IssueRequest(PENDING)
    │
    ├──→ approve() ──→ APPROVED
    │                      │
    │                      ▼
    │               issue(items)
    │                 ├─→ 檢查庫存充足性
    │                 ├─→ 扣減 Inventory.quantityOnHand
    │                 ├─→ 建立 IssueRecord(s)
    │                 └─→ 狀態 → ISSUED
    │
    └──→ reject() ──→ REJECTED
```

### 6.3 庫存調撥流程

```
transfer(from_inventory, to_warehouse, quantity)
    │
    ├─→ 來源庫存 quantityOnHand -= quantity
    │   └─→ 記錄 InventoryAdjustment (TRANSFER, -quantity)
    │
    └─→ 目標庫存 UPSERT → quantityOnHand += quantity
        └─→ 記錄 InventoryAdjustment (TRANSFER, +quantity)
```

### 6.4 低庫存預警流程

```
[每日 08:00 排程]
    │
    ▼
查詢所有租戶 quantityOnHand < safetyStock
    │
    ▼
逐筆發布 LowStockAlertEvent
    │
    ▼
[EventListener 處理：通知/Email/etc.]
```

---

## 7. 列舉值定義

### MaterialCategory（材料類別）

| 值 | 說明 |
|---|------|
| LUMINAIRE | 燈具 |
| CONTROLLER | 控制器 |
| POLE | 燈桿 |
| POLE_NUMBER | 桿號牌 |
| CABLE | 電纜 |
| ACCESSORY | 配件 |
| OTHER | 其他 |

### MaterialStatus（材料狀態）

| 值 | 說明 |
|---|------|
| ACTIVE | 啟用中 |
| DEPRECATED | 已停用 |

### WarehouseStatus（倉庫狀態）

| 值 | 說明 |
|---|------|
| ACTIVE | 啟用中 |
| INACTIVE | 已停用 |

### SupplierStatus（廠商狀態）

| 值 | 說明 |
|---|------|
| ACTIVE | 啟用中 |
| INACTIVE | 已停用 |

### PurchaseOrderStatus（採購訂單狀態）

| 值 | 說明 |
|---|------|
| DRAFT | 草稿 |
| SUBMITTED | 已送審 |
| APPROVED | 已核准 |
| RECEIVING | 驗收中 |
| COMPLETED | 已完成 |
| CANCELLED | 已取消 |

### IssueRequestStatus（領料申請狀態）

| 值 | 說明 |
|---|------|
| PENDING | 待審核 |
| APPROVED | 已核准 |
| ISSUED | 已發料 |
| REJECTED | 已駁回 |

### AdjustmentType（庫存調整類型）

| 值 | 說明 |
|---|------|
| COUNT | 盤點 |
| TRANSFER | 調撥 |
| CORRECTION | 修正 |
| DISPOSAL | 報廢 |

### DisposalType（處置類型）

| 值 | 說明 |
|---|------|
| RETURN_WAREHOUSE | 退回倉庫 |
| SCRAP | 報廢 |

### ApprovedMaterialStatus（審驗材料狀態）

| 值 | 說明 |
|---|------|
| ACTIVE | 有效 |
| EXPIRED | 已過期 |
| REVOKED | 已撤銷 |
