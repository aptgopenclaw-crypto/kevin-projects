# TS-06 材料管理 — Test Specification

> **對應 SA**：SA-06-material (FN-06-001 ~ FN-06-045)  
> **對應 SD**：SD-06-material  
> **Test Classes**：6 classes, 26 test methods  
> **最新驗證**：2026-04-24 · 全部 PASS

---

## 使用方式

本文件是 **AI 實作 prompt 的驗收標準**。請 AI 實作或修改某個 FN 時，附上對應段落：

```
請實作 FN-06-025 轉庫作業
【SA】SA-06 §收料與轉庫
【SD】SD-06 §5 POST /v1/auth/material/inventory/transfer
【TC】（貼 FN-06-025 的 Test Cases 表）
請確保所有 TC PASS。
```

---

## 1. 倉庫管理 (FN-06-001 ~ FN-06-004)

### FN-06-001 倉庫列表查詢

**SA**: SA-06 §倉庫管理 | **SD**: SD-06 §5 | **API**: `GET /v1/auth/material/warehouses`  
**Service**: `WarehouseService.list()` | **SRS**: SRS-07-001 | **Spec**: §7-1A

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-001-01 | Happy | 列表查詢 | warehouses exist | list() | warehouse list | 分頁 | ✅ WarehouseServiceTest.list |

---

### FN-06-002 新增倉庫

**SA**: SA-06 §倉庫管理 | **SD**: SD-06 §5 | **API**: `POST /v1/auth/material/warehouses`  
**Service**: `WarehouseService.create()` | **SRS**: SRS-07-001 | **Spec**: §7-1A

**商業規則**：
- 新增預設 status=ACTIVE

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-002-01 | Happy | 新增倉庫 | valid data | create("WH-01", "主庫") | 200, status=ACTIVE | status 預設 | ✅ WarehouseServiceTest.create |

---

### FN-06-003 編輯倉庫

**SA**: SA-06 §倉庫管理 | **SD**: SD-06 §5 | **API**: `PUT /v1/auth/material/warehouses/{id}`  
**Service**: `WarehouseService.update()` | **SRS**: SRS-07-001 | **Spec**: §7-1A

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-003-01 | Happy | 更新倉庫 | warehouse exists | update(name="更新") | 200 | name 更新 | ✅ WarehouseServiceTest.update |

---

### FN-06-004 刪除倉庫

**SA**: SA-06 §倉庫管理 | **SD**: SD-06 §5 | **API**: `DELETE /v1/auth/material/warehouses/{id}`  
**Service**: `WarehouseService.delete()` | **SRS**: SRS-07-001 | **Spec**: §7-1A

**商業規則**：
- 軟刪除：status=INACTIVE

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-004-01 | Happy | 軟刪除 | warehouse exists | delete() | status=INACTIVE | soft delete | ✅ WarehouseServiceTest.delete_setsInactive |
| TC-06-004-02 | Error | 不存在 | invalid id | findOrThrow() | WAREHOUSE_NOT_FOUND | errorCode | ✅ WarehouseServiceTest.findOrThrow_notFound |

---

## 2. 材料規格 & 供應商 (FN-06-005 ~ FN-06-016)

> **實作狀態**：大部分未有專屬測試

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-06-005 | 材料規格列表 | TC-06-005-01 | GET /material/specs → 200, list | ⬜ 待補 |
| FN-06-006 | 新增材料規格 | TC-06-006-01 | POST /material/specs → 201 | ⬜ 待補 |
| FN-06-007 | 編輯材料規格 | TC-06-007-01 | PUT /material/specs/{id} → 200 | ⬜ 待補 |
| FN-06-008 | 刪除材料規格 | TC-06-008-01 | DELETE /material/specs/{id} → 200 | ⬜ 待補 |
| FN-06-009 | 供應商列表 | TC-06-009-01 | GET /material/suppliers → 200 | ⬜ 待補 |
| FN-06-010 | 新增供應商 | TC-06-010-01 | POST /material/suppliers → 201 | ⬜ 待補 |
| FN-06-011 | 編輯供應商 | TC-06-011-01 | PUT /material/suppliers/{id} → 200 | ⬜ 待補 |
| FN-06-012 | 刪除供應商 | TC-06-012-01 | DELETE /material/suppliers/{id} → 200 | ⬜ 待補 |
| FN-06-013 | 合格材料列表 | TC-06-013-01 | GET /material/approved → 200 | ⬜ 待補 |
| FN-06-014 | 新增合格材料 | TC-06-014-01 | POST /material/approved → 201 | ⬜ 待補 |
| FN-06-015 | 批次匯入合格材料 | TC-06-015-01 | POST /material/approved/import → batch | ⬜ 待實作 |
| FN-06-016 | 停用合格材料 | TC-06-016-01 | PUT /material/approved/{id}/disable → INACTIVE | ⬜ 待補 |

---

## 3. 採購管理 (FN-06-017 ~ FN-06-022)

### FN-06-017 採購單列表

**SA**: SA-06 §採購管理 | **SD**: SD-06 §5 | **API**: `GET /v1/auth/material/purchase-orders`  
**SRS**: SRS-07-002 | **Spec**: §7-1B

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-017-01 | Happy | 列表查詢 | POs exist | GET /purchase-orders | 200 | list | ⬜ 待補 |

---

### FN-06-018 新增採購單

**SA**: SA-06 §採購管理 | **SD**: SD-06 §5 | **API**: `POST /v1/auth/material/purchase-orders`  
**Service**: `PurchaseOrderService.create()` | **SRS**: SRS-07-002 | **Spec**: §7-1B

**商業規則**：
- 新增預設 status=DRAFT
- 自動產生 poNumber

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-018-01 | Happy | 新增採購單 | valid data | create(supplierId, items) | status=DRAFT, poNumber 自動 | poNumber 非 null | ✅ PurchaseOrderServiceTest.create |

---

### FN-06-019 編輯採購單

**SA**: SA-06 §採購管理 | **SD**: SD-06 §5 | **API**: `PUT /v1/auth/material/purchase-orders/{id}`  
**Service**: `PurchaseOrderService.update()` | **SRS**: SRS-07-002 | **Spec**: §7-1B

**商業規則**：
- 僅 DRAFT 可編輯

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-019-01 | Error | 非 DRAFT 不可編輯 | status=APPROVED | update() | 拋錯 | invalid status | ✅ PurchaseOrderServiceTest.update_nonDraft |

---

### FN-06-020 送審採購單

**SA**: SA-06 §採購管理 | **SD**: SD-06 §5 | **API**: `PUT /v1/auth/material/purchase-orders/{id}/submit`  
**Service**: `PurchaseOrderService.submit()` | **SRS**: SRS-07-002 | **Spec**: §7-1B

**商業規則**：
- 僅 DRAFT → SUBMITTED
- 已 SUBMITTED → 拋錯

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-020-01 | Happy | 送審成功 | status=DRAFT | submit() | status=SUBMITTED | status 更新 | ✅ PurchaseOrderServiceTest.submit |
| TC-06-020-02 | Error | 非 DRAFT | status=SUBMITTED | submit() | 拋錯 | invalid status | ✅ PurchaseOrderServiceTest.submit_nonDraft |

---

### FN-06-021 核准採購單

**SA**: SA-06 §採購管理 | **SD**: SD-06 §5 | **API**: `PUT /v1/auth/material/purchase-orders/{id}/approve`  
**SRS**: SRS-07-002 | **Spec**: §7-1B

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-021-01 | Happy | 核准 | status=SUBMITTED | approve() | status=APPROVED | status | ⬜ 待補 |

---

### FN-06-022 完成採購

**SA**: SA-06 §採購管理 | **SD**: SD-06 §5 | **API**: `PUT /v1/auth/material/purchase-orders/{id}/complete`  
**SRS**: SRS-07-002 | **Spec**: §7-1B

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-022-01 | Happy | 完成 | status=APPROVED | complete() | status=COMPLETED | status | ⬜ 待補 |

---

### 採購單補充

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-PO-01 | Error | 不存在 | invalid id | findOrThrow() | PURCHASE_ORDER_NOT_FOUND | errorCode | ✅ PurchaseOrderServiceTest.findOrThrow_notFound |

---

## 4. 收料管理 (FN-06-023 ~ FN-06-025)

### FN-06-023 新增收料紀錄

**SA**: SA-06 §收料管理 | **SD**: SD-06 §5 | **API**: `POST /v1/auth/material/receiving`  
**Service**: `ReceivingService.receive()` | **SRS**: SRS-07-003 | **Spec**: §7-1C

**商業規則**：
- 已有庫存 → 累加數量
- 無庫存 → 新建庫存紀錄

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-023-01 | Happy | 累加既有庫存 | inventory exists (qty=10) | receive(qty=20) | inventory.qty=30 | qty 累加 | ✅ ReceivingServiceTest.receive_existingInventory_updatesQty |
| TC-06-023-02 | Happy | 新建庫存紀錄 | no inventory | receive(qty=15) | new inventory, qty=15 | record created | ✅ ReceivingServiceTest.receive_newInventory_creates |

---

### FN-06-024 收料紀錄列表

**SA**: SA-06 §收料管理 | **SD**: SD-06 §5 | **API**: `GET /v1/auth/material/receiving`  
**SRS**: SRS-07-003 | **Spec**: §7-1C

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-024-01 | Happy | 列表查詢 | records exist | GET /material/receiving | 200 | list | ⬜ 待補 |

---

### FN-06-025 轉庫作業

**SA**: SA-06 §收料與轉庫 | **SD**: SD-06 §5 | **API**: `POST /v1/auth/material/inventory/transfer`  
**Service**: `InventoryAdjustmentService.transfer()` | **SRS**: SRS-07-003 | **Spec**: §7-1C

**商業規則**：
- 來源庫存不足 → INSUFFICIENT_INVENTORY
- 來源扣減，目標增加

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-025-01 | Happy | 轉庫成功 | from=50, to=10 | transfer(qty=15) | from=35, to=25 | 雙方 qty | ✅ InventoryAdjustmentServiceTest.transfer_success |
| TC-06-025-02 | Error | 庫存不足 | onHand=3 | transfer(qty=10) | INSUFFICIENT_INVENTORY | errorCode | ✅ InventoryAdjustmentServiceTest.transfer_insufficient |

---

## 5. 庫存管理 (FN-06-026 ~ FN-06-031)

### FN-06-026 庫存總覽查詢

**SA**: SA-06 §庫存管理 | **SD**: SD-06 §5 | **API**: `GET /v1/auth/material/inventory`  
**Service**: `InventoryService.list()` | **SRS**: SRS-07-004 | **Spec**: §7-1D

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-026-01 | Happy | 庫存列表 | inventory exists | list() | inventory list | 含 onHand, safety | ✅ InventoryServiceTest.list |

---

### FN-06-027 庫存盤點

**SA**: SA-06 §庫存管理 | **SD**: SD-06 §5 | **API**: `POST /v1/auth/material/inventory/count`  
**Service**: `InventoryAdjustmentService.count()` | **SRS**: SRS-07-004 | **Spec**: §7-1D

**商業規則**：
- 盤點調整：actualQuantity 與 onHand 差異 → 自動計算 change

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-027-01 | Happy | 盤點 | onHand=20 | count(actual=25) | change=+5, qty=25 | change 計算 | ✅ InventoryAdjustmentServiceTest.count |

---

### FN-06-028 庫存調整

**SA**: SA-06 §庫存管理 | **SD**: SD-06 §5 | **API**: `POST /v1/auth/material/inventory/adjust`  
**Service**: `InventoryAdjustmentService.correction()` | **SRS**: SRS-07-004 | **Spec**: §7-1D

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-028-01 | Happy | 修正庫存 | onHand=20 | correction(qty=-3) | qty=17 | 調整值 | ✅ InventoryAdjustmentServiceTest.correction |

---

### FN-06-029 安全庫存量設定

**SA**: SA-06 §庫存管理 | **SD**: SD-06 §5 | **API**: `PUT /v1/auth/material/inventory/{id}/safety-stock`  
**SRS**: SRS-07-004 | **Spec**: §7-1D

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-029-01 | Happy | 設定安全庫存 | inventory exists | PUT safety-stock=10 | 200, safetyStock=10 | 值更新 | ⬜ 待補 |

---

### FN-06-030 安全庫存預警 (E12)

**SA**: SA-06 §庫存管理 | **SD**: SD-06 §5 | **API**: (排程)  
**SRS**: SRS-07-004 | **Spec**: §7-1D

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-030-01 | Happy | 偵測低庫存 | onHand < safety | 排程 | 通知 | notification | ⬜ 待實作 |

---

### FN-06-031 低庫存預警列表

**SA**: SA-06 §庫存管理 | **SD**: SD-06 §5 | **API**: `GET /v1/auth/material/inventory/low-stock`  
**Service**: `InventoryService.findBelowSafetyStock()` | **SRS**: SRS-07-004 | **Spec**: §7-1D

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-031-01 | Happy | 低庫存列表 | onHand=3, safety=10 | findBelowSafetyStock() | 含此 inventory | below safety | ✅ InventoryServiceTest.findBelowSafetyStock |

---

### 庫存補充

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-INV-01 | Happy | 庫存彙總 | category=LUMINAIRE | summarize() | category summary | count + qty | ✅ InventoryServiceTest.summarize |
| TC-06-INV-02 | Error | 不存在 | invalid id | findOrThrow() | error | errorCode | ✅ InventoryServiceTest.findOrThrow_notFound |

---

## 6. 領料管理 (FN-06-032 ~ FN-06-036)

### FN-06-032 領料申請列表

**SA**: SA-06 §領料管理 | **SD**: SD-06 §5 | **API**: `GET /v1/auth/material/issues`  
**SRS**: SRS-07-005 | **Spec**: §7-1E

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-032-01 | Happy | 列表查詢 | issues exist | GET /material/issues | 200 | list | ⬜ 待補 |

---

### FN-06-033 新增領料申請

**SA**: SA-06 §領料管理 | **SD**: SD-06 §5 | **API**: `POST /v1/auth/material/issues`  
**Service**: `IssueService.createManual()` | **SRS**: SRS-07-005 | **Spec**: §7-1E

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-033-01 | Happy | 手動建立 | valid data | createManual(repairTicketId=10) | status=PENDING | status | ✅ IssueServiceTest.createManual |

---

### FN-06-034 核准領料

**SA**: SA-06 §領料管理 | **SD**: SD-06 §5 | **API**: `PUT /v1/auth/material/issues/{id}/approve`  
**Service**: `IssueService.approve()` | **SRS**: SRS-07-005 | **Spec**: §7-1E

**商業規則**：
- 僅 PENDING → APPROVED
- 非 PENDING → 拋錯

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-034-01 | Happy | 核准 | status=PENDING | approve() | status=APPROVED | status 更新 | ✅ IssueServiceTest.approve |
| TC-06-034-02 | Error | 狀態不合 | status=ISSUED | approve() | 拋錯 | invalid status | ✅ IssueServiceTest.approve_wrongStatus |

---

### FN-06-035 確認出料(扣庫)

**SA**: SA-06 §領料管理 | **SD**: SD-06 §5 | **API**: `PUT /v1/auth/material/issues/{id}/confirm`  
**Service**: `IssueService.issue()` | **SRS**: SRS-07-005 | **Spec**: §7-1E

**商業規則**：
- 扣減庫存
- 庫存不足 → INSUFFICIENT_INVENTORY

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-035-01 | Happy | 出料成功 | onHand=50, issueQty=5 | issue() | onHand=45, status=ISSUED | qty 扣減 | ✅ IssueServiceTest.issue_success |
| TC-06-035-02 | Error | 庫存不足 | onHand=2, issueQty=5 | issue() | INSUFFICIENT_INVENTORY | errorCode | ✅ IssueServiceTest.issue_insufficient |

---

### FN-06-036 出料紀錄查詢

**SA**: SA-06 §領料管理 | **SD**: SD-06 §5 | **API**: `GET /v1/auth/material/issues/records`  
**SRS**: SRS-07-005 | **Spec**: §7-1E

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-036-01 | Happy | 出料紀錄 | issued records exist | GET /issues/records | 200 | list | ⬜ 待補 |

---

### 領料補充

#### 駁回領料

**Service**: `IssueService.reject()` | 

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-06-ISS-01 | Happy | 駁回 | status=PENDING | reject() | status=REJECTED | status | ✅ IssueServiceTest.reject |

---

## 7. 費用與廢品 (FN-06-037 ~ FN-06-039)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-06-037 | 費用支出報表 | TC-06-037-01 | GET /material/reports/cost → 彙總 | ⬜ 待實作 |
| FN-06-038 | 廢品處理(繳庫) | TC-06-038-01 | POST /material/disposal → 建立 | ⬜ 待實作 |
| FN-06-039 | 廢品紀錄查詢 | TC-06-039-01 | GET /material/disposal → list | ⬜ 待實作 |

---

## 8. 廠商專區 (FN-06-040 ~ FN-06-045)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-06-040 | 廠商材料規格查詢 | TC-06-040-01 | GET /contractor/specs → filtered | ⬜ 待實作 |
| FN-06-041 | 廠商月(年)用量查詢 | TC-06-041-01 | GET /contractor/usage → aggregated | ⬜ 待實作 |
| FN-06-042 | 廠商安全庫存查詢 | TC-06-042-01 | GET /contractor/inventory → filtered | ⬜ 待實作 |
| FN-06-043 | 材料壽命分析 | TC-06-043-01 | GET /contractor/lifetime → analysis | ⬜ 待實作 |
| FN-06-044 | 材料品質分析 | TC-06-044-01 | GET /contractor/quality → analysis | ⬜ 待實作 |
| FN-06-045 | 備存需求分析 | TC-06-045-01 | GET /contractor/stock-suggest → suggestions | ⬜ 待實作 |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ✅ 已有自動化 | 22 |
| ⬜ 待補（已實作 FN） | 16 |
| ⬜ 待實作（未實作 FN） | 10 |
| **總 TC 數** | **48** |

### 待補測試優先序

| 優先 | FN | 待建 TC | 原因 |
|------|-----|--------|------|
| 1 | FN-06-005~012 | 8 TC | 材料規格/供應商 CRUD 已實作，完全無 Controller 測試 |
| 2 | FN-06-013~016 | 4 TC | 合格材料 CRUD 已實作 |
| 3 | FN-06-017/021/022/024/029/032/036 | 7 TC | 採購/收料/領料缺 API/列表測試 |
| 4 | FN-06-037~039 | 3 TC | 費用報表/廢品（Phase 5+） |
| 5 | FN-06-040~045 | 6 TC | 廠商專區（Phase 5+） |
