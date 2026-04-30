# TS-04 報修維護 — Test Specification

> **對應 SA**：SA-04-repair (FN-04-001 ~ FN-04-047)  
> **對應 SD**：SD-04-repair  
> **Test Classes**：8 classes, 44 test methods  
> **最新驗證**：2026-04-24 · 全部 PASS

---

## 使用方式

本文件是 **AI 實作 prompt 的驗收標準**。請 AI 實作或修改某個 FN 時，附上對應段落：

```
請實作 FN-04-014 派工
【SA】SA-04 §報修流程
【SD】SD-04 §3 POST /v1/auth/repair/{id}/dispatch
【TC】（貼 FN-04-014 的 Test Cases 表）
請確保所有 TC PASS。
```

---

## 1. 民眾報修 (FN-04-001 ~ FN-04-003)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-04-001 | 民眾報修頁面 | TC-04-001-01 | GET /public/repair → 200 | ⬜ 待實作 |
| FN-04-002 | 提交民眾報修 | TC-04-002-01 | POST /public/repair → 201, ticketNo | ⬜ 待實作 |
| FN-04-003 | 報修進度查詢 | TC-04-003-01 | GET /public/repair/{ticketNo}/status → 200 | ⬜ 待實作 |

---

## 2. 外部系統介接 (FN-04-004 ~ FN-04-005)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-04-004 | 1999 陳情案件接收 | TC-04-004-01 | POST /integration/1999/receive → 建立工單 | ⬜ 待實作 |
| FN-04-005 | 1999 處理結果回覆 | TC-04-005-01 | 完工後事件驅動回覆 1999 | ⬜ 待實作 |

---

## 3. 附件管理 (FN-04-006 ~ FN-04-009)

### FN-04-006 上傳附件

**SA**: SA-04 §附件 | **SD**: SD-04 §3 | **API**: `POST /v1/auth/attachments`  
**Service**: `TicketAttachmentService.upload()` | **SRS**: SRS-05-003 | **Spec**: §5-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-006-01 | Happy | 上傳附件 | authenticated | multipart file | 200, attachment | id + url | ✅ TicketAttachmentServiceTest.upload |

---

### FN-04-007 附件列表查詢

**SA**: SA-04 §附件 | **SD**: SD-04 §3 | **API**: `GET /v1/auth/attachments`  
**Service**: `TicketAttachmentService.getByTicket()` | **SRS**: SRS-05-003 | **Spec**: §5-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-007-01 | Happy | 查詢附件 | ticket 有附件 | GET /attachments?ticketId=1 | 200, list | list 非空 | ✅ TicketAttachmentServiceTest.getByTicket |

---

### FN-04-008 下載附件

**SA**: SA-04 §附件 | **SD**: SD-04 §3 | **API**: `GET /v1/auth/attachments/{id}/download`  
**Service**: `TicketAttachmentService.download()` | **SRS**: SRS-05-003 | **Spec**: §5-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-008-01 | Happy | 下載附件 | attachment exists | GET /attachments/{id}/download | 200, file stream | Content-Type | ✅ TicketAttachmentServiceTest.download |
| TC-04-008-02 | Error | 不存在 | invalid id | GET /attachments/{id}/download | not found | errorCode | ✅ TicketAttachmentServiceTest.download_notFound |

---

### FN-04-009 刪除附件

**SA**: SA-04 §附件 | **SD**: SD-04 §3 | **API**: `DELETE /v1/auth/attachments/{id}`  
**SRS**: SRS-05-003 | **Spec**: §5-3

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-009-01 | Happy | 刪除附件 | attachment exists | DELETE /attachments/{id} | 200 | 已刪除 | ⬜ 待補 |

---

## 4. 報修工單核心 (FN-04-010 ~ FN-04-021)

### FN-04-010 報修工單列表

**SA**: SA-04 §報修工單 | **SD**: SD-04 §3 | **API**: `GET /v1/auth/repair`  
**Service**: `RepairTicketService.list()` | **SRS**: SRS-05-005 | **Spec**: §5-5

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-010-01 | API | 列表查詢 | perm=REPAIR_VIEW | GET /repair | 200 | HTTP 200 | ✅ RepairTicketControllerTest.list |
| TC-04-010-02 | Error | 無權限 | no REPAIR_VIEW | GET /repair | 403 | HTTP 403 | ✅ RepairTicketControllerTest.list_noPermission |
| TC-04-010-03 | Happy | Service 列表 | tickets exist | list() | ticket list | 分頁 | ✅ RepairTicketServiceTest.list |

---

### FN-04-011 新增報修工單（內部）

**SA**: SA-04 §報修工單 | **SD**: SD-04 §3 | **API**: `POST /v1/auth/repair`  
**Service**: `RepairTicketService.createFromFault()` / `createDirect()` | **SRS**: SRS-05-004 | **Spec**: §5-4

**商業規則**：
- 從障礙工單建立（FaultTicket → RepairTicket）
- 直接建立（指定 deviceId）
- deviceId 不存在 → 拋錯

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-011-01 | API | 新增工單 | perm=REPAIR_MANAGE | POST /repair | 200 | HTTP 200 | ✅ RepairTicketControllerTest.create |
| TC-04-011-02 | Error | 無權限 | no perm | POST /repair | 403 | HTTP 403 | ✅ RepairTicketControllerTest.create_noPermission |
| TC-04-011-03 | Happy | 從障礙建立 | faultTicket exists | createFromFault(faultId) | ticket created | 含 faultTicketId | ✅ RepairTicketServiceTest.createFromFault |
| TC-04-011-04 | Error | 障礙不存在 | invalid faultId | createFromFault(999) | not found | errorCode | ✅ RepairTicketServiceTest.createFromFault_notFound |
| TC-04-011-05 | Happy | 直接建立 | device exists | createDirect(deviceId) | ticket created | status=PENDING | ✅ RepairTicketServiceTest.createDirect |
| TC-04-011-06 | Error | 設備不存在 | invalid deviceId | createDirect(999) | not found | errorCode | ✅ RepairTicketServiceTest.createDirect_deviceNotFound |

---

### FN-04-012 查看工單詳情

**SA**: SA-04 §報修工單 | **SD**: SD-04 §3 | **API**: `GET /v1/auth/repair/{id}`  
**Service**: `RepairTicketService.getById()` | **SRS**: SRS-05-008 | **Spec**: §5-8

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-012-01 | API | 查詢詳情 | ticket exists | GET /repair/{id} | 200 | HTTP 200 | ✅ RepairTicketControllerTest.getById |
| TC-04-012-02 | Error | 不存在 | invalid id | GET /repair/{id} | 404 | HTTP 404 | ✅ RepairTicketControllerTest.getById_notFound |
| TC-04-012-03 | Error | Service 不存在 | invalid id | getById() | not found | errorCode | ✅ RepairTicketServiceTest.getById_notFound |

---

### FN-04-013 收案

**SA**: SA-04 §報修流程 | **SD**: SD-04 §3 | **API**: `PUT /v1/auth/repair/{id}/accept`  
**Service**: `RepairTicketService.accept()` | **SRS**: SRS-05-006 | **Spec**: §5-6

**商業規則**：
- 僅 PENDING 狀態可收案
- 收案後 → ACCEPTED

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-013-01 | API | 收案 | perm=REPAIR_MANAGE | PUT /repair/{id}/accept | 200 | HTTP 200 | ✅ RepairTicketControllerTest.accept |
| TC-04-013-02 | Happy | Service 收案 | status=PENDING | accept() | status=ACCEPTED | status 更新 | ✅ RepairTicketServiceTest.accept_success |
| TC-04-013-03 | Error | 狀態不合 | status≠PENDING | accept() | 拋錯 | invalid status | ✅ RepairTicketServiceTest.accept_invalidStatus |

---

### FN-04-014 派工

**SA**: SA-04 §報修流程 | **SD**: SD-04 §3 | **API**: `POST /v1/auth/repair/{id}/dispatch`  
**Service**: `RepairDispatchService.dispatch()` | **SRS**: SRS-05-004 | **Spec**: §5-4

**商業規則**：
- 僅 ACCEPTED 或 TRANSFERRED 狀態可派工
- 記錄派工人員、派工時間

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-014-01 | API | 派工 | perm=REPAIR_MANAGE | POST /repair/{id}/dispatch | 200 | HTTP 200 | ✅ RepairTicketControllerTest.dispatch |
| TC-04-014-02 | Happy | 正常派工 | status=ACCEPTED | dispatch() | status=DISPATCHED | status + assignee | ✅ RepairDispatchServiceTest.dispatch_success |
| TC-04-014-03 | Error | 狀態不合 | status≠ACCEPTED | dispatch() | 拋錯 | invalid status | ✅ RepairDispatchServiceTest.dispatch_invalidStatus |
| TC-04-014-04 | Error | 工單不存在 | invalid id | dispatch() | not found | errorCode | ✅ RepairDispatchServiceTest.dispatch_notFound |
| TC-04-014-05 | Happy | TRANSFERRED 可派工 | status=TRANSFERRED | dispatch() | status=DISPATCHED | from transferred | ✅ RepairDispatchServiceTest.dispatch_fromTransferred |
| TC-04-014-06 | Happy | 查詢派工紀錄 | dispatch exists | getByTicketId() | dispatch record | assignee info | ✅ RepairDispatchServiceTest.getByTicketId |

---

### FN-04-015 開始處理

**SA**: SA-04 §報修流程 | **SD**: SD-04 §3 | **API**: `PUT /v1/auth/repair/{id}/start`  
**SRS**: SRS-05-006 | **Spec**: §5-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-015-01 | Happy | 開始處理 | status=DISPATCHED | PUT /repair/{id}/start | status=IN_PROGRESS | status 更新 | ⬜ 待補 |

---

### FN-04-016 完工回報

**SA**: SA-04 §報修流程 | **SD**: SD-04 §3 | **API**: `PUT /v1/auth/repair/{id}/complete`  
**Service**: `RepairTicketService.reportCompletion()` | **SRS**: SRS-05-004 | **Spec**: §5-4

**商業規則**：
- 僅 IN_PROGRESS 狀態可完工
- 非 IN_PROGRESS → 拋錯

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-016-01 | API | 完工回報 | perm=REPAIR_MANAGE | PUT /repair/{id}/complete | 200 | HTTP 200 | ✅ RepairTicketControllerTest.complete |
| TC-04-016-02 | Happy | Service 完工 | status=IN_PROGRESS | reportCompletion() | status=COMPLETED | status | ✅ RepairTicketServiceTest.reportCompletion |
| TC-04-016-03 | Error | 狀態不合 | status≠IN_PROGRESS | reportCompletion() | 拋錯 | invalid status | ✅ RepairTicketServiceTest.reportCompletion_wrongStatus |

---

### FN-04-017 ~ FN-04-019 送審/結案/退回

> 簽核相關操作透過 Workflow Engine 執行，參照 TS-02。

| FN ID | 功能名稱 | TC 設計 | 自動化 |
|-------|---------|---------|--------|
| FN-04-017 | 送審 | → TS-02 FN-02-005 | ↗ 參照 TS-02 |
| FN-04-018 | 結案審核通過 | → TS-02 FN-02-006 | ↗ 參照 TS-02 |
| FN-04-019 | 退回補件 | → TS-02 FN-02-007 | ↗ 參照 TS-02 |

---

### FN-04-020 改分轉送

**SA**: SA-04 §報修流程 | **SD**: SD-04 §3 | **API**: `PUT /v1/auth/repair/{id}/transfer`  
**Service**: `RepairTicketService.transfer()` | **SRS**: SRS-05-006 | **Spec**: §5-6

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-020-01 | API | 改分轉送 | perm=REPAIR_MANAGE | PUT /repair/{id}/transfer | 200 | HTTP 200 | ✅ RepairTicketControllerTest.transfer |

---

### FN-04-021 匯出案件清冊

**SA**: SA-04 §匯出 | **SD**: SD-04 §3 | **API**: `GET /v1/auth/repair/export`  
**SRS**: SRS-05-013 | **Spec**: §5-13

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-021-01 | Happy | 匯出清冊 | perm=REPAIR_EXPORT | GET /repair/export | 200, file | Content-Type | ⬜ 待實作 |

---

## 5. 工單欄位 & 歷程 (FN-04-022 ~ FN-04-025)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-04-022 | 報修資訊欄位 | — | 含於 FN-04-012 詳情查詢 | — |
| FN-04-023 | 維護資訊欄位 | — | 含於 FN-04-012 詳情查詢 | — |
| FN-04-024 | 設備維護履歷 | TC-04-024-01 | GET /devices/{id}/repair-history → 200 | ⬜ 待實作 |
| FN-04-025 | 設備概覽（報修時） | — | 組合 FN-03-004 + FN-04-024 | — |

---

## 6. 報表產出 (FN-04-026 ~ FN-04-028)

> **實作狀態**：Phase 5+ (未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-04-026 | 通知機關報告單 | TC-04-026-01 | GET /repair/{id}/report/notification → PDF | ⬜ 待實作 |
| FN-04-027 | 查報單 | TC-04-027-01 | GET /repair/{id}/report/investigation → PDF | ⬜ 待實作 |
| FN-04-028 | 設計單 | TC-04-028-01 | GET /repair/{id}/report/design → PDF | ⬜ 待實作 |

---

## 7. 事件處理 (FN-04-029)

### FN-04-029 結案後更新圖資

**SA**: SA-04 §事件 | **SD**: SD-04 §4 | **API**: (E9 事件處理)  
**Service**: Event Listener | **SRS**: SRS-05-010

→ 由 RepairClosedListener 處理，見下方 §9

---

## 8. 巡查管理 (FN-04-030 ~ FN-04-037)

### FN-04-030 巡查任務列表

**SA**: SA-04 §巡查 | **SD**: SD-04 §3 | **API**: `GET /v1/auth/inspection`  
**Service**: `InspectionService` | **SRS**: SRS-05-014 | **Spec**: §5-14

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-030-01 | API | 列表查詢 | perm=INSPECTION_VIEW | GET /inspection | 200 | HTTP 200 | ✅ InspectionControllerTest.listTasks |
| TC-04-030-02 | Error | 無權限 | no perm | GET /inspection | 403 | HTTP 403 | ✅ InspectionControllerTest.listTasks_noPermission |

---

### FN-04-031 新增巡查任務

**SA**: SA-04 §巡查 | **SD**: SD-04 §3 | **API**: `POST /v1/auth/inspection`  
**Service**: `InspectionService.createTask()` | **SRS**: SRS-05-014 | **Spec**: §5-14

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-031-01 | API | 新增任務 | perm=INSPECTION_MANAGE | POST /inspection | 200 | HTTP 200 | ✅ InspectionControllerTest.createTask |
| TC-04-031-02 | Happy | Service 新增 | valid data | createTask() | task created | id 非 null | ✅ InspectionServiceTest.createTask |

---

### FN-04-032 編輯巡查任務

**SA**: SA-04 §巡查 | **SD**: SD-04 §3 | **API**: `PUT /v1/auth/inspection/{id}`  
**SRS**: SRS-05-014 | **Spec**: §5-14

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-032-01 | Happy | 編輯任務 | task exists | PUT /inspection/{id} | 200 | 欄位更新 | ⬜ 待補 |

---

### FN-04-033 停用巡查任務

**SA**: SA-04 §巡查 | **SD**: SD-04 §3 | **API**: `PUT /v1/auth/inspection/{id}/disable`  
**Service**: `InspectionService.deactivateTask()` | **SRS**: SRS-05-014 | **Spec**: §5-14

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-033-01 | API | 停用任務 | perm=INSPECTION_MANAGE | PUT /inspection/{id}/disable | 200 | HTTP 200 | ✅ InspectionControllerTest.deactivateTask |
| TC-04-033-02 | Happy | Service 停用 | task active | deactivateTask() | status=INACTIVE | status | ✅ InspectionServiceTest.deactivateTask |
| TC-04-033-03 | Error | 任務不存在 | invalid id | deactivateTask(999) | not found | errorCode | ✅ InspectionServiceTest.getTaskById_notFound |

---

### FN-04-034 巡查紀錄列表

**SA**: SA-04 §巡查 | **SD**: SD-04 §3 | **API**: `GET /v1/auth/inspection/{taskId}/records`  
**SRS**: SRS-05-014 | **Spec**: §5-14

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-034-01 | API | 查詢紀錄 | task has records | GET /inspection/{taskId}/records | 200 | HTTP 200 | ✅ InspectionControllerTest.listRecords |

---

### FN-04-035 新增巡查紀錄

**SA**: SA-04 §巡查 | **SD**: SD-04 §3 | **API**: `POST /v1/auth/inspection/{taskId}/records`  
**Service**: `InspectionService.createRecord()` | **SRS**: SRS-05-014 | **Spec**: §5-14

**商業規則**：
- 正常 → 僅記錄，不產生障礙
- 需維修 → 自動建立 FaultTicket（E13 事件）
- 需維修但無設備 → 不建立 FaultTicket

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-035-01 | API | 新增紀錄 | perm=INSPECTION_MANAGE | POST /inspection/{taskId}/records | 200 | HTTP 200 | ✅ InspectionControllerTest.createRecord |
| TC-04-035-02 | Happy | 正常—無障礙 | result=NORMAL | createRecord() | record, no fault | fault 未建立 | ✅ InspectionServiceTest.createRecord_normal_noFault |
| TC-04-035-03 | Happy | 需維修 → 自動建障礙 | result=NEED_REPAIR, device exists | createRecord() | record + faultTicket | E13 觸發 | ✅ InspectionServiceTest.createRecord_needRepair_createsFault |
| TC-04-035-04 | Edge | 需維修但無設備 | result=NEED_REPAIR, no device | createRecord() | record, no fault | 無設備跳過 | ✅ InspectionServiceTest.createRecord_needRepair_noDevice |

---

### FN-04-036 ~ FN-04-037 巡查進階

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-04-036 | 編輯巡查紀錄 | TC-04-036-01 | PUT /inspection/records/{id} → 200 | ⬜ 待補 |
| FN-04-037 | 巡查派工 | TC-04-037-01 | POST /inspection/{id}/dispatch → 200 | ⬜ 待實作 |

---

## 9. 事件監聽器 (跨切面)

### FaultApprovedListener

**SA**: SA-04 §事件 | **SD**: SD-04 §4 | **API**: (內部 Event)  
**Service**: `FaultApprovedListener` | **SRS**: SRS-05-004

**商業規則**：
- FAULT_REVIEW 完成 → 自動建立報修工單
- 非 FAULT_REVIEW 或非正確 step → 跳過
- 無設備 → 跳過

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-E1-01 | Happy | 障礙確認 → 建報修 | FAULT_REVIEW CONFIRMED | event | RepairTicket created | ticket.faultId | ✅ FaultApprovedListenerTest.onFaultConfirmed_createsRepair |
| TC-04-E1-02 | Edge | 非 FAULT_REVIEW | type=REPAIR_DISPATCH | event | 跳過 | no action | ✅ FaultApprovedListenerTest.wrongWorkflowType_skips |
| TC-04-E1-03 | Edge | 非正確 step | step≠CONFIRMED | event | 跳過 | no action | ✅ FaultApprovedListenerTest.wrongStep_skips |
| TC-04-E1-04 | Edge | 無設備 | deviceId=null | event | 跳過 | no action | ✅ FaultApprovedListenerTest.noDevice_skips |

---

### RepairClosedListener

**SA**: SA-04 §事件 | **SD**: SD-04 §4 | **API**: (內部 Event)  
**Service**: `RepairClosedListener` | **SRS**: SRS-05-010

**商業規則**：
- REPAIR_CLOSE 完成 → 更新設備狀態
- 非 REPAIR_CLOSE → 跳過
- 無設備 → 跳過

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-04-E2-01 | Happy | 結案 → 更新設備 | REPAIR_CLOSE COMPLETED | event | device status updated | device.status | ✅ RepairClosedListenerTest.onRepairClosed_updatesDevice |
| TC-04-E2-02 | Edge | 非 REPAIR_CLOSE | type=FAULT_REVIEW | event | 跳過 | no action | ✅ RepairClosedListenerTest.wrongWorkflowType_skips |
| TC-04-E2-03 | Edge | 無設備 | deviceId=null | event | 跳過 | no action | ✅ RepairClosedListenerTest.noDevice_skips |

---

## 10. 進階功能 (FN-04-038 ~ FN-04-047)

> **實作狀態**：Phase 5+ (大部分未實作)

| FN ID | 功能名稱 | TC ID | TC 設計 | 自動化 |
|-------|---------|-------|---------|--------|
| FN-04-038 | 非契約案件篩選 | TC-04-038-01 | GET /repair?contractFilter=NONE → filtered list | ⬜ 待實作 |
| FN-04-039 | 開放資料匯出 | TC-04-039-01 | GET /repair/export/open-data → JSON | ⬜ 待實作 |
| FN-04-040 | 里長通知設定 | TC-04-040-01 | POST/PUT /repair/borough-notify → 200 | ⬜ 待實作 |
| FN-04-041 | 里內故障通知推送 | TC-04-041-01 | 事件驅動推送通知 | ⬜ 待實作 |
| FN-04-042 | 維護案件統計 | TC-04-042-01 | GET /repair/statistics → 200 | ⬜ 待實作 |
| FN-04-043 | 維護時間統計 | TC-04-043-01 | GET /repair/statistics/time → avg/min/max | ⬜ 待實作 |
| FN-04-044 | 通報來源統計 | TC-04-044-01 | GET /repair/statistics/source → by source | ⬜ 待實作 |
| FN-04-045 | 故障分類統計 | TC-04-045-01 | GET /repair/statistics/fault-category | ⬜ 待實作 |
| FN-04-046 | 故障熱區統計 | TC-04-046-01 | GET /repair/statistics/heatmap → GeoJSON | ⬜ 待實作 |
| FN-04-047 | 材料換修統計 | TC-04-047-01 | GET /repair/statistics/material | ⬜ 待實作 |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ✅ 已有自動化 | 40 |
| ⬜ 待補（已實作 FN） | 3 |
| ⬜ 待實作（未實作 FN） | 20 |
| ↗ 參照 TS-02 | 3 FN |
| **總 TC 數** | **63** |

### 待補測試優先序

| 優先 | FN | 待建 TC | 原因 |
|------|-----|--------|------|
| 1 | FN-04-015 | 1 TC | 開始處理—核心流程節點，已實作缺測試 |
| 2 | FN-04-009/032/036 | 3 TC | 附件刪除、巡查編輯，已實作缺測試 |
| 3 | FN-04-001~003 | 3 TC | 民眾報修（公開 API） |
| 4 | FN-04-042~047 | 6 TC | 統計報表（Phase 5+） |
| 5 | FN-04-026~028 | 3 TC | PDF 報表（Phase 5+） |
