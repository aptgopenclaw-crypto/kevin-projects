# WBS 1.3 — Phase 3：材料管理

> **狀態**：✅ 已完成  
> **Flyway**：V40–V41  
> **SRS 對應**：SRS-07-001~007  
> **x-plan**：`/_archive/x-plan/phase-3-material.md`

---

## 1.3.1 資料庫 Schema

| WBS ID | 工作包 | Flyway | 產出 | 狀態 |
|--------|-------|--------|------|------|
| 1.3.1.1 | 基礎資料 DDL | V40 | `warehouses`, `material_specs`, `suppliers`, `inventory`, `approved_materials` | ✅ |
| 1.3.1.2 | 營運資料 DDL | V41 | `purchase_orders`, `purchase_items`, `receiving_records`, `issue_requests`, `issue_records`, `inventory_adjustments`, `disposal_records` + menu + permission | ✅ |

---

## 1.3.2 後端 — 基礎資料 (`com.taipei.iot.material`)

### 1.3.2.1 Entity 層

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.3.2.1.1 | Warehouse | `TenantAware` | ✅ |
| 1.3.2.1.2 | MaterialSpec | `TenantAware` + JSONB specs | ✅ |
| 1.3.2.1.3 | Supplier | `TenantAware` | ✅ |
| 1.3.2.1.4 | Inventory | `TenantAware`, FK → warehouse + materialSpec | ✅ |
| 1.3.2.1.5 | ApprovedMaterial | `TenantAware`, 批次匯入 | ✅ |
| 1.3.2.1.6 | 列舉型別 | MaterialCategory / WarehouseType / SupplierStatus / ApprovedMaterialStatus / PurchaseOrderStatus / ReceivingType / IssueType / AdjustmentType / DisposalType | ✅ |

### 1.3.2.2 Repository 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.3.2.2.1 | WarehouseRepository | ✅ |
| 1.3.2.2.2 | MaterialSpecRepository | ✅ |
| 1.3.2.2.3 | SupplierRepository | ✅ |
| 1.3.2.2.4 | InventoryRepository | ✅ |
| 1.3.2.2.5 | ApprovedMaterialRepository | ✅ |

### 1.3.2.3 DTO 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.3.2.3.1 | WarehouseRequest / Response | ✅ |
| 1.3.2.3.2 | MaterialSpecRequest / Response | ✅ |
| 1.3.2.3.3 | SupplierRequest / Response | ✅ |
| 1.3.2.3.4 | InventoryResponse / LowStockAlert | ✅ |
| 1.3.2.3.5 | ApprovedMaterialRequest / Response / BatchImportRequest | ✅ |

### 1.3.2.4 Service 層

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.3.2.4.1 | WarehouseService | CRUD | ✅ |
| 1.3.2.4.2 | MaterialSpecService | CRUD + JSONB specs | ✅ |
| 1.3.2.4.3 | SupplierService | CRUD | ✅ |
| 1.3.2.4.4 | InventoryService | 查詢 + 安全庫存預警 (E12) | ✅ |
| 1.3.2.4.5 | ApprovedMaterialService | CRUD + 批次匯入 + ACTIVE 查驗 | ✅ |

### 1.3.2.5 Controller 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.3.2.5.1 | WarehouseController | ✅ |
| 1.3.2.5.2 | MaterialSpecController | ✅ |
| 1.3.2.5.3 | SupplierController | ✅ |
| 1.3.2.5.4 | InventoryController | ✅ |
| 1.3.2.5.5 | ApprovedMaterialController | ✅ |

---

## 1.3.3 後端 — 營運作業

### 1.3.3.1 Entity 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.3.3.1.1 | PurchaseOrder + PurchaseItem | `TenantAware` | ✅ |
| 1.3.3.1.2 | ReceivingRecord | `TenantAware`, auto-UPSERT inventory | ✅ |
| 1.3.3.1.3 | IssueRequest + IssueRecord | `TenantAware` | ✅ |
| 1.3.3.1.4 | InventoryAdjustment | 盤點/調撥 | ✅ |
| 1.3.3.1.5 | DisposalRecord | 報廢 | ✅ |

### 1.3.3.2 Repository 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.3.3.2.1 | PurchaseOrderRepository | ✅ |
| 1.3.3.2.2 | PurchaseItemRepository | ✅ |
| 1.3.3.2.3 | ReceivingRecordRepository | ✅ |
| 1.3.3.2.4 | IssueRequestRepository | ✅ |
| 1.3.3.2.5 | IssueRecordRepository | ✅ |
| 1.3.3.2.6 | InventoryAdjustmentRepository | ✅ |
| 1.3.3.2.7 | DisposalRecordRepository | ✅ |

### 1.3.3.3 Service 層

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.3.3.3.1 | PurchaseOrderService | 採購 CRUD + 送審 + 核准 | ✅ |
| 1.3.3.3.2 | ReceivingService | 收料入庫 + auto UPSERT inventory | ✅ |
| 1.3.3.3.3 | IssueService | 領料出庫 + confirmDeduction 扣庫 | ✅ |
| 1.3.3.3.4 | InventoryAdjustmentService | 盤點 + 調撥 | ✅ |
| 1.3.3.3.5 | DisposalService | 報廢處理 | ✅ |

### 1.3.3.4 Controller 層

| WBS ID | 工作包 | 狀態 |
|--------|-------|------|
| 1.3.3.4.1 | PurchaseOrderController | ✅ |
| 1.3.3.4.2 | ReceivingController | ✅ |
| 1.3.3.4.3 | IssueController | ✅ |
| 1.3.3.4.4 | InventoryAdjustmentController | ✅ |

---

## 1.3.4 前端 — 材料管理

| WBS ID | 工作包 | 檔案 | 狀態 |
|--------|-------|------|------|
| 1.3.4.1 | TypeScript 型別 | `types/material.ts` (11 enum + interfaces) | ✅ |
| 1.3.4.2 | API 模組 | `api/material/index.ts` | ✅ |
| 1.3.4.3 | Store | `stores/materialStore.ts` (Pinia + 快取) | ✅ |
| 1.3.4.4 | 材料規格頁面 | `MaterialSpecView.vue` | ✅ |
| 1.3.4.5 | 倉庫管理頁面 | `WarehouseView.vue` | ✅ |
| 1.3.4.6 | 供應商管理頁面 | `SupplierView.vue` | ✅ |
| 1.3.4.7 | 庫存管理頁面 | `InventoryView.vue` | ✅ |
| 1.3.4.8 | 採購單頁面 | `PurchaseOrderView.vue` | ✅ |
| 1.3.4.9 | 合格材料頁面 | `ApprovedMaterialView.vue` | ✅ |
| 1.3.4.10 | 收料入庫頁面 | `ReceivingView.vue` | ✅ |
| 1.3.4.11 | 領料申請頁面 | `IssueRequestView.vue` | ✅ |
| 1.3.4.12 | 盤點調整頁面 | `AdjustmentView.vue` | ✅ |
| 1.3.4.13 | 報廢處理頁面 | `DisposalView.vue` | ✅ |
| 1.3.4.14 | 共用元件 | `MaterialSelector.vue` (remote-search select) | ✅ |
| 1.3.4.15 | 路由 | 10 routes under `/admin/material/*` | ✅ |
| 1.3.4.16 | 國際化 | ~120 keys (zh-TW/en/zh-CN) | ✅ |

---

## 1.3.5 單元測試

| WBS ID | 工作包 | 測試類 | 狀態 |
|--------|-------|--------|------|
| 1.3.5.1 | 倉庫 Service | WarehouseServiceTest | ✅ |
| 1.3.5.2 | 材料規格 Service | MaterialSpecServiceTest | ✅ |
| 1.3.5.3 | 庫存 Service | InventoryServiceTest | ✅ |
| 1.3.5.4 | 採購 Service | PurchaseOrderServiceTest | ✅ |
| 1.3.5.5 | 領料 Service | IssueServiceTest | ✅ |
| 1.3.5.6 | 合格材料 Service | ApprovedMaterialServiceTest | ✅ |

---

## 1.3.6 共用基礎設施

| WBS ID | 工作包 | 說明 | 狀態 |
|--------|-------|------|------|
| 1.3.6.1 | AuditCategory 擴充 | MATERIAL | ✅ |
| 1.3.6.2 | AuditEventType 擴充 | 6 個 material event types | ✅ |
| 1.3.6.3 | ErrorCode 擴充 | 85xxx 系列 (11 個) | ✅ |
| 1.3.6.4 | Menu + Permission Seed | V41 menu + permission + role binding | ✅ |
