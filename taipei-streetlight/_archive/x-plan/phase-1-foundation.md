# Phase 1：地基層 — 04 資產 + 03 簽核引擎 + 障礙偵測

> Flyway V30 ~ V32 ｜涵蓋模組：04 資產管理、03 簽核引擎
> 前置條件：無（本 Phase 為整條鏈的起點）
> 後續依賴：Phase 2（報修派工）依賴本 Phase 的 devices + workflow + fault_tickets

---

## 總覽

| Step | Flyway | 範圍 | 產出 |
|------|--------|------|------|
| 1 | V30 | 04 devices + circuits + device_events + contracts | 6 張表 + indexes + FK |
| 2 | V31 | 03 workflow engine 全套 | 5 張表 + seed 流程定義 |
| 3 | V32 | 04 fault_tickets + fault_correlations + menu + permission | 2 張表 + 選單 + 權限 + role binding |

---

## Step 1：V30 — 設備主表 + 回路 + 契約（04）

### 1-1 Flyway Migration

**檔案**：`V30__device__create_tables.sql`

**建表順序**（依 FK 依賴）：
1. `contracts`（無 FK 依賴其他新表）
2. `devices`（FK → contracts, dept_info）
3. `circuits`（FK → devices, `panel_box_device_id` **nullable**）
4. `ALTER devices ADD FK → circuits`（雙向 FK，後補）
5. `device_events`（FK → devices）

> DDL 細節已在 [4-3-資產資訊管理.md](4-3-資產資訊管理.md) §1 及 [cross-module](cross-module-03-07-unified-design.md) §2-3 定義，此處不重複。
> **D-6 Graceful Degradation**：所有 FK / 拓撲 / 回路欄位 nullable，circuits UNIQUE 改為 `(tenant_id, circuit_number)`。

### 1-1a 設計原則：Graceful Degradation

IoT 設備形態多元，系統不假定所有欄位都有值：

| 場景 | 有的資料 | 沒有的 | 系統行為 |
|------|----------|--------|----------|
| 只有路燈 | LUMINAIRE 基本欄位 | 回路、Gateway、契約 | CRUD + 地圖 + 工單全可用，拓撲退化為平面列表 |
| 路燈 + 回路編號 | 上 + circuit_number | 分電箱、Gateway | + 回路維度關聯偵測啟用 |
| 路燈 + Gateway | 上 + parent_device_id | 回路 | + 拓撲樹狀檢視 + Gateway 心跳偵測 |
| 電池 IoT（無市電） | LUMINAIRE, connectivity=DIRECT | 回路、分電箱 | 回路偵測跳過，SIM 到期偵測視 network_config |
| 完整路燈系統 | 全欄位 | — | 所有功能啟用 |

Service 層寫法：**null-check first, skip if absent**。

### 1-2 後端實作清單

| # | 類別 | 檔案 | 說明 |
|---|------|------|------|
| 1 | Entity | `Device.java` | `@Filter("tenantFilter")`, `TenantAware`, `TenantEntityListener`, `@JdbcTypeCode(SqlTypes.JSON)` for attributes/network_config |
| 2 | Entity | `DeviceType.java` (enum) | POLE, LUMINAIRE, PANEL_BOX, CONTROLLER, POWER_EQUIPMENT, ATTACHMENT |
| 3 | Entity | `DeviceStatus.java` (enum) | ACTIVE, REPORTED, UNDER_REPAIR, INACTIVE, DECOMMISSIONED |
| 4 | Entity | `ConnectivityType.java` (enum) | NONE, DIRECT, GATEWAY |
| 5 | Entity | `Circuit.java` | `TenantAware`, FK → devices(panel_box_device_id) |
| 6 | Entity | `DeviceEvent.java` | `TenantAware`, FK → devices |
| 7 | Entity | `DeviceEventType.java` (enum) | INSTALL, REPLACE, REPAIR, INSPECT, ADOPT, DECOMMISSION, MATERIAL_CHANGE |
| 8 | Entity | `Contract.java` | `TenantAware`, JSONB attributes |
| 9 | Entity | `ContractStatus.java` (enum) | ACTIVE, EXPIRED, TERMINATED |
| 10 | Repository | `DeviceRepository.java` | extends `JpaRepository` + `TenantScopedRepository`, 自訂查詢（分頁+篩選+DataScope） |
| 11 | Repository | `CircuitRepository.java` | `TenantScopedRepository` |
| 12 | Repository | `DeviceEventRepository.java` | `TenantScopedRepository` |
| 13 | Repository | `ContractRepository.java` | `TenantScopedRepository` |
| 14 | DTO | `DeviceRequest.java` | 含 `@Valid` 驗證（deviceType 必填、deviceCode 必填） |
| 15 | DTO | `DeviceResponse.java` | 含 deptName, parentDeviceCode, circuitNumber, childrenCount |
| 16 | DTO | `DeviceStatsResponse.java` | totalDevices, byType, byStatus, onlineRate, openFaults |
| 17 | DTO | `CircuitRequest.java` / `CircuitResponse.java` | |
| 18 | DTO | `ContractRequest.java` / `ContractResponse.java` | |
| 19 | Service | `DeviceService.java` | CRUD + DataScope 驗證 + 拓撲循環防護（recursive CTE）+ JSONB 大小限制 |
| 20 | Service | `CircuitService.java` | CRUD + 刪除限制（CIRCUIT_HAS_DEVICES） |
| 21 | Service | `DeviceEventService.java` | 歷程寫入（供 05/06 結案時呼叫） |
| 22 | Service | `ContractService.java` | CRUD + 保固到期提醒 |
| 23 | Service | `DeviceExportService.java` | ODS / XLS / CSV 匯出，展開 JSONB attributes |
| 24 | Controller | `DeviceController.java` | 8 個端點（見 4-3 x-plan §5-1） |
| 25 | Controller | `CircuitController.java` | 5 個端點（見 4-3 x-plan §5-2） |
| 26 | Controller | `ContractController.java` | CRUD + 列表 |

### 1-2a 設備狀態生命週期

```
                 ┌──────────┐
     手動停用──→  │ INACTIVE │  ←── 手動停用
     │           │  (停用)   │           │
     │           └────┬─────┘           │
     │            手動復用               │
     │                │                 │
     ↑                ↓                 ↑
┌─────────┐    E1:障礙確認    ┌──────────────┐   E4:派工    ┌──────────────┐
│ ACTIVE  │ ─────────────→ │   REPORTED    │ ──────────→ │ UNDER_REPAIR │
│ (正常)  │                │   (已報修)     │              │  (維修中)     │
└─────────┘                └──────────────┘              └──────┬───────┘
     ↑                                                         │
     │                   E9/E10:結案審核通過                     │
     └─────────────────────────────────────────────────────────┘

     ACTIVE / INACTIVE → DECOMMISSIONED (已除帳/報廢，不可逆)
```

| 狀態 | 中文 | 觸發條件 | 說明 |
|------|------|----------|------|
| `ACTIVE` | 正常 | 預設值 / E9 結案審核通過 | 設備正常運作 |
| `REPORTED` | 已報修 | E1 障礙申告審核通過 | 已確認有障礙，等待派工 |
| `UNDER_REPAIR` | 維修中 | E4 報修派工（dispatch 建立） | 已派工，施工進行中 |
| `INACTIVE` | 停用 | 手動操作 | 行政停用（非障礙，如移設、暫停） |
| `DECOMMISSIONED` | 已除帳 | 手動操作 | 報廢，不可逆 |

> **注意**：REPORTED / UNDER_REPAIR 只由工作流事件自動觸發，不可手動設定。

### 1-3 DataScope 整合

```java
// DeviceService.java
@DataPermission
public Page<DeviceResponse> listDevices(DeviceQueryParams params, Pageable pageable) {
    Set<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();
    return deviceRepository.findByFilters(
        params.getDeviceType(), params.getStatus(), params.getKeyword(),
        visibleDeptIds, pageable
    );
}
```

### 1-4 拓撲循環防護

```java
// DeviceService.java — 新增/編輯 parent_device_id 時呼叫
private void validateNoCircularReference(Long childId, Long parentId) {
    if (parentId == null) return;
    boolean hasCircle = deviceRepository.checkCircularReference(childId, parentId);
    if (hasCircle) {
        throw new BusinessException(ErrorCode.DEVICE_CIRCULAR_REFERENCE);
    }
}
```

Repository 使用 native query：
```java
@Query(value = """
    WITH RECURSIVE ancestors AS (
        SELECT id, parent_device_id FROM devices WHERE id = :parentId
        UNION ALL
        SELECT d.id, d.parent_device_id
        FROM devices d JOIN ancestors a ON d.id = a.parent_device_id
    )
    SELECT CASE WHEN EXISTS (SELECT 1 FROM ancestors WHERE id = :childId) THEN true ELSE false END
    """, nativeQuery = true)
boolean checkCircularReference(@Param("childId") Long childId, @Param("parentId") Long parentId);
```

### 1-5 審計事件

在 `AuditCategory` 新增 `ASSET`，在 `AuditEventType` 新增：
- `CREATE_DEVICE`, `UPDATE_DEVICE`, `DELETE_DEVICE`, `EXPORT_DEVICE`
- `CREATE_CIRCUIT`, `UPDATE_CIRCUIT`
- `CREATE_CONTRACT`, `UPDATE_CONTRACT`

### 1-6 ErrorCode 新增

```java
DEVICE_NOT_FOUND(60001),
DEVICE_CODE_DUPLICATE(60002),
DEVICE_CIRCULAR_REFERENCE(60003),
DEVICE_HAS_CHILDREN(60004),
DEVICE_HAS_OPEN_FAULTS(60005),
CIRCUIT_NOT_FOUND(60010),
CIRCUIT_HAS_DEVICES(60011),
CONTRACT_NOT_FOUND(60020),
```

### 1-7 前端（04 資產管理頁面）

| # | 檔案 | 說明 |
|---|------|------|
| 1 | `src/types/device.ts` | TypeScript 型別定義（Device, Circuit, Contract, DeviceType enum 等） |
| 2 | `src/api/device/index.ts` | 設備/回路/契約 API layer |
| 3 | `src/stores/deviceStore.ts` | 設備統計快取 |
| 4 | `src/views/admin/asset/DeviceManagementView.vue` | 設備列表 + 篩選 + 動態表單 Dialog（依 device_type 切換專有欄位） |
| 5 | `src/views/admin/asset/CircuitManagementView.vue` | 回路管理 |
| 6 | `src/views/admin/asset/ContractManagementView.vue` | 契約管理 |
| 7 | `src/views/admin/asset/DeviceTopologyView.vue` | 拓撲檢視（樹狀結構 + 在線狀態） |
| 8 | i18n | zh-TW / en / zh-CN 新增 asset 相關 key |
| 9 | router | 新增 /admin/asset/* 路由 |

### 1-8 單元測試

| # | 測試類 | 涵蓋範圍 |
|---|--------|----------|
| 1 | `DeviceControllerTest` | 所有端點 + 權限驗證 |
| 2 | `DeviceServiceTest` | CRUD + 循環防護 + DataScope + JSONB 驗證 |
| 3 | `CircuitControllerTest` | CRUD + 刪除限制 |
| 4 | `CircuitServiceTest` | CIRCUIT_HAS_DEVICES 驗證 |
| 5 | `ContractServiceTest` | CRUD + 狀態轉換 |
| 6 | `DeviceExportServiceTest` | 匯出格式驗證 |

---

## Step 2：V31 — 簽核引擎（03）

### 2-1 Flyway Migration

**檔案**：`V31__workflow__create_tables.sql`

**建表順序**：
1. `workflow_definitions`
2. `workflow_steps_template`（FK → workflow_definitions）
3. `workflow_instances`（FK → workflow_definitions, tenant）
4. `workflow_step_logs`（FK → workflow_instances）
5. `delegate_settings`
6. `INSERT workflow_definitions` seed（5 種流程）
7. `INSERT workflow_steps_template` seed（每種流程的步驟）

### 2-2 流程步驟 Seed 資料

```sql
-- FAULT_REVIEW 流程步驟
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('FAULT_REVIEW', 1, 'OPEN',           '申告建立',   NULL,       NULL),
('FAULT_REVIEW', 2, 'REVIEW',         '審核確認',   'OPERATOR', NULL),
('FAULT_REVIEW', 3, 'CONFIRMED',      '確認通過',   NULL,       'AUTO_CREATE_REPAIR'),
('FAULT_REVIEW', 4, 'REJECTED',       '駁回(誤報)', NULL,       NULL),
('FAULT_REVIEW', 5, 'MERGED',         '合併(關聯)', NULL,       NULL);

-- REPAIR_DISPATCH 流程步驟
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role) VALUES
('REPAIR_DISPATCH', 1, 'PENDING',     '未收案',     NULL),
('REPAIR_DISPATCH', 2, 'ACCEPTED',    '已收案',     'OPERATOR'),
('REPAIR_DISPATCH', 3, 'DISPATCHED',  '已派工',     'OPERATOR'),
('REPAIR_DISPATCH', 4, 'IN_PROGRESS', '處理中',     'FIELD_USER'),
('REPAIR_DISPATCH', 5, 'COMPLETION_REPORTED', '完工回報', 'FIELD_USER'),
('REPAIR_DISPATCH', 6, 'TRANSFERRED', '改分轉送',   'OPERATOR');

-- REPAIR_CLOSE 流程步驟
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('REPAIR_CLOSE', 1, 'PENDING_REVIEW', '完工審核中', NULL,       NULL),
('REPAIR_CLOSE', 2, 'CLOSED',         '結案',      NULL,       'AUTO_SYNC_ASSET'),
('REPAIR_CLOSE', 3, 'RETURNED',       '退回補件',   NULL,       NULL);

-- REPLACEMENT_REVIEW 流程步驟
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('REPLACEMENT_REVIEW', 1, 'DRAFT',          '草稿',      NULL,       NULL),
('REPLACEMENT_REVIEW', 2, 'DISPATCHED',     '已派工',    'OPERATOR', NULL),
('REPLACEMENT_REVIEW', 3, 'IN_PROGRESS',    '施工中',    'FIELD_USER', NULL),
('REPLACEMENT_REVIEW', 4, 'SELF_CHECKED',   '自主檢核',  NULL,       NULL),
('REPLACEMENT_REVIEW', 5, 'PENDING_REVIEW', '報竣審核',  'OPERATOR', NULL),
('REPLACEMENT_REVIEW', 6, 'CLOSED',         '結案',      NULL,       'AUTO_SYNC_ASSET'),
('REPLACEMENT_REVIEW', 7, 'RETURNED',       '退回補件',  NULL,       NULL);

-- ASSET_CHANGE 流程步驟
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('ASSET_CHANGE', 1, 'DRAFT',          '草稿',      NULL,       NULL),
('ASSET_CHANGE', 2, 'PENDING_REVIEW', '審核中',    'DEPT_ADMIN', NULL),
('ASSET_CHANGE', 3, 'APPROVED',       '核准',      NULL,       NULL),
('ASSET_CHANGE', 4, 'APPLIED',        '已生效',    NULL,       'AUTO_SYNC_ASSET'),
('ASSET_CHANGE', 5, 'RETURNED',       '退回補件',  NULL,       NULL);
```

### 2-3 後端實作清單

| # | 類別 | 檔案 | 說明 |
|---|------|------|------|
| 1 | Entity | `WorkflowDefinition.java` | 全域表（不需 TenantAware） |
| 2 | Entity | `WorkflowStepsTemplate.java` | 全域表 |
| 3 | Entity | `WorkflowInstance.java` | `TenantAware`, ticket_type + ticket_id 多態 FK |
| 4 | Entity | `WorkflowStepLog.java` | `TenantAware`, JSONB attachments + before/after snapshot, **代理欄位**: originalAssigneeId, isDelegated |
| 5 | Entity | `DelegateSetting.java` | `TenantAware`, **end_date 必填（禁止無限期代理）**, reason |
| 6 | Enum | `WorkflowType.java` | FAULT_REVIEW, REPAIR_DISPATCH, REPAIR_CLOSE, REPLACEMENT_REVIEW, ASSET_CHANGE |
| 7 | Enum | `WorkflowStatus.java` | ACTIVE, COMPLETED, CANCELLED |
| 8 | Enum | `WorkflowAction.java` | SUBMIT, APPROVE, REJECT, RETURN, DISPATCH, MERGE, COMPLETE, CANCEL |
| 9 | Enum | `TicketType.java` | FAULT_TICKET, REPAIR_TICKET, REPLACEMENT_ORDER, ASSET_CHANGE |
| 10 | Repository | `WorkflowDefinitionRepository.java` | 全域（不實作 TenantScopedRepository） |
| 11 | Repository | `WorkflowStepsTemplateRepository.java` | 全域 |
| 12 | Repository | `WorkflowInstanceRepository.java` | `TenantScopedRepository` |
| 13 | Repository | `WorkflowStepLogRepository.java` | `TenantScopedRepository` |
| 14 | Repository | `DelegateSettingRepository.java` | `TenantScopedRepository` |
| 15 | DTO | `WorkflowTransitionRequest.java` | targetStep, action, comment, attachments |
| 16 | DTO | `WorkflowInstanceResponse.java` | 含 ticketType, ticketId, currentStep, assignedToName |
| 17 | DTO | `WorkflowStepLogResponse.java` | 含 actorName, action, comment, attachments, actedAt |
| 18 | DTO | `DelegateSettingRequest.java` / `DelegateSettingResponse.java` | |
| 19 | Event | `WorkflowTransitionEvent.java` | Spring ApplicationEvent，攜帶 instance + targetStep + action |
| 20 | Service | `WorkflowService.java` (interface) | 統一介面（見 cross-module §5-2） |
| 21 | Service | `WorkflowServiceImpl.java` | **核心 FSM**：狀態轉換表 + 驗證 + 歷程記錄 + 事件發布 |
| 22 | Service | `DelegateService.java` | CRUD + 代理期間驗證 + 重疊檢查 |
| 23 | Controller | `WorkflowController.java` | 待辦列表、流程歷程、狀態轉換 |
| 24 | Controller | `DelegateController.java` | 代理人 CRUD |

### 2-4 狀態轉換表（核心）

```java
// WorkflowServiceImpl.java 中的靜態定義
private static final Map<String, Map<String, Set<String>>> TRANSITIONS = Map.of(
    "FAULT_REVIEW", Map.of(
        "OPEN",    Set.of("REVIEW"),
        "REVIEW",  Set.of("CONFIRMED", "REJECTED", "MERGED")
    ),
    "REPAIR_DISPATCH", Map.of(
        "PENDING",    Set.of("ACCEPTED"),
        "ACCEPTED",   Set.of("DISPATCHED"),
        "DISPATCHED", Set.of("IN_PROGRESS", "TRANSFERRED"),
        "IN_PROGRESS", Set.of("COMPLETION_REPORTED")
    ),
    "REPAIR_CLOSE", Map.of(
        "COMPLETION_REPORTED", Set.of("PENDING_REVIEW"),
        "PENDING_REVIEW",     Set.of("CLOSED", "RETURNED"),
        "RETURNED",           Set.of("COMPLETION_REPORTED")
    ),
    "REPLACEMENT_REVIEW", Map.of(
        "DRAFT",          Set.of("DISPATCHED"),
        "DISPATCHED",     Set.of("IN_PROGRESS"),
        "IN_PROGRESS",    Set.of("SELF_CHECKED"),
        "SELF_CHECKED",   Set.of("PENDING_REVIEW"),
        "PENDING_REVIEW", Set.of("CLOSED", "RETURNED"),
        "RETURNED",       Set.of("PENDING_REVIEW")
    ),
    "ASSET_CHANGE", Map.of(
        "DRAFT",          Set.of("PENDING_REVIEW"),
        "PENDING_REVIEW", Set.of("APPROVED", "RETURNED"),
        "APPROVED",       Set.of("APPLIED"),
        "RETURNED",       Set.of("PENDING_REVIEW")
    )
);
```

### 2-5 審計事件

`AuditCategory` 新增 `WORKFLOW`，`AuditEventType` 新增：
- `WORKFLOW_SUBMIT`, `WORKFLOW_APPROVE`, `WORKFLOW_REJECT`, `WORKFLOW_RETURN`, `WORKFLOW_DISPATCH`

### 2-6 ErrorCode 新增

```java
WORKFLOW_INSTANCE_NOT_FOUND(90001),
WORKFLOW_INVALID_TRANSITION(90002),
WORKFLOW_NOT_ASSIGNED_TO_USER(90003),
WORKFLOW_SELF_APPROVAL_NOT_ALLOWED(90004),
DELEGATE_PERIOD_OVERLAP(90010),
DELEGATE_SELF_NOT_ALLOWED(90011),
DELEGATE_END_DATE_REQUIRED(90012),
```

### 2-6a 代理人機制（詳細設計）

#### 核心規則

| 規則 | 說明 |
|------|------|
| end_date 必填 | 禁止無限期代理，每次代理都是一筆有起訖的設定 |
| 不可自我代理 | DB constraint `chk_delegate_not_self` + Service 驗證 |
| 同一委託人同時只能有一個有效代理 | `DELEGATE_PERIOD_OVERLAP` — 日期區間不可重疊 |
| 代理人限同部門 | 代理人必須與委託人同一 dept_id（主管的下屬） |
| 代理範圍：只能簽核 | ✅ 審核通過 / 駁回 / 退回。❌ 不能以主管身份起案、不能修改代理設定 |
| 過期自動失效 | 查詢時以 `start_date <= today AND end_date >= today` 過濾 |

#### 代理人設定流程

```
李主管（DEPT_ADMIN, dept=6）出差 4/22~4/25
  → 進入「代理人設定」頁面
  → 選擇代理人：王大明（OPERATOR, dept=6, 同部門）
  → 填寫起訖日期：2026-04-22 ~ 2026-04-25
  → 填寫原因：出差
  → 儲存 → delegate_settings 寫入一筆
```

#### 代理簽核流程

```java
// WorkflowServiceImpl.transition() — 簽核時的代理判斷

public void transition(Long instanceId, String targetStep,
                       Long actorId, String action, String comment, ...) {
    WorkflowInstance instance = findInstance(instanceId);

    // 1. 送件人不可自審（防止自己送件自己簽）
    if (instance.getCreatorId().equals(actorId)) {
        throw new BusinessException(ErrorCode.WORKFLOW_SELF_APPROVAL_NOT_ALLOWED);
    }

    // 2. 檢查 actorId 是否為原始 assignee，或其有效代理人
    Long originalAssigneeId = resolveAssignee(instance);  // 原始簽核人（主管）
    boolean isDelegated = false;

    if (!originalAssigneeId.equals(actorId)) {
        // actorId 不是原始簽核人 → 檢查是否為有效代理人
        DelegateSetting delegate = delegateService
            .findActiveDelegate(originalAssigneeId, instance.getTenantId())
            .orElseThrow(() -> new BusinessException(WORKFLOW_NOT_ASSIGNED_TO_USER));

        if (!delegate.getDelegateId().equals(actorId)) {
            throw new BusinessException(WORKFLOW_NOT_ASSIGNED_TO_USER);
        }
        isDelegated = true;
    }

    // 3. 寫入 step_log，標記代理資訊
    String finalComment = isDelegated
        ? String.format("[代理簽核] 代理人：%s，代理 %s。%s",
            actorName, originalAssigneeName, comment)
        : comment;

    WorkflowStepLog log = WorkflowStepLog.builder()
        .instanceId(instanceId)
        .stepCode(targetStep)
        .action(action)
        .actorId(actorId)
        .actorName(actorName)
        .originalAssigneeId(isDelegated ? originalAssigneeId : null)
        .isDelegated(isDelegated)
        .comment(finalComment)
        .build();
    stepLogRepo.save(log);

    // 4. 狀態轉換 + 事件發布（同原有邏輯）
    ...
}
```

#### 待辦列表查詢（含代理案件）

```java
// WorkflowService — 查詢「我的待辦」
public Page<WorkflowInstanceResponse> getMyPendingTasks(Long userId, Pageable pageable) {
    // 1. 我自己被指派的待辦
    Set<Long> assigneeIds = new HashSet<>();
    assigneeIds.add(userId);

    // 2. 誰把我設為代理人（且日期有效）
    List<DelegateSetting> activeDelegations = delegateService
        .findActiveDelegationsForDelegate(userId, TenantContext.getTenantId());
    for (DelegateSetting ds : activeDelegations) {
        assigneeIds.add(ds.getDelegatorId());  // 加入委託人的 ID
    }

    // 3. 查詢所有 assignee ∈ assigneeIds 的待辦
    Page<WorkflowInstance> instances = instanceRepo
        .findPendingByAssignees(assigneeIds, pageable);

    // 4. Response 中標記哪些是代理案件
    return instances.map(inst -> {
        WorkflowInstanceResponse resp = mapper.toResponse(inst);
        if (!inst.getCurrentAssigneeId().equals(userId)) {
            resp.setDelegatedFrom(inst.getCurrentAssigneeName());  // 「代理 李主管」
        }
        return resp;
    });
}
```

#### 日期過期處理

```
4/26（代理過期後）：
  → 王大明的待辦列表查詢 → findActiveDelegationsForDelegate → 空集合
  → 王大明只看到自己的待辦，李主管的工單不再出現
  → 若有新工單需李主管簽核 → 只出現在李主管自己的待辦

若代理期間有未結案件：
  → 已簽核的 → step_log 記錄不變（歷史紀錄保留 is_delegated=true）
  → 未簽核的 → 代理過期後回到李主管的待辦（因為代理人的查詢不再命中）
```

#### 防止自己送件自己審件

| 場景 | 系統行為 |
|------|----------|
| 張同仁（OPERATOR）起案 → 張同仁自己想審核 | ❌ `WORKFLOW_SELF_APPROVAL_NOT_ALLOWED`，creator_id == actor_id 被擋 |
| 張同仁起案 → 李主管審核 | ✅ 正常 |
| 張同仁起案 → 李主管的代理人王大明審核 | ✅ 正常（王大明 ≠ 張同仁） |
| 李主管自己起案 → 李主管的代理人王大明審核 | ✅ 正常（不同人） |
| 李主管自己起案 → 李主管自己想審核 | ❌ `WORKFLOW_SELF_APPROVAL_NOT_ALLOWED` |
| SUPER_ADMIN 起案 → SUPER_ADMIN 自己審核 | ❌ 一樣被擋，SUPER_ADMIN 不豁免自審規則 |

### 2-7 前端

| # | 檔案 | 說明 |
|---|------|------|
| 1 | `src/types/workflow.ts` | WorkflowInstance, WorkflowStepLog, DelegateSetting 型別 |
| 2 | `src/api/workflow/index.ts` | 待辦列表、流程歷程、狀態轉換 API |
| 3 | `src/views/admin/workflow/PendingTasksView.vue` | 待辦案件列表（跨工單類型） |
| 4 | `src/views/admin/workflow/DelegateSettingsView.vue` | 代理人管理 |
| 5 | `src/components/WorkflowStepper.vue` | **共用元件**：`<el-steps>` 渲染流程歷程，供各工單詳情頁引用 |
| 6 | `src/components/WorkflowActionBar.vue` | **共用元件**：根據 currentStep 顯示可用操作按鈕（通過/退回/派工） |

### 2-8 單元測試

| # | 測試類 | 涵蓋範圍 |
|---|--------|----------|
| 1 | `WorkflowServiceTest` | 所有 5 種流程的合法/非法轉換 |
| 2 | `WorkflowServiceTest` | **自審防護**：creator_id == actor_id → 拋 SELF_APPROVAL_NOT_ALLOWED |
| 3 | `WorkflowServiceTest` | **代理簽核**：代理人可簽、step_log 記錄 is_delegated=true + original_assignee_id |
| 4 | `WorkflowServiceTest` | **代理過期**：過期後代理人無法簽核 → 拋 NOT_ASSIGNED_TO_USER |
| 5 | `WorkflowControllerTest` | 端點 + 權限 |
| 6 | `WorkflowControllerTest` | 待辦列表含代理案件、標記 delegatedFrom |
| 7 | `DelegateServiceTest` | CRUD + 重疊驗證 + 自我代理拒絕 + end_date 必填 + 同部門驗證 |

---

## Step 3：V32 — 障礙工單 + 關聯偵測（04）

### 3-1 Flyway Migration

**檔案**：`V32__fault__create_tables.sql`

**內容**：
1. `fault_tickets`（FK → devices, circuits）
2. `fault_correlations`
3. `ALTER fault_tickets ADD FK → fault_correlations`
4. Menu：35(資產管理), 36(設備管理), 37(回路管理), 38(障礙工單), 39(契約管理)
5. Permission：DEVICE_VIEW, DEVICE_MANAGE, CIRCUIT_VIEW, CIRCUIT_MANAGE, FAULT_VIEW, FAULT_MANAGE, DEVICE_EXPORT, CONTRACT_VIEW, CONTRACT_MANAGE
6. Role binding

> Menu 35~38 的 DDL 已在 [4-3 x-plan §1-6](4-3-資產資訊管理.md) 定義，39(契約管理) 新增。

### 3-2 後端實作清單

| # | 類別 | 檔案 | 說明 |
|---|------|------|------|
| 1 | Entity | `FaultTicket.java` | `TenantAware`, FK → devices, circuits, fault_correlations |
| 2 | Entity | `FaultTicketSource.java` (enum) | CITIZEN_REPORT, PATROL, AUTO_ALERT |
| 3 | Entity | `FaultTicketStatus.java` (enum) | OPEN, IN_PROGRESS, RESOLVED, MERGED |
| 4 | Entity | `FaultCorrelation.java` | `TenantAware` |
| 5 | Entity | `RootCauseType.java` (enum) | CIRCUIT, PANEL_BOX, GATEWAY, POWER_OUTAGE |
| 6 | Repository | `FaultTicketRepository.java` | `TenantScopedRepository`, 分頁+篩選查詢 |
| 7 | Repository | `FaultCorrelationRepository.java` | `TenantScopedRepository` |
| 8 | DTO | `FaultTicketRequest.java` / `FaultTicketResponse.java` | |
| 9 | DTO | `FaultCorrelationResponse.java` | |
| 10 | Service | `FaultTicketService.java` | CRUD + 新增時自動觸發關聯偵測 |
| 11 | Service | `FaultCorrelationService.java` | **關聯偵測核心邏輯**（三維度：回路/Gateway/地理） |
| 12 | Controller | `FaultTicketController.java` | 7 個端點（見 4-3 x-plan §5-3） |

### 3-3 關聯偵測邏輯

```java
@Service
public class FaultCorrelationService {

    /** 被動偵測：新工單建立後同步檢查（null-check first, skip if absent） */
    public void detectOnNewTicket(FaultTicket ticket) {
        // 維度 1：同回路近 30 分鐘內 ≥ 3 筆（有回路才偵測）
        if (ticket.getCircuitId() != null) {
            long count = faultTicketRepo.countRecentByCircuit(
                ticket.getCircuitId(), ticket.getTenantId(),
                LocalDateTime.now().minusMinutes(30));
            if (count >= 3) {
                createCorrelation(RootCauseType.CIRCUIT, ticket.getCircuitId(), ticket.getTenantId());
            }
        }
        // ↑ 若 circuit_id == null（只有路燈/電池IoT），此維度自動跳過

        // 維度 2：同 Gateway 下是否已有 GATEWAY 告警（有 parent 才偵測）
        if (ticket.getDeviceId() != null) {
            Device device = deviceRepo.findById(ticket.getDeviceId()).orElse(null);
            if (device != null && device.getParentDeviceId() != null
                    && device.getConnectivityType() == ConnectivityType.GATEWAY) {
                mergeToExistingCorrelation(device.getParentDeviceId(), ticket);
            }
        }
        // ↑ 若 parent_device_id == null（直連/無通訊），此維度自動跳過
    }

    /** 主動偵測：定時排程每 5 分鐘（只掃有 Gateway 角色的設備） */
    @Scheduled(fixedRate = 300_000)
    public void scanGatewayHeartbeats() {
        // 只查 connectivity_type='DIRECT' 且 parent_device_id IS NOT NULL 的 parent
        // 若整個租戶沒有 Gateway 設備 → 查詢結果為空 → 不做任何動作
    }

    /** 預防偵測：每日 09:00（只掃有 SIM 的設備） */
    @Scheduled(cron = "0 0 9 * * *")
    public void scanSimExpiry() {
        // 只查 network_config->>'sim_expiry' IS NOT NULL
        // 電池 IoT 無 SIM / 無市電設備 → 自動不在查詢範圍
    }
}
```

### 3-4 前端

| # | 檔案 | 說明 |
|---|------|------|
| 1 | `src/types/fault.ts` | FaultTicket, FaultCorrelation 型別 |
| 2 | `src/api/fault/index.ts` | 障礙工單 + 關聯障礙 API |
| 3 | `src/views/admin/asset/FaultTicketView.vue` | 障礙工單列表 + 新增 + 處理 |
| 4 | `src/views/admin/asset/FaultCorrelationView.vue` | 關聯障礙列表 + 確認 + 解決 |

### 3-5 單元測試

| # | 測試類 | 涵蓋範圍 |
|---|--------|----------|
| 1 | `FaultTicketControllerTest` | 所有端點 + 權限 |
| 2 | `FaultTicketServiceTest` | CRUD + 工單建立觸發關聯偵測 |
| 3 | `FaultCorrelationServiceTest` | 三維度偵測邏輯 + 合併工單 |

---

## Phase 1 完成標準

- [ ] V30 migration 執行成功，devices / circuits / device_events / contracts 表建立
- [ ] V31 migration 執行成功，workflow 5 張表 + seed 資料
- [ ] V32 migration 執行成功，fault_tickets / fault_correlations + 選單權限
- [ ] 04 設備 CRUD API 可用，含 DataScope 過濾
- [ ] 04 回路 CRUD API 可用，含刪除限制
- [ ] 04 契約 CRUD API 可用
- [ ] 04 設備匯出（XLS/CSV）可用
- [ ] 03 WorkflowService 核心 FSM 可用，5 種流程狀態轉換正確
- [ ] 03 代理人設定 CRUD 可用
- [ ] 04 障礙工單 CRUD + 關聯偵測可用
- [ ] 前端設備管理 / 回路管理 / 契約管理 / 障礙工單頁面可用
- [ ] 前端待辦案件 / 代理人設定頁面可用
- [ ] 所有單元測試通過
- [ ] `mvn test` 全過
