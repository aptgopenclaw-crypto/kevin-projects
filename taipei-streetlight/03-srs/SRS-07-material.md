# SRS-07 材料管理

> **對應需求**：§7-(1) ~ §7-(2)  
> **設計參照**：`/02-spec/07-material-management.md`、`/_archive/x-plan/phase-3-material.md`、`/_archive/x-plan/cross-module-03-07-unified-design.md`  
> **狀態**：⚠️ 機關端核心已完成，廠商端查詢/分析待開發

---

## SRS-07-001 基本資料管理

**來源**：§7-(1)-A

### User Story

> 身為 **GOV_STAFF**（材料管理權限），我可管理庫別、材料規格、供應/承攬廠商等基本資料。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-07-001-1 | 庫別 CRUD：庫名、位置、負責人、狀態 |
| AC-07-001-2 | 材料規格 CRUD：6 大類（燈具/控制器/燈桿/號碼牌/線纜/其他），含規格屬性 |
| AC-07-001-3 | 供應商 CRUD：名稱、聯絡人、電話、地址、統編 |
| AC-07-001-4 | 所有基本資料支援停用（不刪除） |

### 資料模型

```
warehouses: id, name, location, manager_name, status, tenant_id
material_specs: id, category(LUMINAIRE/CONTROLLER/POLE/POLE_NUMBER/CABLE/OTHER),
                name, unit, description, attributes(JSONB), safety_stock, tenant_id
suppliers: id, name, contact_name, phone, address, tax_id, status, tenant_id
```

### API 端點

| Method | Path | 說明 |
|--------|------|------|
| CRUD | `/v1/material/warehouses` | 庫別管理 |
| CRUD | `/v1/material/specs` | 材料規格管理 |
| CRUD | `/v1/material/suppliers` | 供應商管理 |

### 狀態：✅ 已完成

---

## SRS-07-002 採購管理

**來源**：§7-(1)-B

### 主要流程

1. 建立採購單（PurchaseOrder），選擇供應商
2. 新增採購明細（PurchaseItem）：材料規格、數量、單價
3. 驗收完成後，建立收料紀錄（自動入庫）
4. 採購文件管理（附件上傳）

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-07-002-1 | 可建立採購單，含供應商、採購日期、預計交貨日 |
| AC-07-002-2 | 採購明細含：材料規格、數量、單價 |
| AC-07-002-3 | 驗收文件可上傳管理 |
| AC-07-002-4 | 採購狀態追蹤（草稿→已下單→已交貨→已驗收） |

### 資料模型

```
purchase_orders: id, supplier_id, order_date, expected_delivery_date, 
                 status(DRAFT/ORDERED/DELIVERED/ACCEPTED), total_amount, tenant_id
purchase_items: id, order_id, spec_id, quantity, unit_price
```

### 狀態：✅ 已完成

---

## SRS-07-003 收料管理

**來源**：§7-(1)-C

### 主要流程

1. 建立收料紀錄（ReceivingRecord），關聯採購單
2. 填入實收數量
3. 系統自動 UPSERT `inventory` 表：同一倉庫+規格，數量累加
4. 可執行轉庫作業（A 倉→B 倉）

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-07-003-1 | 可建立收料紀錄，關聯採購單 |
| AC-07-003-2 | 收料自動更新庫存數量 |
| AC-07-003-3 | 支援倉庫間轉庫作業 |

### 資料模型

```
receiving_records: id, purchase_order_id, warehouse_id, spec_id, 
                   quantity, received_at, received_by, tenant_id
inventory: id, warehouse_id, spec_id, quantity, tenant_id
           UNIQUE(warehouse_id, spec_id, tenant_id)
```

### 狀態：✅ 已完成

---

## SRS-07-004 庫存管理與安全庫存預警

**來源**：§7-(1)-D

### User Story

> 身為 **GOV_STAFF**，我可查看各倉庫庫存總覽；庫存低於安全庫存量時，系統自動預警。

### 主要流程

1. 庫存總覽：按倉庫/規格查看現有數量
2. 庫存盤點：實際盤點後輸入數量，系統自動計算差異
3. 安全庫存預警：每日 08:00 排程檢查，低於閾值推送通知
4. 庫存調整：手動調整（盤盈/盤虧/修正），需記錄原因

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-07-004-1 | 庫存總覽可依倉庫/規格篩選查詢 |
| AC-07-004-2 | 支援庫存盤點，自動計算差異量 |
| AC-07-004-3 | 材料規格可設定安全庫存量（safety_stock） |
| AC-07-004-4 | 低於安全庫存時自動預警通知（每日排程） |
| AC-07-004-5 | 庫存調整需記錄調整原因與操作人 |

### 資料模型

```
inventory_adjustments: id, warehouse_id, spec_id, adjustment_type(COUNT/TRANSFER/CORRECTION),
                       quantity_before, quantity_after, reason, adjusted_by, tenant_id
```

### 事件驅動

- **E12**：每日 @Scheduled 08:00 檢查安全庫存 → 推送通知至 MATERIAL_MANAGE 權限使用者

### 狀態：✅ 已完成

---

## SRS-07-005 出料管理

**來源**：§7-(1)-E

### 主要流程

1. 建立領料申請（IssueRequest）：案由、材料規格、數量、用途
2. 審核通過後產生出料紀錄（IssueRecord）
3. 系統自動扣減庫存
4. 庫存不足時拒絕出料

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-07-005-1 | 可建立領料申請，含案由、規格、數量 |
| AC-07-005-2 | 出料時驗證庫存是否充足 |
| AC-07-005-3 | 出料自動扣減庫存 |
| AC-07-005-4 | 庫存不足時拒絕並提示 |

### 資料模型

```
issue_requests: id, warehouse_id, spec_id, quantity, reason, 
                status(PENDING/APPROVED/ISSUED/REJECTED), requested_by, tenant_id
issue_records: id, request_id, warehouse_id, spec_id, quantity, issued_by, issued_at
```

### 事件驅動

- **E6**：換裝派工 → 自動建立 issue_request

### 狀態：✅ 已完成

---

## SRS-07-006 報核管理

**來源**：§7-(1)-F

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-07-006-1 | 費用支出表報（採購金額、數量匯總） |
| AC-07-006-2 | 廢品處理（繳庫）紀錄 |
| AC-07-006-3 | 報表可匯出 |

### 資料模型

```
disposal_records: id, warehouse_id, spec_id, quantity, disposal_type,
                  reason, disposed_by, disposed_at, tenant_id
```

### 狀態：✅ 已完成

---

## SRS-07-007 廠商材料查詢/匯出/統計分析

**來源**：§7-(2)

### User Story

> 身為 **GOV_STAFF / PM**，我可在平台內直接查詢各得標廠商的材料規格、各月使用量、安全庫存，並統計分析材料壽命與品質。

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-07-007-1 | 可查詢各廠商所有材料規格清單 |
| AC-07-007-2 | 可查看各材料各月/各年使用數量 |
| AC-07-007-3 | 可查看安全庫存量與當前庫存比較 |
| AC-07-007-4 | 可匯出材料資料（ODS/XLS/CSV） |
| AC-07-007-5 | 可統計分析材料使用壽命 |
| AC-07-007-6 | 可統計分析材料品質（故障率） |
| AC-07-007-7 | 可分析備存需求（基於消耗趨勢） |
| AC-07-007-8 | 以上功能為平台內直接操作（非外部連結） |

### 狀態：❌ 未開始
