# TS-02 簽核引擎 — Test Specification

> **對應 SA**：SA-02-approval (FN-02-001 ~ FN-02-022)  
> **對應 SD**：SD-02-approval  
> **Test Classes**：4 classes, 37 test methods  
> **最新驗證**：2026-04-24 · 全部 PASS

---

## 使用方式

本文件是 **AI 實作 prompt 的驗收標準**。請 AI 實作或修改某個 FN 時，附上對應段落：

```
請實作 FN-02-005 送審
【SA】SA-02 §簽核流程
【SD】SD-02 §4 FSM + §5 Sequence
【TC】（貼 FN-02-005 的 Test Cases 表）
請確保所有 TC PASS。
```

---

## 1. 簽核流程操作 (FN-02-001 ~ FN-02-010)

### FN-02-001 待辦案件列表

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §3 | **API**: `GET /v1/auth/workflow/pending`  
**Service**: `WorkflowService.getMyPendingTasks()` | **SRS**: SRS-03-001 | **Spec**: §3-1

**商業規則**：
- 包含自己的待辦 + 代理人委託的待辦
- 多租戶隔離

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-001-01 | API | 查詢待辦 | authenticated | GET /workflow/pending | 200 | HTTP 200 | ✅ WorkflowControllerTest.getPendingTasks |
| TC-02-001-02 | Error | 未登入 | no token | GET /workflow/pending | 401 | HTTP 401 | ✅ WorkflowControllerTest.getPendingTasks_noToken |
| TC-02-001-03 | Happy | 含代理人委託 | 有代理委託 | getMyPendingTasks | 含被代理的案件 | includes delegated | ✅ WorkflowServiceTest.getMyPendingTasks_includesDelegated |

---

### FN-02-002 待辦案件數量

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §3 | **API**: `GET /v1/auth/workflow/pending/count`  
**Service**: `WorkflowService.getPendingCount()` | **SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-002-01 | Happy | 待辦數量 | 有 pending tasks | GET /pending/count | 200, count ≥ 1 | count 值 | ⬜ 待補 |

---

### FN-02-003 流程歷程查詢

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §3 | **API**: `GET /v1/auth/workflow/{instanceId}/logs`  
**Service**: `WorkflowService.getStepLogs()` | **SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-003-01 | API | 查詢歷程 | instance exists | GET /workflow/{id}/logs | 200, log list | HTTP 200 | ✅ WorkflowControllerTest.getStepLogs |

---

### FN-02-004 流程狀態查詢

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §3 | **API**: `GET /v1/auth/workflow/instance`  
**Service**: `WorkflowService.getInstance()` | **SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-004-01 | Happy | 查詢狀態 | instance exists | GET /workflow/instance | 200, currentStep | step 正確 | ⬜ 待補 |

---

### FN-02-005 送審

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §4 FSM | **API**: `POST /v1/auth/workflow/{instanceId}/transition`  
**Service**: `WorkflowService.transition()` | **SRS**: SRS-03-001 | **Spec**: §3-1

**商業規則**：
- FSM 狀態轉換：OPEN → REVIEW（合法）
- 非法轉換 → 拋錯
- 已完成的 instance 不可再操作
- 自審防護：提交人不可自己審核

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-005-01 | API | 送審 API | authenticated | POST /workflow/{id}/transition | 200 | HTTP 200 | ✅ WorkflowControllerTest.transition |
| TC-02-005-02 | Happy | OPEN → REVIEW 合法 | FAULT_REVIEW, step=OPEN | action=SUBMIT | step=REVIEW | currentStep | ✅ WorkflowServiceTest.transition_OPEN_to_REVIEW_legal |
| TC-02-005-03 | Error | 非法狀態轉換 | step 與 action 不匹配 | illegal action | 拋出 BusinessException | errorCode | ✅ WorkflowServiceTest.transition_illegal |
| TC-02-005-04 | Error | 已完成 instance | status=COMPLETED | transition | 拋錯 | 不可操作 | ✅ WorkflowServiceTest.transition_completedInstance |
| TC-02-005-05 | Error | 自審防護 | submitter == approver | transition | `SELF_APPROVAL` | errorCode | ✅ WorkflowServiceTest.transition_selfApproval |

---

### FN-02-006 審核通過

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §4 FSM | **API**: `POST /v1/auth/workflow/{instanceId}/transition`  
**Service**: `WorkflowService.transition()` | **SRS**: SRS-03-001 | **Spec**: §3-1

**商業規則**：
- REVIEW → CONFIRMED：流程完成，觸發下游事件

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-006-01 | Happy | 審核通過（完成） | REVIEW step | action=APPROVE | status=COMPLETED | status 欄位 | ✅ WorkflowServiceTest.transition_REVIEW_CONFIRMED_completed |

---

### FN-02-007 退回補件

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §4 FSM | **API**: `POST /v1/auth/workflow/{instanceId}/transition`  
**Service**: `WorkflowService.transition()` | **SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-007-01 | Happy | 退回補件 | REVIEW step | action=RETURN | step 回退 | currentStep | ⬜ 待補 |

---

### FN-02-008 駁回

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §4 FSM | **API**: `POST /v1/auth/workflow/{instanceId}/transition`  
**Service**: `WorkflowService.transition()` | **SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-008-01 | Happy | 駁回 | REVIEW step | action=REJECT | status=REJECTED | status 欄位 | ⬜ 待補 |

---

### FN-02-009 取消流程

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §3 | **API**: `POST /v1/auth/workflow/{instanceId}/cancel`  
**Service**: `WorkflowService.cancel()` | **SRS**: SRS-03-001 | **Spec**: §3-1

**商業規則**：
- 僅提交者可取消
- 已完成不可取消

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-009-01 | API | 取消流程 | 提交者操作 | POST /workflow/{id}/cancel | 200 | HTTP 200 | ✅ WorkflowControllerTest.cancel |
| TC-02-009-02 | Error | 無權限取消 | 非提交者 | POST /workflow/{id}/cancel | 403 | HTTP 403 | ✅ WorkflowControllerTest.cancel_noPermission |
| TC-02-009-03 | Happy | 狀態設為 CANCELLED | pending instance | cancel() | status=CANCELLED | status | ✅ WorkflowServiceTest.cancel_setsCancelled |
| TC-02-009-04 | Error | 已完成不可取消 | status=COMPLETED | cancel() | 拋錯 | errorCode | ✅ WorkflowServiceTest.cancel_alreadyCompleted |

---

### FN-02-010 派工（簽核中）

**SA**: SA-02 §簽核流程 | **SD**: SD-02 §4 FSM | **API**: `POST /v1/auth/workflow/{instanceId}/transition`  
**Service**: `WorkflowService.transition()` | **SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-010-01 | Happy | REPAIR_DISPATCH 流程 | dispatch instance | transition | step 更新 | currentStep | ✅ WorkflowServiceTest.transition_repairDispatch |
| TC-02-010-02 | Happy | ASSET_CHANGE 流程 | asset change instance | transition | step 更新 | currentStep | ✅ WorkflowServiceTest.transition_assetChange |

---

## 2. 流程類型定義 (FN-02-011 ~ FN-02-015)

### FN-02-011 FAULT_REVIEW 流程

**SA**: SA-02 §流程定義 | **SD**: SD-02 §4 | **API**: (內部)  
**Service**: `WorkflowService.createInstance()` | **SRS**: SRS-03-001 | **Spec**: §3-1

**商業規則**：
- 障礙確認流程：OPEN → REVIEW → CONFIRMED
- 觸發條件：障礙工單建立

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-011-01 | Happy | 建立 FAULT_REVIEW | — | createInstance(FAULT_REVIEW) | instance, step=OPEN | type + step | ✅ WorkflowServiceTest.createInstance_faultReview_OPEN |
| TC-02-011-02 | Error | 未知流程類型 | — | createInstance(UNKNOWN) | 拋錯 | errorCode | ✅ WorkflowServiceTest.createInstance_unknownType |

---

### FN-02-012 REPAIR_DISPATCH 流程

**SA**: SA-02 §流程定義 | **SD**: SD-02 §4 | **API**: (內部)  
**SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-012-01 | Happy | REPAIR_DISPATCH 轉換 | dispatch instance | transition | step 正確 | FSM | ✅ WorkflowServiceTest.transition_repairDispatch |

---

### FN-02-013 REPAIR_CLOSE 流程

**SA**: SA-02 §流程定義 | **SD**: SD-02 §4 | **API**: (內部)  
**SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-013-01 | Happy | REPAIR_CLOSE 完成 | close instance | transition | COMPLETED | status | ⬜ 待補 |

---

### FN-02-014 REPLACEMENT_REVIEW 流程

**SA**: SA-02 §流程定義 | **SD**: SD-02 §4 | **API**: (內部)  
**SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-014-01 | Happy | REPLACEMENT_REVIEW 轉換 | replacement instance | transition | step 正確 | FSM | ⬜ 待補 |

---

### FN-02-015 ASSET_CHANGE 流程

**SA**: SA-02 §流程定義 | **SD**: SD-02 §4 | **API**: (內部)  
**SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-015-01 | Happy | ASSET_CHANGE 轉換 | asset change instance | transition | step 正確 | FSM | ✅ WorkflowServiceTest.transition_assetChange |

---

## 3. 代理人管理 (FN-02-016 ~ FN-02-020)

### FN-02-016 代理人列表查詢

**SA**: SA-02 §代理人 | **SD**: SD-02 §3 | **API**: `GET /v1/auth/delegates`  
**Service**: `DelegateService.getMyDelegates()` | **SRS**: SRS-03-002 | **Spec**: §3-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-016-01 | API | 查詢代理列表 | authenticated | GET /delegates | 200 | HTTP 200 | ✅ DelegateControllerTest.list |
| TC-02-016-02 | Error | 未登入 | no token | GET /delegates | 401 | HTTP 401 | ✅ DelegateControllerTest.list_noToken |
| TC-02-016-03 | Error | 無權限 | no DELEGATE perm | GET /delegates | 403 | HTTP 403 | ✅ DelegateControllerTest.list_noPermission |
| TC-02-016-04 | Happy | 我的代理列表 | has active delegates | getMyDelegates() | delegate list | 含 active 代理 | ✅ DelegateServiceTest.getMyDelegates |

---

### FN-02-017 新增代理人

**SA**: SA-02 §代理人 | **SD**: SD-02 §3 | **API**: `POST /v1/auth/delegates`  
**Service**: `DelegateService.create()` | **SRS**: SRS-03-002 | **Spec**: §3-2

**商業規則**：
- 不可自我代理
- endDate 可為 null（永久代理）
- 日期區間不可重疊

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-017-01 | API | 新增代理 | authenticated | POST /delegates | 200 | HTTP 200 | ✅ DelegateControllerTest.create |
| TC-02-017-02 | Happy | 正常新增 | valid delegate | create() | delegate created | id 非 null | ✅ DelegateServiceTest.create_success |
| TC-02-017-03 | Error | 自我代理 | delegator == delegate | create() | 拋錯 | errorCode | ✅ DelegateServiceTest.create_selfDelegation |
| TC-02-017-04 | Happy | endDate=null（永久） | — | endDate=null | 201 | endDate 為 null | ✅ DelegateServiceTest.create_endDateNull |
| TC-02-017-05 | Error | 日期重疊 | overlapping period | create() | 拋錯 | errorCode | ✅ DelegateServiceTest.create_overlapping |

---

### FN-02-018 停用代理

**SA**: SA-02 §代理人 | **SD**: SD-02 §3 | **API**: `PUT /v1/auth/delegates/{id}/deactivate`  
**Service**: `DelegateService.deactivate()` | **SRS**: SRS-03-002 | **Spec**: §3-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-018-01 | API | 停用代理 | delegate exists | PUT /delegates/{id}/deactivate | 200 | HTTP 200 | ✅ DelegateControllerTest.deactivate |
| TC-02-018-02 | Error | 無權限 | no perm | PUT /delegates/{id}/deactivate | 403 | HTTP 403 | ✅ DelegateControllerTest.deactivate_noPermission |
| TC-02-018-03 | Happy | 設為 INACTIVE | active delegate | deactivate() | status=INACTIVE | status | ✅ DelegateServiceTest.deactivate_setsInactive |
| TC-02-018-04 | Error | 不存在 | invalid id | deactivate() | not found | errorCode | ✅ DelegateServiceTest.deactivate_notFound |

---

### FN-02-019 代理到期自動失效

**SA**: SA-02 §代理人 | **SD**: SD-02 §3 | **API**: (系統排程)  
**Service**: 排程 Job | **SRS**: SRS-03-002 | **Spec**: §3-2

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-019-01 | Happy | 到期自動失效 | endDate < now | 排程觸發 | status=INACTIVE | 自動更新 | ⬜ 待實作 |

---

### FN-02-020 代理簽核執行

**SA**: SA-02 §代理人 | **SD**: SD-02 §4 | **API**: (同 FN-02-005~008)  
**Service**: `WorkflowService.transition()` | **SRS**: SRS-03-002 | **Spec**: §3-2

**商業規則**：
- 有效代理人可代行審核
- 無效代理 → 拋錯

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-020-01 | Happy | 有效代理簽核 | active delegate | transition (as delegate) | step 更新 | 代理成功 | ✅ WorkflowServiceTest.transition_validDelegate |
| TC-02-020-02 | Error | 無效代理 | expired delegate | transition (as delegate) | 拋錯 | errorCode | ✅ WorkflowServiceTest.transition_invalidDelegate |

---

## 4. 流程管理 (FN-02-021 ~ FN-02-022)

### FN-02-021 流程定義列表

**SA**: SA-02 §流程管理 | **SD**: SD-02 §3 | **API**: `GET /v1/auth/admin/workflow-definitions`  
**Service**: `WorkflowService.listDefinitions()` | **SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-021-01 | Happy | 列出流程定義 | ADMIN | GET /workflow-definitions | 200, list | 含 5 種流程 | ⬜ 待實作 |

---

### FN-02-022 流程步驟模板查詢

**SA**: SA-02 §流程管理 | **SD**: SD-02 §3 | **API**: `GET /v1/auth/admin/workflow-definitions/{type}/steps`  
**Service**: `WorkflowService.getSteps()` | **SRS**: SRS-03-001 | **Spec**: §3-1

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-022-01 | Happy | 查詢步驟模板 | ADMIN | GET /definitions/FAULT_REVIEW/steps | 200, step list | 含 OPEN, REVIEW, CONFIRMED | ⬜ 待實作 |

---

## 5. 代理人候選人 (補充)

### 候選人查詢

**SA**: SA-02 §代理人 | **SD**: SD-02 §3 | **API**: `GET /v1/auth/delegates/candidates`  
**Service**: `DelegateService.getCandidates()` | **SRS**: SRS-03-002 | **Spec**: §3-2

**商業規則**：
- ALL scope → 所有候選人
- DEPT scope → 本部門候選人

#### Test Cases

| TC ID | 類型 | 場景 | 前置條件 | 輸入 | 預期結果 | 驗證點 | 自動化 |
|-------|------|------|---------|------|---------|--------|--------|
| TC-02-C-01 | API | 查詢候選人 | authenticated | GET /delegates/candidates | 200 | HTTP 200 | ✅ DelegateControllerTest.candidates |
| TC-02-C-02 | Error | 無權限 | no perm | GET /delegates/candidates | 403 | HTTP 403 | ✅ DelegateControllerTest.candidates_noPermission |
| TC-02-C-03 | Happy | ALL scope | scope=ALL | getCandidates() | 所有用戶 | list 大 | ✅ DelegateServiceTest.getCandidates_allScope |
| TC-02-C-04 | Happy | DEPT scope | scope=DEPT | getCandidates() | 本部門用戶 | list 較小 | ✅ DelegateServiceTest.getCandidates_deptScope |

---

## 統計摘要

| 分類 | TC 數量 |
|------|---------|
| ✅ 已有自動化 | 33 |
| ⬜ 待補（已實作 FN） | 4 |
| ⬜ 待實作（未實作 FN） | 3 |
| **總 TC 數** | **40** |

### 待補測試優先序

| 優先 | FN | 待建 TC | 原因 |
|------|-----|--------|------|
| 1 | FN-02-007/008 | 2 TC | 退回/駁回是核心簽核操作，需明確測試 |
| 2 | FN-02-002/004 | 2 TC | 待辦數量/狀態查詢，API 已存在 |
| 3 | FN-02-013/014 | 2 TC | REPAIR_CLOSE / REPLACEMENT_REVIEW FSM |
| 4 | FN-02-019 | 1 TC | 代理到期排程 |
| 5 | FN-02-021/022 | 2 TC | 流程管理 API（Phase 3+） |
