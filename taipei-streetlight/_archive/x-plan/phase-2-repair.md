# Phase 2：報修派工 — 05 報修維護

> Flyway V33 ~ V34 ｜涵蓋模組：05 報修維護
> 前置條件：Phase 1 完成（devices + circuits + contracts + workflow engine + fault_tickets）
> 後續依賴：Phase 3（換裝維護）依賴本 Phase 的 repair_tickets

---

## 總覽

| Step | Flyway | 範圍 | 產出 |
|------|--------|------|------|
| 4 | V33 | 05 repair_tickets + repair_dispatches + ticket_attachments | 3 張表 + indexes |
| 5 | V34 | 05 inspection_tasks + inspection_records + menu + permission | 2 張表 + 選單 + 權限 + role binding |

---

## Step 4：V33 — 報修工單 + 派工 + 附件

### 4-1 Flyway Migration

**檔案**：`V33__repair__create_tables.sql`

**建表順序**（依 FK 依賴）：
1. `repair_tickets`（FK → fault_tickets, devices, circuits, contracts, dept_info）
2. `repair_dispatches`（FK → repair_tickets, contracts）
3. `ticket_attachments`（多態 FK，無硬 FK constraint）

> DDL 已在 [cross-module §2-3 / 05 區塊](cross-module-03-07-unified-design.md) 完整定義。

### 4-2 後端實作清單

| # | 類別 | 檔案 | 說明 |
|---|------|------|------|
| **Entities** | | | |
| 1 | Entity | `RepairTicket.java` | `TenantAware`, FK → FaultTicket, Device, Circuit, Contract |
| 2 | Entity | `RepairTicketStatus.java` (enum) | PENDING, ACCEPTED, DISPATCHED, IN_PROGRESS, COMPLETION_REPORTED, PENDING_REVIEW, RETURNED, TRANSFERRED, TRACKING, CLOSED |
| 3 | Entity | `RepairTicketSource.java` (enum) | FAULT_TICKET, CITIZEN_WEB, EXTERNAL_1999, PATROL, PHONE |
| 4 | Entity | `RepairTicketPriority.java` (enum) | LOW, NORMAL, HIGH, URGENT |
| 5 | Entity | `RepairDispatch.java` | `TenantAware`, FK → RepairTicket, Contract |
| 6 | Entity | `RepairDispatchStatus.java` (enum) | DISPATCHED, IN_PROGRESS, COMPLETED, CANCELLED |
| 7 | Entity | `TicketAttachment.java` | `TenantAware`, 多態（ticket_type + ticket_id） |
| 8 | Entity | `TicketType.java` (shared enum) | FAULT_TICKET, REPAIR_TICKET, REPLACEMENT_ORDER |
| 9 | Entity | `AttachmentPhase.java` (enum) | BEFORE, DURING, AFTER, REPORT |
| 10 | Entity | `ScanStatus.java` (enum) | PENDING, CLEAN, INFECTED |
| **Repositories** | | | |
| 11 | Repository | `RepairTicketRepository.java` | `TenantScopedRepository`, 分頁+多條件篩選+DataScope |
| 12 | Repository | `RepairDispatchRepository.java` | `TenantScopedRepository` |
| 13 | Repository | `TicketAttachmentRepository.java` | `TenantScopedRepository` |
| **DTOs** | | | |
| 14 | DTO | `RepairTicketRequest.java` | source, reporter info, device_id, fault_ticket_id, priority |
| 15 | DTO | `RepairTicketResponse.java` | 含 faultTicket summary, deviceCode, circuitNumber, contractName, currentStep, dispatchHistory |
| 16 | DTO | `RepairTicketQueryParams.java` | status, source, priority, deptId, keyword, dateRange |
| 17 | DTO | `DispatchRequest.java` | assignedTo, assignedOrg, contractId, dueDate, note |
| 18 | DTO | `CompletionReportRequest.java` | repairDescription, faultCause, attachments[] |
| 19 | DTO | `AttachmentUploadRequest.java` | fileType, description, gpsLat, gpsLng, takenAt, phase |
| 20 | DTO | `AttachmentResponse.java` | |
| **Services** | | | |
| 21 | Service | `RepairTicketService.java` | 見 §4-3 核心業務邏輯 |
| 22 | Service | `RepairDispatchService.java` | 派工管理 |
| 23 | Service | `TicketAttachmentService.java` | 上傳+病毒掃描狀態+查詢 |
| **Event Listeners** | | | |
| 24 | Listener | `FaultApprovedListener.java` | 監聽 E1：障礙審核通過 → 自動建立 repair_ticket |
| 25 | Listener | `RepairClosedListener.java` | 監聽 E9：結案審核通過 → 呼叫 DeviceEventService 同步資產 |
| **Controllers** | | | |
| 26 | Controller | `RepairTicketController.java` | 見 §4-4 API 端點 |
| 27 | Controller | `TicketAttachmentController.java` | 附件上傳/下載/列表 |

### 4-3 核心業務邏輯

#### 報修工單建立（雙路徑）

```java
@Service
public class RepairTicketService {

    /**
     * 路徑 A：障礙審核通過 → 自動建立
     * 由 FaultApprovedListener 呼叫
     */
    public RepairTicket createFromFault(Long faultTicketId) {
        FaultTicket fault = faultTicketRepo.findById(faultTicketId)
            .orElseThrow(() -> new BusinessException(FAULT_TICKET_NOT_FOUND));

        RepairTicket ticket = RepairTicket.builder()
            .ticketNumber(generateTicketNumber())  // RT-20260422-001
            .faultTicketId(faultTicketId)
            .deviceId(fault.getDeviceId())
            .circuitId(fault.getCircuitId())
            .source(RepairTicketSource.FAULT_TICKET)
            .reporterName(fault.getReporterName())
            .reportDescription(fault.getDescription())
            .status(RepairTicketStatus.PENDING)
            .priority(RepairTicketPriority.NORMAL)
            .build();

        RepairTicket saved = repo.save(ticket);

        // 建立 workflow instance (REPAIR_DISPATCH)
        workflowService.createInstance(
            "REPAIR_DISPATCH", "REPAIR_TICKET", saved.getId(), null);

        return saved;
    }

    /**
     * 路徑 B：外部系統(1999)/ 民眾網頁 / 電話 → 手動立案
     * 此路徑不經過 fault_ticket，直接建立 repair_ticket
     */
    @AuditEvent(AuditEventType.CREATE_REPAIR_TICKET)
    public RepairTicket createDirect(RepairTicketRequest request) {
        // 驗證 device_id 存在（若有填）
        if (request.getDeviceId() != null) {
            deviceRepo.findById(request.getDeviceId())
                .orElseThrow(() -> new BusinessException(DEVICE_NOT_FOUND));
        }

        RepairTicket ticket = mapper.toEntity(request);
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setStatus(RepairTicketStatus.PENDING);

        RepairTicket saved = repo.save(ticket);

        workflowService.createInstance(
            "REPAIR_DISPATCH", "REPAIR_TICKET", saved.getId(), null);

        return saved;
    }
}
```

#### 派工流程

```java
@AuditEvent(AuditEventType.DISPATCH_REPAIR)
public RepairDispatch dispatch(Long ticketId, DispatchRequest request) {
    RepairTicket ticket = findTicketOrThrow(ticketId);
    validateStatus(ticket, RepairTicketStatus.ACCEPTED);

    // 1. 建立派工紀錄
    RepairDispatch dispatch = RepairDispatch.builder()
        .repairTicketId(ticketId)
        .assignedTo(request.getAssignedTo())
        .assignedOrg(request.getAssignedOrg())
        .contractId(request.getContractId())
        .dueDate(request.getDueDate())
        .dispatchNote(request.getNote())
        .dispatchedBy(SecurityContextUtils.getCurrentUserId())
        .status(RepairDispatchStatus.DISPATCHED)
        .build();
    dispatchRepo.save(dispatch);

    // 2. 更新工單狀態
    ticket.setStatus(RepairTicketStatus.DISPATCHED);
    repo.save(ticket);

    // 3. Workflow 轉換
    WorkflowInstance instance = workflowService.findByTicket("REPAIR_TICKET", ticketId);
    workflowService.dispatch(instance.getId(),
        SecurityContextUtils.getCurrentUserId(),
        request.getAssignedTo(),
        "派工給 " + request.getAssignedOrg());

    return dispatch;
}
```

#### 完工回報

```java
@AuditEvent(AuditEventType.COMPLETE_REPAIR)
public void reportCompletion(Long ticketId, CompletionReportRequest request) {
    RepairTicket ticket = findTicketOrThrow(ticketId);
    validateStatus(ticket, RepairTicketStatus.IN_PROGRESS);

    // 1. 更新維修描述
    ticket.setRepairDescription(request.getRepairDescription());
    ticket.setFaultCause(request.getFaultCause());
    ticket.setCompletedAt(LocalDateTime.now());
    ticket.setStatus(RepairTicketStatus.COMPLETION_REPORTED);
    repo.save(ticket);

    // 2. 上傳附件（維修前/中/後照片含 GPS）
    for (AttachmentUploadRequest att : request.getAttachments()) {
        TicketAttachment attachment = TicketAttachment.builder()
            .ticketType(TicketType.REPAIR_TICKET)
            .ticketId(ticketId)
            .fileType(att.getFileType())
            .fileUrl(att.getFileUrl())
            .description(att.getDescription())
            .gpsLat(att.getGpsLat())
            .gpsLng(att.getGpsLng())
            .takenAt(att.getTakenAt())
            .phase(att.getPhase())
            .scanStatus(ScanStatus.PENDING)
            .build();
        attachmentRepo.save(attachment);
    }

    // 3. Workflow 轉換 + 事件（通知 dispatched_by）
    WorkflowInstance instance = workflowService.findByTicket("REPAIR_TICKET", ticketId);
    workflowService.submit(instance.getId(),
        SecurityContextUtils.getCurrentUserId(),
        "完工回報：" + request.getRepairDescription(),
        request.getAttachments());
}
```

### 4-4 API 端點

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/v1/repair/tickets` | REPAIR_VIEW | 分頁列表+篩選+DataScope |
| GET | `/v1/repair/tickets/{id}` | REPAIR_VIEW | 單筆詳情（含派工歷史+附件+流程歷程） |
| POST | `/v1/repair/tickets` | REPAIR_MANAGE | 手動立案（1999/民眾/電話） |
| PUT | `/v1/repair/tickets/{id}` | REPAIR_MANAGE | 更新工單資訊 |
| POST | `/v1/repair/tickets/{id}/accept` | REPAIR_MANAGE | 收案 |
| POST | `/v1/repair/tickets/{id}/dispatch` | REPAIR_DISPATCH | 派工 |
| POST | `/v1/repair/tickets/{id}/complete` | REPAIR_MANAGE | 完工回報 |
| POST | `/v1/repair/tickets/{id}/transfer` | REPAIR_MANAGE | 改分轉送 |
| GET | `/v1/repair/tickets/{id}/attachments` | REPAIR_VIEW | 附件列表 |
| POST | `/v1/repair/tickets/{id}/attachments` | REPAIR_MANAGE | 上傳附件 |
| GET | `/v1/repair/attachments/{id}/download` | REPAIR_VIEW | 下載附件 |

### 4-5 事件監聽器

#### E1：障礙審核通過 → 自動建立報修工單 + 設備狀態→已報修

```java
@Component
public class FaultApprovedListener {

    private final RepairTicketService repairTicketService;
    private final DeviceService deviceService;

    @EventListener
    @Transactional
    public void onFaultConfirmed(WorkflowTransitionEvent event) {
        if (!"FAULT_REVIEW".equals(event.getWorkflowType())) return;
        if (!"CONFIRMED".equals(event.getTargetStep())) return;

        // 1. 自動建立報修工單
        repairTicketService.createFromFault(event.getTicketId());

        // 2. 設備狀態 → 已報修
        FaultTicket fault = faultTicketRepo.findById(event.getTicketId()).orElseThrow();
        if (fault.getDeviceId() != null) {
            deviceService.updateStatus(fault.getDeviceId(), DeviceStatus.REPORTED);
        }
    }
}
```

#### E4：報修派工 → 設備狀態→維修中

```java
@Component
public class RepairDispatchedListener {

    private final DeviceService deviceService;

    @EventListener
    @Transactional
    public void onRepairDispatched(WorkflowTransitionEvent event) {
        if (!"REPAIR_DISPATCH".equals(event.getWorkflowType())) return;
        if (!"DISPATCHED".equals(event.getTargetStep())) return;

        RepairTicket ticket = repairTicketRepo.findById(event.getTicketId()).orElseThrow();
        if (ticket.getDeviceId() != null) {
            deviceService.updateStatus(ticket.getDeviceId(), DeviceStatus.UNDER_REPAIR);
        }
    }
}
```

#### E9：報修結案審核通過 → 設備狀態→正常 + 同步資產

```java
@Component
public class RepairClosedListener {

    private final DeviceEventService deviceEventService;
    private final DeviceService deviceService;

    @EventListener
    @Transactional
    public void onRepairClosed(WorkflowTransitionEvent event) {
        if (!"REPAIR_CLOSE".equals(event.getWorkflowType())) return;
        if (!"CLOSED".equals(event.getTargetStep())) return;

        RepairTicket ticket = repairTicketRepo.findById(event.getTicketId())
            .orElseThrow();

        // 1. 恢復設備狀態：維修中 → 正常
        if (ticket.getDeviceId() != null) {
            deviceService.updateStatus(ticket.getDeviceId(), DeviceStatus.ACTIVE);
        }

        // 2. 寫入 device_events 歷程
        deviceEventService.recordEvent(DeviceEvent.builder()
            .deviceId(ticket.getDeviceId())
            .eventType(DeviceEventType.REPAIR)
            .description(ticket.getRepairDescription())
            .repairTicketId(ticket.getId())
            .build());
    }
}
```

### 4-6 審計事件

`AuditCategory` 使用已註冊的 `MAINTENANCE`，`AuditEventType` 新增：
- `CREATE_REPAIR_TICKET`, `UPDATE_REPAIR_TICKET`, `CLOSE_REPAIR_TICKET`
- `DISPATCH_REPAIR`, `COMPLETE_REPAIR`

### 4-7 ErrorCode 新增

```java
REPAIR_TICKET_NOT_FOUND(70001),
REPAIR_TICKET_INVALID_STATUS(70002),
DISPATCH_NOT_FOUND(70010),
```

---

## Step 5：V34 — 巡查管理 + 報修選單權限

### 5-1 Flyway Migration

**檔案**：`V34__inspection__create_tables.sql`

**內容**：
1. `inspection_tasks`（FK → dept_info）
2. `inspection_records`（FK → inspection_tasks, devices, fault_tickets）
3. Menu：40(報修維護), 41(報修工單), 42(巡查管理)
4. Permission：REPAIR_VIEW, REPAIR_MANAGE, REPAIR_DISPATCH, INSPECTION_VIEW, INSPECTION_MANAGE
5. Role binding

### 5-2 後端實作清單

| # | 類別 | 檔案 | 說明 |
|---|------|------|------|
| 1 | Entity | `InspectionTask.java` | `TenantAware`, FK → DeptInfo |
| 2 | Entity | `InspectionTaskType.java` (enum) | ONE_TIME, RECURRING |
| 3 | Entity | `InspectionTaskStatus.java` (enum) | ACTIVE, INACTIVE |
| 4 | Entity | `InspectionRecord.java` | `TenantAware`, FK → InspectionTask, Device |
| 5 | Entity | `InspectionResult.java` (enum) | NORMAL, ABNORMAL, NEED_REPAIR |
| 6 | Repository | `InspectionTaskRepository.java` | `TenantScopedRepository` |
| 7 | Repository | `InspectionRecordRepository.java` | `TenantScopedRepository` |
| 8 | DTO | `InspectionTaskRequest.java` / `InspectionTaskResponse.java` | |
| 9 | DTO | `InspectionRecordRequest.java` / `InspectionRecordResponse.java` | |
| 10 | Service | `InspectionService.java` | CRUD + 排程觸發 + 異常→自動建 fault_ticket (E13) |
| 11 | Controller | `InspectionController.java` | 8 個端點 |

### 5-3 巡查發現異常 → 自動建 fault_ticket (E13)

```java
public InspectionRecord createRecord(InspectionRecordRequest request) {
    InspectionRecord record = mapper.toEntity(request);
    InspectionRecord saved = recordRepo.save(record);

    // 巡查結果為 NEED_REPAIR → 自動建立障礙工單
    if (InspectionResult.NEED_REPAIR == request.getResult()) {
        FaultTicket fault = faultTicketService.createFromInspection(
            request.getDeviceId(), request.getNotes(), saved.getId());
        saved.setFaultTicketId(fault.getId());
        recordRepo.save(saved);
    }

    return saved;
}
```

### 5-4 API 端點

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/v1/inspection/tasks` | INSPECTION_VIEW | 巡查任務列表 |
| GET | `/v1/inspection/tasks/{id}` | INSPECTION_VIEW | 任務詳情 |
| POST | `/v1/inspection/tasks` | INSPECTION_MANAGE | 新增巡查任務 |
| PUT | `/v1/inspection/tasks/{id}` | INSPECTION_MANAGE | 編輯巡查任務 |
| DELETE | `/v1/inspection/tasks/{id}` | INSPECTION_MANAGE | 停用巡查任務 |
| GET | `/v1/inspection/tasks/{id}/records` | INSPECTION_VIEW | 任務下的巡查紀錄 |
| POST | `/v1/inspection/records` | INSPECTION_MANAGE | 新增巡查紀錄（含結果+照片） |
| GET | `/v1/inspection/records/{id}` | INSPECTION_VIEW | 巡查紀錄詳情 |

### 5-5 前端

| # | 檔案 | 說明 |
|---|------|------|
| 1 | `src/types/repair.ts` | RepairTicket, RepairDispatch, AttachmentPhase, InspectionTask 等型別 |
| 2 | `src/api/repair/index.ts` | 報修工單 + 派工 + 附件 API |
| 3 | `src/api/inspection/index.ts` | 巡查任務 + 紀錄 API |
| 4 | `src/stores/repairStore.ts` | 報修統計快取（待收案、處理中、待審核數量） |
| 5 | `src/views/admin/repair/RepairTicketView.vue` | 報修工單列表+篩選+分頁 |
| 6 | `src/views/admin/repair/RepairTicketDetailView.vue` | 工單詳情：含 WorkflowStepper + 派工歷史 + 附件 + 操作按鈕 |
| 7 | `src/views/admin/repair/RepairDispatchDialog.vue` | 派工 Dialog（選擇廠商/人員、設定期限） |
| 8 | `src/views/admin/repair/CompletionReportDialog.vue` | 完工回報 Dialog（上傳照片+GPS+描述） |
| 9 | `src/views/admin/repair/InspectionView.vue` | 巡查任務列表 |
| 10 | `src/views/admin/repair/InspectionRecordView.vue` | 巡查紀錄填寫 |
| 11 | `src/components/AttachmentUploader.vue` | **共用元件**：多圖上傳 + GPS 讀取 + phase 標記（維修前/中/後） |
| 12 | `src/components/AttachmentGallery.vue` | **共用元件**：照片畫廊 + GPS 地圖標記 |
| 13 | i18n | zh-TW / en / zh-CN 新增 repair + inspection 相關 key |
| 14 | router | 新增 /admin/repair/* 路由 |

#### 報修工單詳情頁佈局

```
┌──────────────────────────────────────────────────┐
│  工單號：RT-20260422-001  狀態：已派工  優先：一般  │
├──────────────────────────────────────────────────┤
│                                                   │
│  📋 WorkflowStepper（流程歷程）                    │
│  [未收案] → [已收案] → [已派工✓] → [處理中] → ... │
│                                                   │
├──────────────────────────────────────────────────┤
│  Tab: 基本資料 │ 派工記錄 │ 附件 │ 流程歷程        │
│                                                   │
│  基本資料 Tab:                                     │
│    報修來源：障礙轉立                              │
│    設備：路燈-001（LED路燈，忠孝東路）              │
│    回路：C-001                                     │
│    報修人：王先生 0912-345-678                      │
│    描述：路燈不亮...                                │
│                                                   │
│  派工記錄 Tab:                                     │
│    [第1次派工] 2026-04-22 → 光輝維護公司 → 張三    │
│    備註：回路維修                                   │
│                                                   │
│  附件 Tab: AttachmentGallery（維修前/中/後分類）    │
│                                                   │
│  流程歷程 Tab: WorkflowStepLog list                │
│                                                   │
├──────────────────────────────────────────────────┤
│  WorkflowActionBar：[收案] [派工] [完工回報]       │
│  （根據 currentStep + 使用者角色 動態顯示）        │
└──────────────────────────────────────────────────┘
```

### 5-6 單元測試

| # | 測試類 | 涵蓋範圍 |
|---|--------|----------|
| 1 | `RepairTicketControllerTest` | 所有端點 + 權限驗證 |
| 2 | `RepairTicketServiceTest` | 雙路徑建單 + 狀態流轉 + DataScope |
| 3 | `RepairDispatchServiceTest` | 派工 + 退回重派 + dispatched_by 驗證 |
| 4 | `TicketAttachmentServiceTest` | 上傳 + 檔案類型驗證 + scan_status |
| 5 | `FaultApprovedListenerTest` | E1 事件：障礙審核通過→自動建單 |
| 6 | `RepairClosedListenerTest` | E9 事件：結案→資產同步 |
| 7 | `InspectionServiceTest` | CRUD + E13 異常→自動建 fault_ticket |
| 8 | `InspectionControllerTest` | 端點 + 權限 |

---

## Phase 2 完成標準

- [ ] V33 migration 執行成功，repair_tickets / repair_dispatches / ticket_attachments 表建立
- [ ] V34 migration 執行成功，inspection 表 + 選單權限
- [ ] 報修工單 CRUD + 分頁篩選 + DataScope 可用
- [ ] 派工流程可用（指定人員 + 設定期限 + 記錄 dispatched_by）
- [ ] 完工回報可用（照片 + GPS + 維修前/中/後分類）
- [ ] E1 事件：障礙審核通過→自動建立報修工單（驗證）
- [ ] E9 事件：結案審核通過→設備狀態更新 + device_events 歷程（驗證）
- [ ] 巡查任務 CRUD + 巡查紀錄填寫可用
- [ ] E13 事件：巡查異常→自動建立 fault_ticket（驗證）
- [ ] 前端報修工單頁面（含詳情 + WorkflowStepper + 操作按鈕）可用
- [ ] 前端巡查管理頁面可用
- [ ] 共用元件 AttachmentUploader + AttachmentGallery 可用
- [ ] 所有單元測試通過
- [ ] `mvn test` 全過
