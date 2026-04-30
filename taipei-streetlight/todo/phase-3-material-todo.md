# Phase 3 — 材料管理（庫存 + 採購 + 領料）TODO

> **建立日期**: 2026-04-27  
> **最後更新**: 2026-04-27 (全部完成)  
> **甘特圖**: 04/06 – 04/25 (3 週)  
> **前置**: Phase 1 地基層（設備表），與 Phase 2 並行  
> **執行計畫**: 99-plan/2026-04-24-gantt.md §Phase 3  
> **關鍵路徑**: Phase 3 → Phase 4（E6 領料整合）  
> **里程碑**: m0c — 2026-04-25 (Phase 2+3 完成)  
> **WBS**: 04-wbs/phase-3.md  
> **SRS 對應**: SRS-07-001~007

---

### 進度總覽

| 區塊 | Gantt ID | 工期 | 進度 | 說明 |
|------|----------|------|------|------|
| 3a DB Schema (V40–V41, 12 tables) | 3a | 2d | ✅ | 12 tables + menu/permission |
| 3b 基礎資料 (倉庫/規格/供應商/庫存) | 3b | 5d | ✅ | 5 controllers + 安全庫存預警 |
| 3c 營運作業 (採購/收料/領料/盤點) | 3c | 8d | ✅ | 4 controllers + auto UPSERT inventory |
| 3d 前端 13 頁面 | 3d | 10d | ✅ | 13 Vue pages + MaterialSelector |
| 3e 單元測試 (6 類) | 3e | 3d | ✅ | 6 test classes |

### 甘特依賴

```
3a DB Schema (2d) ──→ 3b 基礎資料 (5d) ──→ 3c 營運作業 (8d) ──→ 3e 測試 (3d)
       │
       └──→ 3d 前端 13 頁面 (10d, 與後端並行) ──────────────────────┘
```

---

## 3a — DB Schema V40–V41 (2 天) ✅ 已完成

> **SD**: SD-06-material.md

### Flyway Migrations

- [x] **V40** — 基礎資料 DDL: `warehouses`, `material_specs`, `suppliers`, `inventory`, `approved_materials`
- [x] **V41** — 營運資料 DDL: `purchase_orders`, `purchase_items`, `receiving_records`, `issue_requests`, `issue_records`, `inventory_adjustments`, `disposal_records` + menu + permission

### 表格統計 (12 tables)

| 表 | 分類 | 說明 |
|----|------|------|
| warehouses | 基礎 | 倉庫 |
| material_specs | 基礎 | 材料規格 + JSONB specs |
| suppliers | 基礎 | 供應商 |
| inventory | 基礎 | 庫存量 (warehouse × materialSpec) |
| approved_materials | 基礎 | 合格材料清單 |
| purchase_orders | 營運 | 採購單 |
| purchase_items | 營運 | 採購項目 |
| receiving_records | 營運 | 收料入庫 |
| issue_requests | 營運 | 領料申請 |
| issue_records | 營運 | 領料紀錄 |
| inventory_adjustments | 營運 | 盤點/調撥 |
| disposal_records | 營運 | 報廢處理 |

---

## 3b — 基礎資料 (5 天) ✅ 已完成

> **Package**: `com.taipei.iot.material`  
> **SD**: SD-06-material.md

### Entity

- [x] Warehouse — TenantAware
- [x] MaterialSpec — TenantAware + JSONB specs
- [x] Supplier — TenantAware
- [x] Inventory — TenantAware, FK → warehouse + materialSpec
- [x] ApprovedMaterial — TenantAware, 批次匯入
- [x] 列舉: MaterialCategory / WarehouseType / SupplierStatus / ApprovedMaterialStatus / PurchaseOrderStatus / ReceivingType / IssueType / AdjustmentType / DisposalType

### Service

- [x] WarehouseService — CRUD
- [x] MaterialSpecService — CRUD + JSONB specs
- [x] SupplierService — CRUD
- [x] InventoryService — 查詢 + 安全庫存預警 (E12)
- [x] ApprovedMaterialService — CRUD + 批次匯入 + ACTIVE 查驗

### Controller

- [x] WarehouseController
- [x] MaterialSpecController
- [x] SupplierController
- [x] InventoryController
- [x] ApprovedMaterialController

---

## 3c — 營運作業 (8 天) ✅ 已完成

> **Package**: `com.taipei.iot.material`  
> **SD**: SD-06-material.md

### Entity

- [x] PurchaseOrder + PurchaseItem — TenantAware
- [x] ReceivingRecord — TenantAware, auto-UPSERT inventory
- [x] IssueRequest + IssueRecord — TenantAware
- [x] InventoryAdjustment — 盤點/調撥
- [x] DisposalRecord — 報廢

### Service

- [x] PurchaseOrderService — 採購 CRUD + 送審 + 核准
- [x] ReceivingService — 收料入庫 + auto UPSERT inventory
- [x] IssueService — 領料出庫 + confirmDeduction 扣庫
- [x] InventoryAdjustmentService — 盤點 + 調撥
- [x] DisposalService — 報廢處理

### Controller

- [x] PurchaseOrderController
- [x] ReceivingController
- [x] IssueController
- [x] InventoryAdjustmentController

---

## 3d — 前端 13 頁面 (10 天) ✅ 已完成

- [x] TypeScript 型別: `types/material.ts` (11 enum + interfaces)
- [x] API 模組: `api/material/index.ts`
- [x] Store: `stores/materialStore.ts` (Pinia + 快取)
- [x] MaterialSpecView.vue — 材料規格
- [x] WarehouseView.vue — 倉庫管理
- [x] SupplierView.vue — 供應商管理
- [x] InventoryView.vue — 庫存管理
- [x] PurchaseOrderView.vue — 採購單
- [x] ApprovedMaterialView.vue — 合格材料
- [x] ReceivingView.vue — 收料入庫
- [x] IssueRequestView.vue — 領料申請
- [x] AdjustmentView.vue — 盤點調整
- [x] DisposalView.vue — 報廢處理
- [x] MaterialSelector.vue — 共用 remote-search select
- [x] 10 routes under `/admin/material/*`
- [x] 國際化: ~120 keys (zh-TW/en/zh-CN)

---

## 3e — 單元測試 (3 天) ✅ 已完成

| # | 測試類 | 狀態 |
|---|--------|------|
| 1 | WarehouseServiceTest | ✅ |
| 2 | MaterialSpecServiceTest | ✅ |
| 3 | InventoryServiceTest | ✅ |
| 4 | PurchaseOrderServiceTest | ✅ |
| 5 | IssueServiceTest | ✅ |
| 6 | ApprovedMaterialServiceTest | ✅ |

---

## 共用基礎設施 ✅ 已完成

- [x] AuditCategory 擴充: MATERIAL
- [x] AuditEventType 擴充: 6 個 material event types
- [x] ErrorCode 擴充: 85xxx 系列 (11 個)
- [x] Menu + Permission Seed: V41 menu + permission + role binding
