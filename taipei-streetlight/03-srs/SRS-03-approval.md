# SRS-03 簽核管理

> **對應需求**：§3-(1) ~ §3-(2)  
> **設計參照**：`/02-spec/03-approval-workflow.md`、`/_archive/x-plan/phase-1-foundation.md`（Step 2）、`/_archive/x-plan/cross-module-03-07-unified-design.md`  
> **狀態**：✅ 已完成

---

## SRS-03-001 工作流程呈現

**來源**：§3-(1)

### User Story

> 身為**承辦人或相關人員**，我可隨時以「表列」與「圖示」兩種方式查看需陳核案件的處理狀態、經過的每一步驟與時間點。

### 主要流程

1. 案件提交後自動建立 `workflow_instance`，綁定案件類型與 ID
2. 系統依 `workflow_definitions` 載入該類型的審核步驟範本
3. 每一步驟執行後，寫入 `workflow_step_logs`（含執行者、時間、動作、備註、異動差異）
4. **表列模式**：以時間軸列表顯示每一步驟的執行者、時間、動作、備註
5. **圖示模式**：以 Stepper / 流程圖視覺化呈現，已完成步驟、當前步驟、待執行步驟

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-03-001-1 | 每一案件可切換表列/圖示兩種模式查看審核歷程 |
| AC-03-001-2 | 表列模式顯示：步驟名稱、執行者、執行時間、動作（核准/退回/抽回）、備註 |
| AC-03-001-3 | 圖示模式以 Stepper 呈現已完成/進行中/待執行步驟 |
| AC-03-001-4 | 每步驟可查看修改前後差異（delta） |
| AC-03-001-5 | 相關人員（承辦人、審核人、代理人）均可查看 |

### 資料模型

```
workflow_definitions: id, flow_type(FAULT_REVIEW/REPAIR_DISPATCH/REPAIR_CLOSE/
                     REPLACEMENT_REVIEW/ASSET_CHANGE), name, steps_json
workflow_instances: id, flow_type, ticket_type, ticket_id, status, 
                   creator_id, current_step
workflow_step_logs: id, instance_id, step_name, action(SUBMIT/APPROVE/REJECT/WITHDRAW), 
                   actor_id, actor_name, comment, delta_json, created_at
```

### 工作流類型（5 種）

| 類型 | 說明 | 對應模組 |
|------|------|---------|
| FAULT_REVIEW | 故障立案審核 | §4 / §5 |
| REPAIR_DISPATCH | 維修派工審核 | §5 |
| REPAIR_CLOSE | 維修結案審核 | §5 |
| REPLACEMENT_REVIEW | 換裝審核（自檢/報竣/結案） | §6 |
| ASSET_CHANGE | 資產異動審核（加帳/除帳/變更） | §4 |

### 狀態機轉換

```
PENDING → SUBMITTED → IN_REVIEW → APPROVED → COMPLETED
                  ↘ REJECTED → RESUBMITTED → IN_REVIEW
          SUBMITTED → WITHDRAWN → (可重新提交)
```

### 防弊機制

- 禁止自我審核（creator_id == actor_id 擋回，含 SUPER_ADMIN）
- 禁止跳步審核（必須按步驟順序）
- 每步驟只能由指定角色/人員執行

### 狀態：✅ 已完成（Phase 1 Step 2，含 FSM + 前端 WorkflowStepper/ActionBar）

---

## SRS-03-002 代理人設定

**來源**：§3-(2)

### User Story

> 身為 **GOV_STAFF / GOV_MGR**，我可指定同部門人員為代理人，代理期間代理人可於主畫面直接辦理我的案件。

### 主要流程

1. 使用者進入「代理設定」頁面，指定代理人與代理期間
2. 代理人必須為同部門人員，不可自我指定
3. 代理期間內，代理人登入後可看到被代理人的待辦案件
4. 代理人簽核時，紀錄中標記「代理 XXX」
5. 代理期間結束後自動失效，未處理案件回歸原承辦人

### 驗收準則

| AC ID | 驗收條件 |
|-------|---------|
| AC-03-002-1 | 可設定代理人與代理期間（起訖日期，end_date 必填） |
| AC-03-002-2 | 代理人僅限同部門、不可自我指定 |
| AC-03-002-3 | 不可設定無限期代理（需有結束日期） |
| AC-03-002-4 | 代理人不得再轉委代理（禁止無限鏈委託） |
| AC-03-002-5 | 代理期間，代理人於主畫面可見被代理人待辦案件 |
| AC-03-002-6 | 代理人簽核時，歷程紀錄中顯示「代理 [原承辦人姓名]」 |
| AC-03-002-7 | 代理期間結束後自動失效 |
| AC-03-002-8 | 時間重疊檢查：同一使用者同一時間不可有多個代理人 |

### 資料模型

```
delegate_settings: id, delegator_id, delegate_id, dept_id, 
                   start_date, end_date, status(ACTIVE/EXPIRED), tenant_id
```

### API 端點

| Method | Path | 說明 |
|--------|------|------|
| GET | `/v1/workflow/delegates` | 我的代理設定 |
| POST | `/v1/workflow/delegates` | 新增代理 |
| PUT | `/v1/workflow/delegates/{id}` | 修改代理 |
| DELETE | `/v1/workflow/delegates/{id}` | 取消代理 |

### 狀態：✅ 已完成
