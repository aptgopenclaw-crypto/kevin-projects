# TS-05 換裝維護 — Test Specification

> **對應 SA**：SA-05-replacement (FN-05-001 ~ FN-05-035)  
> **對應 SD**：SD-05-replacement  
> **Test Classes**：7 classes, 38 test methods  
> **最新驗證**：2026-04-24 · 全部 PASS

---

## 使用方式

本文件是 **AI 實作 prompt 的驗收標準**。請 AI 實作或修改某個 FN 時，附上對應段落：

```
請實作 FN-05-015 新增換裝項目
【SA】SA-05 §換裝項目
【SD】SD-05 §3 POST /v1/auth/replacement/{id}/items
【TC】（貼 FN-05-015 的 Test Cases 表）
請確保所有 TC PASS。
```

---

## 1. 號碼牌管理 (FN-05-001 ~ FN-05-004)

### FN-05-001 號碼牌列表查詢

**SA**: SA-05 §號碼牌 | **SD**: SD-05 §3 | **API**: `GET /v1/auth/pole-numbers`  
**Service**: `LightPoleNumberService` | **SRS**: SRS-06-001 | **Spec**: §6-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-001-01 | API | 列表查詢 | perm=POLE_NUMBER_MANAGE | GET /pole-numbers | 200 | HTTP 200 | ✅ LightPoleNumberControllerTest.list |
| TC-05-001-02 | Error | 無權限 | no perm | GET /pole-numbers | 403 | HTTP 403 | ✅ LightPoleNumberControllerTest.list_noPermission |

---

### FN-05-002 產生號碼牌編號

**SA**: SA-05 §號碼牌 | **SD**: SD-05 §3 | **API**: `POST /v1/auth/pole-numbers/generate`  
**Service**: `LightPoleNumberService.generate()` | **SRS**: SRS-06-001 | **Spec**: §6-1

**商業規則**：
- 號碼牌編號不可重複

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-002-01 | API | 產生號碼牌 | perm=POLE_NUMBER_MANAGE | POST /pole-numbers/generate | 200 | HTTP 200 | ✅ LightPoleNumberControllerTest.generate |
| TC-05-002-02 | Happy | 正常產生 | poleNumber 不存在 | generate("PN-001", deviceId) | 200, created | id 非 null | ✅ LightPoleNumberServiceTest.generate_success |
| TC-05-002-03 | Error | 號碼重複 | poleNumber 已存在 | generate("PN-001") | 拋錯 | duplicate error | ✅ LightPoleNumberServiceTest.generate_duplicate |

---

### FN-05-003 重製 QR Code

**SA**: SA-05 §號碼牌 | **SD**: SD-05 §3 | **API**: `GET /v1/auth/pole-numbers/{id}/qr`  
**SRS**: SRS-06-001 | **Spec**: §6-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-003-01 | Happy | 產生 QR Code | pole exists | GET /pole-numbers/{id}/qr | 200, PNG image | Content-Type=image/png | ⬜ 待實作 |

---

### FN-05-004 刪除號碼牌

**SA**: SA-05 §號碼牌 | **SD**: SD-05 §3 | **API**: `DELETE /v1/auth/pole-numbers/{id}`  
**SRS**: SRS-06-001 | **Spec**: §6-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-004-01 | Happy | 刪除號碼牌 | pole exists | DELETE /pole-numbers/{id} | 200 | 已刪除 | ⬜ 待補 |

---

## 2. 批次匯入 (FN-05-005 ~ FN-05-006)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-05-005 | 批次匯入資產清冊 | TC-05-005-01 | POST /replacement/import → 差異預覽 | ⬜ 待實作 |
| FN-05-006 | 匯入差異確認 | TC-05-006-01 | PUT /replacement/import/confirm → 批次更新 | ⬜ 待實作 |

---

## 3. 換裝工單核心 (FN-05-007 ~ FN-05-011)

### FN-05-007 換裝工單列表

**SA**: SA-05 §換裝工單 | **SD**: SD-05 §3 | **API**: `GET /v1/auth/replacement`  
**Service**: `ReplacementOrderService.list()` | **SRS**: SRS-06-006 | **Spec**: §6-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-007-01 | API | 列表查詢 | perm=REPLACEMENT_VIEW | GET /replacement | 200 | HTTP 200 | ✅ ReplacementOrderControllerTest.list |
| TC-05-007-02 | Error | 無權限 | no perm | GET /replacement | 403 | HTTP 403 | ✅ ReplacementOrderControllerTest.list_noPermission |
| TC-05-007-03 | Happy | Service 列表 | orders exist | list() | order list | 分頁 | ✅ ReplacementOrderServiceTest.list |

---

### FN-05-008 新增換裝工單

**SA**: SA-05 §換裝工單 | **SD**: SD-05 §3 | **API**: `POST /v1/auth/replacement`  
**Service**: `ReplacementOrderService.createFromRepair()` / `createDirect()` | **SRS**: SRS-06-006 | **Spec**: §6-6

**商業規則**：
- 從報修工單建立（RepairTicket → ReplacementOrder）
- 直接建立（指定 contractId 等）
- repairTicketId 不存在 → 拋錯

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-008-01 | API | 新增工單 | perm=REPLACEMENT_MANAGE | POST /replacement | 200 | HTTP 200 | ✅ ReplacementOrderControllerTest.create |
| TC-05-008-02 | Happy | 從報修建立 | repairTicket exists | createFromRepair(repairId) | order created | 含 repairTicketId | ✅ ReplacementOrderServiceTest.createFromRepair |
| TC-05-008-03 | Error | 報修不存在 | invalid repairId | createFromRepair(999) | not found | errorCode | ✅ ReplacementOrderServiceTest.createFromRepair_notFound |
| TC-05-008-04 | Happy | 直接建立 | valid data | createDirect() | order, status=DRAFT | status | ✅ ReplacementOrderServiceTest.createDirect |

---

### FN-05-009 查看工單詳情

**SA**: SA-05 §換裝工單 | **SD**: SD-05 §3 | **API**: `GET /v1/auth/replacement/{id}`  
**Service**: `ReplacementOrderService.getById()` | **SRS**: SRS-06-006 | **Spec**: §6-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-009-01 | API | 查詢詳情 | order exists | GET /replacement/{id} | 200 | HTTP 200 | ✅ ReplacementOrderControllerTest.getById |
| TC-05-009-02 | Error | 不存在 | invalid id | GET /replacement/{id} | 404 | HTTP 404 | ✅ ReplacementOrderControllerTest.getById_notFound |
| TC-05-009-03 | Error | Service 不存在 | invalid id | getById() | not found | errorCode | ✅ ReplacementOrderServiceTest.getById_notFound |

---

### FN-05-010 派工

**SA**: SA-05 §換裝流程 | **SD**: SD-05 §3 | **API**: `PUT /v1/auth/replacement/{id}/dispatch`  
**Service**: `ReplacementOrderService.dispatch()` | **SRS**: SRS-06-006 | **Spec**: §6-6

**商業規則**：
- 僅 DRAFT 狀態可派工
- 派工 → DISPATCHED

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-010-01 | API | 派工 | perm=REPLACEMENT_MANAGE | PUT /replacement/{id}/dispatch | 200 | HTTP 200 | ✅ ReplacementOrderControllerTest.dispatch |
| TC-05-010-02 | Happy | Service 派工 | status=DRAFT | dispatch() | status=DISPATCHED | status 更新 | ✅ ReplacementOrderServiceTest.dispatch |
| TC-05-010-03 | Error | 狀態不合 | status≠DRAFT | dispatch() | 拋錯 | invalid status | ✅ ReplacementOrderServiceTest.dispatch_invalidStatus |

---

### FN-05-011 開工

**SA**: SA-05 §換裝流程 | **SD**: SD-05 §3 | **API**: `PUT /v1/auth/replacement/{id}/start`  
**Service**: `ReplacementOrderService.startWork()` | **SRS**: SRS-06-006 | **Spec**: §6-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-011-01 | Happy | 開工 | status=DISPATCHED | startWork() | status=IN_PROGRESS | status 更新 | ✅ ReplacementOrderServiceTest.startWork |

---

## 4. 案件類別 & 預匯入 (FN-05-012 ~ FN-05-014)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-05-012 | 案件類別管理 | TC-05-012-01 | CRUD /replacement/types → 200 | ⬜ 待實作 |
| FN-05-013 | 預匯入路燈編號 | TC-05-013-01 | POST pre-import → 預覽 | ⬜ 待實作 |
| FN-05-014 | 預匯入地圖查看 | TC-05-014-01 | GET pre-import/map → GeoJSON | ⬜ 待實作 |

---

## 5. 換裝項目 (FN-05-015 ~ FN-05-019)

### FN-05-015 新增換裝項目

**SA**: SA-05 §換裝項目 | **SD**: SD-05 §3 | **API**: `POST /v1/auth/replacement/{id}/items`  
**Service**: `ReplacementItemService.addItem()` | **SRS**: SRS-06-005 | **Spec**: §6-5

**商業規則**：
- 項目必須屬於該工單
- 材料必須為 ACTIVE 合格材料
- 工單非 DRAFT 狀態時不可新增

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-015-01 | API | 新增項目 | perm=REPLACEMENT_MANAGE | POST /replacement/{id}/items | 200 | HTTP 200 | ✅ ReplacementOrderControllerTest.addItem |
| TC-05-015-02 | Happy | 正常新增 | valid item data | addItem() | item created | id 非 null | ✅ ReplacementItemServiceTest.addItem |
| TC-05-015-03 | Error | 不屬於該工單 | orderId 不匹配 | addItem() | 拋錯 | not belong | ✅ ReplacementItemServiceTest.addItem_notBelongToParent |
| TC-05-015-04 | Error | 材料非 ACTIVE | material.status≠ACTIVE | addItem() | 拋錯 | not active | ✅ ReplacementItemServiceTest.addItem_materialNotActive |
| TC-05-015-05 | Error | 材料不存在 | invalid materialId | addItem() | not found | errorCode | ✅ ReplacementItemServiceTest.addItem_materialNotFound |
| TC-05-015-06 | Error | 工單狀態不合 | status≠DRAFT | addItem() | 拋錯 | invalid status | ✅ ReplacementItemServiceTest.addItem_invalidStatus |

---

### FN-05-016 換裝項目列表

**SA**: SA-05 §換裝項目 | **SD**: SD-05 §3 | **API**: `GET /v1/auth/replacement/{id}/items`  
**Service**: `ReplacementItemService.getItems()` | **SRS**: SRS-06-005 | **Spec**: §6-4

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-016-01 | API | 查詢項目 | order exists | GET /replacement/{id}/items | 200 | HTTP 200 | ✅ ReplacementOrderControllerTest.getItems |
| TC-05-016-02 | Happy | Service 查詢 | items exist | getItems() | item list | list 非空 | ✅ ReplacementItemServiceTest.getItems |

---

### FN-05-017 編輯換裝項目

**SA**: SA-05 §換裝項目 | **SD**: SD-05 §3 | **API**: `PUT /v1/auth/replacement/items/{itemId}`  
**SRS**: SRS-06-005 | **Spec**: §6-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-017-01 | Happy | 編輯項目 | item exists, status=DRAFT | PUT /items/{itemId} | 200 | 欄位更新 | ⬜ 待補 |

---

### FN-05-018 刪除換裝項目

**SA**: SA-05 §換裝項目 | **SD**: SD-05 §3 | **API**: `DELETE /v1/auth/replacement/items/{itemId}`  
**Service**: `ReplacementItemService.deleteItem()` | **SRS**: SRS-06-005 | **Spec**: §6-5

**商業規則**：
- 僅 DRAFT 狀態可刪除

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-018-01 | Happy | DRAFT 可刪 | status=DRAFT | deleteItem() | 200, deleted | 已刪除 | ✅ ReplacementItemServiceTest.deleteItem_draft |
| TC-05-018-02 | Error | 非 DRAFT 不可刪 | status≠DRAFT | deleteItem() | 拋錯 | invalid status | ✅ ReplacementItemServiceTest.deleteItem_nonDraft |

---

### FN-05-019 合格材料查驗

**SA**: SA-05 §換裝項目 | **SD**: SD-05 §3 | **API**: (內部呼叫)  
**SRS**: SRS-06-005 | **Spec**: §6-5

→ 含於 FN-05-015 (TC-05-015-04, addItem_materialNotActive)

---

## 6. 合格材料匯入 (FN-05-020)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-05-020 | 材料清單批次匯入 | TC-05-020-01 | POST /approved-materials/import → 批次建立 | ⬜ 待實作 |

---

## 7. 自主檢核 & 審核 (FN-05-021 ~ FN-05-028)

### FN-05-021 提交自主檢核

**SA**: SA-05 §自檢 | **SD**: SD-05 §3 | **API**: `PUT /v1/auth/replacement/{id}/self-check`  
**Service**: `ReplacementOrderService.selfCheck()` | **SRS**: SRS-06-009 | **Spec**: §6-9

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-021-01 | API | 自主檢核 | perm=REPLACEMENT_MANAGE | PUT /replacement/{id}/self-check | 200 | HTTP 200 | ✅ ReplacementOrderControllerTest.selfCheck |
| TC-05-021-02 | Happy | Service 自檢 | status=IN_PROGRESS | selfCheck() | status=SELF_CHECKED | status 更新 | ✅ ReplacementOrderServiceTest.selfCheck |

---

### FN-05-022 ~ FN-05-023 自檢輔助

| FN ID | 功能名稱 | TC 設計 | 自動化 |
|-------|---------|---------|--------|
| FN-05-022 | 自檢後地圖/清冊查看 | 組合既有查詢 | — |
| FN-05-023 | 組件替換（自檢） | 含於 FN-05-021 | — |

---

### FN-05-024 ~ FN-05-028 送審/結案/退回

**SA**: SA-05 §審核 | **SD**: SD-05 §3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-024-01 | Happy | 報竣送審 | status=SELF_CHECKED | approve() | status=APPROVED | status | ✅ ReplacementOrderServiceTest.approve |
| TC-05-025-01 | Happy | 結案 | approved via workflow | — | status=CLOSED | → TS-02 | ↗ 參照 TS-02 |
| TC-05-026-01 | Happy | 退回補件 | — | — | — | → TS-02 | ↗ 參照 TS-02 |
| TC-05-027-01 | Happy | 退貨 | — | returnOrder() | status=RETURNED | status | ✅ ReplacementOrderServiceTest.returnOrder |
| TC-05-028-01 | Happy | 結案後補救 | — | reopen() | status 回退 | → Phase 5+ | ⬜ 待實作 |

---

## 8. 異動/統計/匯出 (FN-05-029 ~ FN-05-035)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-05-029 | 異動紀錄查詢 | TC-05-029-01 | GET /replacement/{id}/changes → 200 | ⬜ 待實作 |
| FN-05-030 | 派工案件類型統計 | TC-05-030-01 | GET /replacement/statistics → by type | ⬜ 待實作 |
| FN-05-031 | 交付/完成件數統計 | TC-05-031-01 | GET /replacement/statistics/progress | ⬜ 待實作 |
| FN-05-032 | 匯出派工清冊 | TC-05-032-01 | GET /replacement/export → Excel | ⬜ 待實作 |
| FN-05-033 | 匯出異動路燈地圖 | TC-05-033-01 | GET /replacement/{id}/export/map | ⬜ 待實作 |
| FN-05-034 | 產生竣工清單 | TC-05-034-01 | GET /replacement/{id}/report/completion → PDF | ⬜ 待實作 |
| FN-05-035 | 產生用電申請表 | TC-05-035-01 | GET /replacement/{id}/report/power-application → PDF | ⬜ 待實作 |

---

## 9. 事件監聽器 (跨切面)

### ReplacementClosedListener

**SA**: SA-05 §事件 | **SD**: SD-05 §4 | **API**: (內部 Event)  
**Service**: `ReplacementClosedListener` | **SRS**: SRS-06-010

**商業規則**：
- REPLACEMENT_REVIEW CLOSED → 更新工單 status=CLOSED
- 非 REPLACEMENT_REVIEW → 跳過

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-E1-01 | Happy | 結案 → 更新狀態 | REPLACEMENT_REVIEW CLOSED | event | order.status=CLOSED | status | ✅ ReplacementClosedListenerTest.onClosed_updatesStatus |
| TC-05-E1-02 | Edge | 非 REPLACEMENT | type≠REPLACEMENT_REVIEW | event | 跳過 | no action | ✅ ReplacementClosedListenerTest.wrongType_ignores |

---

### ReplacementNeedMaterialListener

**SA**: SA-05 §事件 | **SD**: SD-05 §4 | **API**: (內部 Event)  
**Service**: `ReplacementNeedMaterialListener` | **SRS**: SRS-06-006

**商業規則**：
- 派工完成 (DISPATCHED) → 自動建立材料領料申請
- 非 DISPATCHED step → 跳過

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-05-E2-01 | Happy | 派工 → 建領料 | DISPATCHED step | event | IssueRequest created | request.id | ✅ ReplacementNeedMaterialListenerTest.onDispatched_createsIssue |
| TC-05-E2-02 | Edge | 非 DISPATCHED | step=CLOSED | event | 跳過 | no action | ✅ ReplacementNeedMaterialListenerTest.wrongStep_ignores |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ✅ 已有自動化 | 34 |
| ⬜ 待補（已實作 FN） | 1 |
| ⬜ 待實作（未實作 FN） | 14 |
| ↗ 參照 TS-02 | 2 FN |
| **總 TC 數** | **49** |

### 待補測試優先序

| 優先 | FN | 待建 TC | 原因 |
|------|-----|--------|------|
| 1 | FN-05-017 | 1 TC | 換裝項目編輯已實作，缺測試 |
| 2 | FN-05-004 | 1 TC | 號碼牌刪除已實作 |
| 3 | FN-05-003 | 1 TC | QR Code 產生 |
| 4 | FN-05-005/006 | 2 TC | 批次匯入（Phase 5+） |
| 5 | FN-05-029~035 | 7 TC | 統計/匯出/報表（Phase 5+） |
