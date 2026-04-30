# VR-P3 — Phase 3 材料管理驗證紀錄

> **範圍**：SA-06 材料管理  
> **FN 總數**：45

---

## 最新執行摘要

| 項目 | 值 |
|------|-----|
| 驗證日期 | 2026-04-24 |
| Git Commit | `61b4ced` |
| 涵蓋 Test Classes | 6 |
| 涵蓋 Test Cases | 26 |
| Failures | 0 |
| 已驗 FN | 10 / 45 |
| 未驗 FN（已實作） | 16 |
| 未實作 FN | 19 |

---

## 測試對應

| Test Class | Tests | Result | 涵蓋 FN |
|-----------|-------|--------|---------|
| WarehouseServiceTest | 5 | ✅ PASS | FN-06-001~004 |
| PurchaseOrderServiceTest | 5 | ✅ PASS | FN-06-017~020 |
| ReceivingServiceTest | 2 | ✅ PASS | FN-06-023~024 |
| InventoryServiceTest | 4 | ✅ PASS | FN-06-026, 030, 031 |
| InventoryAdjustmentServiceTest | 4 | ✅ PASS | FN-06-025, 027, 028 |
| IssueServiceTest | 6 | ✅ PASS | FN-06-032~035 |

---

## FN 驗證明細

| FN | 功能 | 驗證方式 | 測試類別 | 結果 | 日期 | 備註 |
|----|------|---------|---------|------|------|------|
| FN-06-001 | 倉庫列表 | Unit | WarehouseServiceTest | ✅ PASS | 04-24 | 缺 Controller Test |
| FN-06-002 | 新增倉庫 | Unit | WarehouseServiceTest | ✅ PASS | 04-24 | |
| FN-06-003 | 編輯倉庫 | Unit | WarehouseServiceTest | ✅ PASS | 04-24 | |
| FN-06-004 | 刪除倉庫 | Unit | WarehouseServiceTest | ✅ PASS | 04-24 | |
| FN-06-005 | 材料規格列表 | 已實作 | — | ⚠️ 待補測 | — | 無 MaterialSpecControllerTest |
| FN-06-006 | 新增材料規格 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-06-007 | 編輯材料規格 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-06-008 | 刪除材料規格 | — | — | 🔲 未實作 | — | |
| FN-06-009 | 供應商列表 | 已實作 | — | ⚠️ 待補測 | — | 無 SupplierControllerTest |
| FN-06-010 | 新增供應商 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-06-011 | 編輯供應商 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-06-012 | 刪除供應商 | — | — | 🔲 未實作 | — | |
| FN-06-013 | 合格材料列表 | 已實作 | — | ⚠️ 待補測 | — | 無 ApprovedMaterialControllerTest |
| FN-06-014 | 新增合格材料 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-06-015 | 批次匯入合格材料 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-06-016 | 停用合格材料 | — | — | 🔲 未實作 | — | |
| FN-06-017 | 採購單列表 | Unit | PurchaseOrderServiceTest | ✅ PASS | 04-24 | 缺 Controller Test |
| FN-06-018 | 新增採購單 | Unit | PurchaseOrderServiceTest | ✅ PASS | 04-24 | |
| FN-06-019 | 編輯採購單 | Unit | PurchaseOrderServiceTest | ✅ PASS | 04-24 | |
| FN-06-020 | 送審採購單 | Unit | PurchaseOrderServiceTest | ✅ PASS | 04-24 | |
| FN-06-021 | 核准採購單 | — | — | 🔲 未實作 | — | |
| FN-06-022 | 完成採購 | — | — | 🔲 未實作 | — | |
| FN-06-023 | 新增收料紀錄 | Unit | ReceivingServiceTest | ✅ PASS | 04-24 | 缺 Controller Test |
| FN-06-024 | 收料紀錄列表 | Unit | ReceivingServiceTest | ✅ PASS | 04-24 | |
| FN-06-025 | 轉庫作業 | Unit | InventoryAdjustmentServiceTest | ✅ PASS | 04-24 | 缺 Controller Test |
| FN-06-026 | 庫存總覽 | Unit | InventoryServiceTest | ✅ PASS | 04-24 | 缺 Controller Test |
| FN-06-027 | 庫存盤點 | Unit | InventoryAdjustmentServiceTest | ✅ PASS | 04-24 | |
| FN-06-028 | 庫存調整 | Unit | InventoryAdjustmentServiceTest | ✅ PASS | 04-24 | |
| FN-06-029 | 安全庫存設定 | — | — | 🔲 未實作 | — | |
| FN-06-030 | 安全庫存預警 | Unit | InventoryServiceTest | ✅ PASS | 04-24 | @Scheduled |
| FN-06-031 | 低庫存列表 | Unit | InventoryServiceTest | ✅ PASS | 04-24 | |
| FN-06-032 | 領料申請列表 | Unit | IssueServiceTest | ✅ PASS | 04-24 | 缺 Controller Test |
| FN-06-033 | 新增領料申請 | Unit | IssueServiceTest | ✅ PASS | 04-24 | |
| FN-06-034 | 核准領料 | Unit | IssueServiceTest | ✅ PASS | 04-24 | |
| FN-06-035 | 確認出料 | Unit | IssueServiceTest | ✅ PASS | 04-24 | |
| FN-06-036 | 出料紀錄 | — | — | 🔲 未實作 | — | |
| FN-06-037 | 費用支出報表 | — | — | 🔲 未實作 | — | |
| FN-06-038 | 廢品處理 | 已實作 | — | ⚠️ 待補測 | — | 無 DisposalControllerTest |
| FN-06-039 | 廢品紀錄 | 已實作 | — | ⚠️ 待補測 | — | |
| FN-06-040 | 廠商材料規格 | — | — | 🔲 未實作 | — | |
| FN-06-041 | 廠商用量查詢 | — | — | 🔲 未實作 | — | |
| FN-06-042 | 廠商庫存查詢 | — | — | 🔲 未實作 | — | |
| FN-06-043 | 材料壽命分析 | — | — | 🔲 未實作 | — | |
| FN-06-044 | 材料品質分析 | — | — | 🔲 未實作 | — | |
| FN-06-045 | 備存需求分析 | — | — | 🔲 未實作 | — | |

---

## P3 總結

| 分類 | 數量 |
|------|------|
| ✅ 已驗通過 (Unit only) | 10 |
| ⚠️ 已實作待補測 | 16 |
| 🔲 未實作 | 19 |
| **合計** | **45** |

### 重大缺口

**材料模組是測試覆蓋最大的缺口**：

1. **0 個 Controller Test** — 9 個 Controller 全部只有 Service 層測試
2. **4 個 Controller 完全無測試** — Supplier, MaterialSpec, ApprovedMaterial, Disposal
3. 建議優先補 Controller Test，一次解決 16 個 ⚠️

### 補測優先序

| 優先 | 待建 Test Class | 解決 FN |
|------|----------------|---------|
| 1 | SupplierControllerTest | FN-06-009~011 (3) |
| 2 | MaterialSpecControllerTest | FN-06-005~007 (3) |
| 3 | ApprovedMaterialControllerTest | FN-06-013~015 (3) |
| 4 | DisposalControllerTest | FN-06-038~039 (2) |
| 5 | WarehouseControllerTest | FN-06-001~004 (升級為 API+Unit) |
| 6 | PurchaseOrderControllerTest | FN-06-017~020 (升級) |
| 7 | ReceivingControllerTest | FN-06-023~024 (升級) |
| 8 | InventoryControllerTest | FN-06-026, 030~031 (升級) |
| 9 | IssueControllerTest | FN-06-032~035 (升級) |
