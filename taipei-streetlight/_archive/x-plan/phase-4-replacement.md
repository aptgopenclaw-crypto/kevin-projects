# Phase 4：換裝維護 — 06 換裝管理

> Flyway V45 ~ V46 ｜涵蓋模組：06 換裝維護
> 前置條件：Phase 3 完成（material_specs + inventory + approved_materials + issue 流程）
> 後續依賴：Phase 5（整合閉環）補齊跨模組 FK + 完整生命週期測試
>
> **注意事項**：
> - V40~V44 已被 Phase 3（材料管理）佔用，本 Phase 從 V45 起編
> - API 路徑統一使用 `/v1/auth/` 前綴
> - 換裝模型為「**換設備**」模式：舊設備除役 + 從庫存領新設備掛到燈桿下
> - 直接呼叫 `DeviceService.replaceComponent()` 已有方法，不需新增 provisionalUpdate
> - WorkflowService 用 `transition()` 方法（7 參數），不是 `submit()`
> - WorkflowTransitionEvent 透過 `event.getInstance().getWorkflowType()` 取值
> - `TenantContext.getCurrentTenantId()`（不是 `getTenantId()`）
> - Controller/Service 模式同 Phase 2/3：手動 Builder，BaseResponse 包裝，PageResponse 分頁
> - Entity 註解模式：`@Filter` + `@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})`（不用 `@FilterDef`）
> - `SystemSettingService` 需新增通用 `getSetting(key)` 方法 + `SettingKey` 新增 `FRONTEND_BASE_URL`
> - `IssueService` 需新增 `createFromReplacement(orderId)` 方法
> - ErrorCode 格式為 5 位字串：`REPLACEMENT_ORDER_NOT_FOUND("80001", 404, "換裝派工單不存在")`
>
> **設計決策紀錄**（2026-04-23 討論確認）：
> - 所有狀態轉換端點放在 `ReplacementOrderController`（含 dispatch, startWork, selfCheck, submitReview, approve, return, resubmit）
> - 自主檢核時新設備 `deviceCode` 由承商掃描實體標籤提供（`SelfCheckItemRequest.deviceCode`）
> - `createFromReplacement()` 直接在 `IssueService` 新增，接收 `orderId` 建立 `IssueRequest`

---

## 總覽

| Step | Flyway | 範圍 | 產出 |
|------|--------|------|------|
| 8 | V45 | 06 replacement_orders + replacement_items + light_pole_numbers | 3 張表 + indexes |
| 9 | V46 | 06 menu + permission + role binding + 跨模組 FK 補齊 | 選單 + 權限 + FK |

---

## 完整業務流程圖（端到端）

### 跨模組全鏈路

```
巡檢員發現路燈故障
    │
    ▼
建立障礙工單 (FAULT_REVIEW)                    ← 已有模組
    │ 審核確認
    ▼
E1：自動建立報修工單 (REPAIR_DISPATCH)           ← 已有模組
    │ 收案 → 派工 → 承商到場檢查
    ▼
承商判定「燈具需換裝」
    │ 在報修詳情頁點「需換裝」(路徑 A)
    ▼
═══════════════════════════════════════════════ Phase 4 範圍 ↓

建立換裝派工單 (REPLACEMENT_REVIEW)
    │ 自動帶入：哪根燈桿、哪個燈具故障、關聯報修單
    ▼
管理員指定承商 + 派工 (DRAFT → DISPATCHED)
    │ E6：自動建立領料申請 (issue_request)
    ▼
承商開工 (DISPATCHED → IN_PROGRESS)
    │ 從庫存領料、前往現場
    ▼
承商自主檢核 (IN_PROGRESS → SELF_CHECKED)
    │ 掃描新設備標籤取得 deviceCode
    │ 呼叫 replaceComponent()：舊設備除役 + 新設備掛上燈桿
    │ 回寫 new_device_id 到 replacement_items
    ▼
報竣送審 (SELF_CHECKED → PENDING_REVIEW)
    │
    ▼
管理員審核
    ├── 通過 → CLOSED (E10 結案)
    │         │ 設備已在 replaceComponent 時更新完成
    │         │ 寫入 device_events
    │         └ 確認扣庫（Phase 5）
    │
    └── 退回 → RETURNED → 補件後 resubmit → PENDING_REVIEW
```

### 換裝派工單狀態機（REPLACEMENT_REVIEW FSM）

```
                  ┌──────────────┐
                  │    DRAFT     │ 草稿（建單）
                  └──────┬───────┘
                         │ dispatch（派工）
                  ┌──────▼───────┐
                  │  DISPATCHED  │ 已派工（E6 觸發領料）
                  └──────┬───────┘
                         │ startWork（開工）
                  ┌──────▼───────┐
                  │  IN_PROGRESS │ 施工中（領料+現場作業）
                  └──────┬───────┘
                         │ selfCheck（自主檢核，執行 replaceComponent）
                  ┌──────▼───────┐
                  │ SELF_CHECKED │ 自主檢核完成
                  └──────┬───────┘
                         │ submitReview（報竣送審）
                  ┌──────▼───────┐
                  │PENDING_REVIEW│ 報竣審核中
                  └──────┬───────┘
                    ┌────┴────┐
              ┌─────▼───┐ ┌──▼──────┐
              │ CLOSED   │ │RETURNED │
              │(E10結案) │ └────┬────┘
              └──────────┘      │ resubmit（補件重送）
                                └──────→ PENDING_REVIEW
```

### 各轉換對應的 API 端點與業務邏輯

| 轉換 | API 端點 | 業務邏輯 |
|------|----------|----------|
| → DRAFT | `POST /orders` 或 `POST /orders/from-repair/{id}` | 建單 + createInstance |
| DRAFT → DISPATCHED | `POST /orders/{id}/dispatch` | 指定承商 + 觸發 E6 自動領料 |
| DISPATCHED → IN_PROGRESS | `POST /orders/{id}/start-work` | 記錄開工時間 |
| IN_PROGRESS → SELF_CHECKED | `POST /orders/{id}/self-check` | **核心**：replaceComponent + 回寫 newDeviceId |
| SELF_CHECKED → PENDING_REVIEW | `POST /orders/{id}/submit-review` | 送審 |
| PENDING_REVIEW → CLOSED | `POST /orders/{id}/approve` | 審核通過 → 觸發 E10 結案 Listener |
| PENDING_REVIEW → RETURNED | `POST /orders/{id}/return` | 退回 + 附退回理由 |
| RETURNED → PENDING_REVIEW | `POST /orders/{id}/resubmit` | 補件後重新送審 |

### 進入換裝的兩條路徑

| 路徑 | 來源 | 說明 |
|------|------|------|
| **路徑 A** | 報修觸發 | 巡檢員/承商在報修詳情頁點「需換裝」→ 自動帶入 repairTicketId、燈桿、故障設備 |
| **路徑 B** | 獨立開立 | 新設、遷移、拆除等不經報修的換裝需求 |

### 設備換裝模型（核心概念）

```
換裝前：
  燈桿(POLE, id=1)
    └── 燈具(LUMINAIRE, id=100) ← 故障

換裝後：
  燈桿(POLE, id=1)
    └── 燈具(LUMINAIRE, id=200) ← 從庫存領出的新設備
  
  舊燈具(id=100) → DECOMMISSIONED
  新燈具(id=200) → ACTIVE, parentDeviceId=1

replacement_items 記錄：
  parent_device_id = 1    (燈桿位置不變)
  old_device_id    = 100  (被替換的舊設備)
  new_device_id    = 200  (領出的新設備，完工時填入)
  before_spec      = {舊設備 attributes snapshot}
  after_spec       = {新設備 attributes snapshot}
  material_spec_id = FK → material_specs (材料規格)
  approved_material_id = FK → approved_materials (合格材料)
```

---

## Step 8：V45 — 換裝派工單 + 明細 + 號碼牌

### 8-1 Flyway Migration

**檔案**：`V45__replacement__create_tables.sql`

**建表順序**：
1. `replacement_orders`（FK → repair_tickets, contracts, dept_info）
2. `replacement_items`（FK → replacement_orders, devices×2, material_specs, approved_materials）
3. `light_pole_numbers`（FK → devices）

**DDL**（基於 cross-module，調整為換設備模型）：

```sql
-- 1. 換裝派工單
CREATE TABLE replacement_orders (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    order_number        VARCHAR(50)     NOT NULL,
    repair_ticket_id    BIGINT          REFERENCES repair_tickets(id),
    contract_id         BIGINT          REFERENCES contracts(id),
    order_type          VARCHAR(30)     NOT NULL,
    dispatch_reason     TEXT,
    location            TEXT,
    expected_quantity   INT,
    work_period_start   DATE,
    work_period_end     DATE,
    assigned_contractor VARCHAR(200),
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    dept_id             BIGINT          REFERENCES dept_info(dept_id),
    created_by          VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, order_number)
);

-- 2. 換裝明細（換設備模型）
CREATE TABLE replacement_items (
    id                      BIGSERIAL       PRIMARY KEY,
    tenant_id               VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    order_id                BIGINT          NOT NULL REFERENCES replacement_orders(id),
    parent_device_id        BIGINT          NOT NULL REFERENCES devices(id),
    old_device_id           BIGINT          NOT NULL REFERENCES devices(id),
    new_device_id           BIGINT          REFERENCES devices(id),
    before_device_type      VARCHAR(30),
    before_spec             JSONB           DEFAULT '{}',
    after_device_type       VARCHAR(30),
    after_spec              JSONB           DEFAULT '{}',
    material_spec_id        BIGINT          REFERENCES material_specs(id),
    approved_material_id    BIGINT          REFERENCES approved_materials(id),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    completed_at            TIMESTAMP,
    completed_by            VARCHAR(50),
    notes                   TEXT,
    created_at              TIMESTAMP       NOT NULL DEFAULT now()
);

-- 3. 號碼牌
CREATE TABLE light_pole_numbers (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    pole_number     VARCHAR(100)    NOT NULL,
    device_id       BIGINT          REFERENCES devices(id),
    qr_code_url     VARCHAR(500),
    issued_at       DATE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, pole_number)
);

-- Indexes
CREATE INDEX idx_replacement_orders_tenant_status ON replacement_orders(tenant_id, status);
CREATE INDEX idx_replacement_orders_repair_ticket ON replacement_orders(repair_ticket_id);
CREATE INDEX idx_replacement_items_order ON replacement_items(order_id);
CREATE INDEX idx_replacement_items_old_device ON replacement_items(old_device_id);
CREATE INDEX idx_replacement_items_new_device ON replacement_items(new_device_id);
CREATE INDEX idx_light_pole_numbers_device ON light_pole_numbers(device_id);

-- issue_requests 補 FK（Phase 3 建表時 replacement_order_id 無 FK）
ALTER TABLE issue_requests
    ADD CONSTRAINT fk_issue_requests_replacement_order
    FOREIGN KEY (replacement_order_id) REFERENCES replacement_orders(id);
```

### 8-2 後端實作清單

| # | 類別 | 檔案 | 說明 |
|---|------|------|------|
| **Entities** | | | |
| 1 | Entity | `ReplacementOrder.java` | `TenantAware`, 含所有 DDL 欄位（見下方） |
| 2 | Entity | `ReplacementOrderType.java` (enum) | NEW_INSTALL, REPLACEMENT, RELOCATION, DECOMMISSION, ADJUSTMENT, SHADE_INSTALL |
| 3 | Entity | `ReplacementOrderStatus.java` (enum) | DRAFT, DISPATCHED, IN_PROGRESS, SELF_CHECKED, PENDING_REVIEW, RETURNED, CLOSED |
| 4 | Entity | `ReplacementItem.java` | `TenantAware`, 換設備模型：parentDeviceId + oldDeviceId + newDeviceId |
| 5 | Entity | `ReplacementItemStatus.java` (enum) | PENDING, IN_PROGRESS, COMPLETED, SKIPPED |
| 6 | Entity | `LightPoleNumber.java` | `TenantAware`, FK → Device |
| 7 | Entity | `PoleNumberStatus.java` (enum) | ACTIVE, DECOMMISSIONED, LOST |
| **Repositories** | | | |
| 8 | Repository | `ReplacementOrderRepository.java` | `TenantScopedRepository`, 分頁+篩選+DataScope |
| 9 | Repository | `ReplacementItemRepository.java` | `TenantScopedRepository` |
| 10 | Repository | `LightPoleNumberRepository.java` | `TenantScopedRepository` |
| **DTOs** | | | |
| 11 | DTO | `ReplacementOrderRequest.java` | orderType, repairTicketId, contractId, dispatchReason, location, workPeriodStart/End, assignedContractor, deptId |
| 12 | DTO | `ReplacementOrderResponse.java` | 含 items list, repairTicketSummary, contractName, currentStep |
| 13 | DTO | `ReplacementOrderQueryParams.java` | status, orderType, contractId, keyword, dateRange |
| 14 | DTO | `ReplacementItemRequest.java` | parentDeviceId, oldDeviceId, afterDeviceType, afterSpec, materialSpecId, approvedMaterialId |
| 15 | DTO | `ReplacementItemResponse.java` | 含 parentDeviceCode, oldDeviceCode, newDeviceCode, beforeSpec, afterSpec |
| 16 | DTO | `SelfCheckRequest.java` | items[]{itemId, deviceCode, newDeviceId(optional), notes} |
| 17 | DTO | `PoleNumberRequest.java` / `PoleNumberResponse.java` | |
| **Services** | | | |
| 18 | Service | `ReplacementOrderService.java` | 見 §8-3 核心業務邏輯 |
| 19 | Service | `ReplacementItemService.java` | 明細 CRUD + 材料管控 |
| 20 | Service | `LightPoleNumberService.java` | 號碼牌產出/重製/QR Code 連結 |
| **Event Listeners** | | | |
| 21 | Listener | `ReplacementClosedListener.java` | 監聽 E10：換裝結案 → 確認 device_events 已記錄 |
| 22 | Listener | `ReplacementNeedMaterialListener.java` | 監聽換裝派工 → 自動建立 issue_request（E6） |
| **Controllers** | | | |
| 23 | Controller | `ReplacementOrderController.java` | 見 §8-4 API 端點 |
| 24 | Controller | `LightPoleNumberController.java` | 號碼牌管理 |

### 8-3 核心業務邏輯

#### ReplacementOrder Entity（完整欄位）

```java
@Entity
@Table(name = "replacement_orders")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementOrder implements TenantAware {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "repair_ticket_id")
    private Long repairTicketId;

    @Column(name = "contract_id")
    private Long contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 30)
    private ReplacementOrderType orderType;

    @Column(name = "dispatch_reason")
    private String dispatchReason;

    @Column(name = "location")
    private String location;

    @Column(name = "expected_quantity")
    private Integer expectedQuantity;

    @Column(name = "work_period_start")
    private LocalDate workPeriodStart;

    @Column(name = "work_period_end")
    private LocalDate workPeriodEnd;

    @Column(name = "assigned_contractor", length = 200)
    private String assignedContractor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReplacementOrderStatus status;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### ReplacementItem Entity（換設備模型）

```java
@Entity
@Table(name = "replacement_items")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementItem implements TenantAware {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "parent_device_id", nullable = false)
    private Long parentDeviceId;    // 燈桿（位置不變）

    @Column(name = "old_device_id", nullable = false)
    private Long oldDeviceId;       // 被替換的舊設備

    @Column(name = "new_device_id")
    private Long newDeviceId;       // 替換上去的新設備（完工時填入）

    @Column(name = "before_device_type", length = 30)
    private String beforeDeviceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_spec", columnDefinition = "jsonb")
    private Map<String, Object> beforeSpec;

    @Column(name = "after_device_type", length = 30)
    private String afterDeviceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_spec", columnDefinition = "jsonb")
    private Map<String, Object> afterSpec;

    @Column(name = "material_spec_id")
    private Long materialSpecId;

    @Column(name = "approved_material_id")
    private Long approvedMaterialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReplacementItemStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 50)
    private String completedBy;

    @Column(name = "notes")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

#### 換裝派工單建立（雙路徑）

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplacementOrderService {

    private final ReplacementOrderRepository repo;
    private final ReplacementItemRepository itemRepo;
    private final RepairTicketRepository repairTicketRepo;
    private final DeviceService deviceService;
    private final DeviceEventService deviceEventService;
    private final WorkflowService workflowService;
    private final DataScopeHelper dataScopeHelper;
    private final AtomicLong dailySequence = new AtomicLong(1);
    private volatile String lastDate = "";

    /**
     * 路徑 A：從報修工單觸發（使用者在報修詳情頁點「需換裝」）
     */
    @Transactional
    @AuditEvent(AuditEventType.CREATE_REPLACEMENT_ORDER)
    public ReplacementOrder createFromRepair(Long repairTicketId, ReplacementOrderRequest request) {
        RepairTicket repair = repairTicketRepo.findById(repairTicketId)
            .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_TICKET_NOT_FOUND));

        ReplacementOrder order = ReplacementOrder.builder()
            .orderNumber(generateOrderNumber())
            .repairTicketId(repairTicketId)
            .contractId(repair.getContractId())
            .orderType(request.getOrderType())
            .dispatchReason(request.getDispatchReason())
            .location(request.getLocation())
            .expectedQuantity(request.getExpectedQuantity())
            .workPeriodStart(request.getWorkPeriodStart())
            .workPeriodEnd(request.getWorkPeriodEnd())
            .assignedContractor(request.getAssignedContractor())
            .status(ReplacementOrderStatus.DRAFT)
            .deptId(repair.getDeptId())
            .createdBy(SecurityContextUtils.getCurrentUserId())
            .build();

        ReplacementOrder saved = repo.save(order);

        workflowService.createInstance(
            "REPLACEMENT_REVIEW", "REPLACEMENT_ORDER", saved.getId(),
            SecurityContextUtils.getCurrentUserId());

        return saved;
    }

    /**
     * 路徑 B：獨立開立（新設、遷移等不經報修的換裝）
     */
    @Transactional
    @AuditEvent(AuditEventType.CREATE_REPLACEMENT_ORDER)
    public ReplacementOrder createDirect(ReplacementOrderRequest request) {
        ReplacementOrder order = ReplacementOrder.builder()
            .orderNumber(generateOrderNumber())
            .orderType(request.getOrderType())
            .contractId(request.getContractId())
            .dispatchReason(request.getDispatchReason())
            .location(request.getLocation())
            .expectedQuantity(request.getExpectedQuantity())
            .workPeriodStart(request.getWorkPeriodStart())
            .workPeriodEnd(request.getWorkPeriodEnd())
            .assignedContractor(request.getAssignedContractor())
            .status(ReplacementOrderStatus.DRAFT)
            .deptId(request.getDeptId())
            .createdBy(SecurityContextUtils.getCurrentUserId())
            .build();

        ReplacementOrder saved = repo.save(order);

        workflowService.createInstance(
            "REPLACEMENT_REVIEW", "REPLACEMENT_ORDER", saved.getId(),
            SecurityContextUtils.getCurrentUserId());

        return saved;
    }

    private synchronized String generateOrderNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(1);
        }
        return String.format("RO-%s-%03d", today, dailySequence.getAndIncrement());
    }

    // ─── 狀態轉換方法 ───

    /** 派工：DRAFT → DISPATCHED（觸發 E6 自動領料由 Listener 處理） */
    @Transactional
    @AuditEvent(AuditEventType.UPDATE_REPLACEMENT_ORDER)
    public void dispatch(Long orderId, ReplacementOrderRequest request) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.DRAFT);

        order.setAssignedContractor(request.getAssignedContractor());
        order.setWorkPeriodStart(request.getWorkPeriodStart());
        order.setWorkPeriodEnd(request.getWorkPeriodEnd());
        order.setStatus(ReplacementOrderStatus.DISPATCHED);
        repo.save(order);

        transitionWorkflow(orderId, "DISPATCHED", "DISPATCH", "派工至承商");
    }

    /** 開工：DISPATCHED → IN_PROGRESS */
    @Transactional
    public void startWork(Long orderId) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.DISPATCHED);

        order.setStatus(ReplacementOrderStatus.IN_PROGRESS);
        repo.save(order);

        transitionWorkflow(orderId, "IN_PROGRESS", "START_WORK", "承商開工");
    }

    /** 報竣送審：SELF_CHECKED → PENDING_REVIEW */
    @Transactional
    public void submitReview(Long orderId) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.SELF_CHECKED);

        order.setStatus(ReplacementOrderStatus.PENDING_REVIEW);
        repo.save(order);

        transitionWorkflow(orderId, "PENDING_REVIEW", "SUBMIT_REVIEW", "報竣送審");
    }

    /** 審核通過：PENDING_REVIEW → CLOSED（觸發 E10 結案由 Listener 處理） */
    @Transactional
    @AuditEvent(AuditEventType.CLOSE_REPLACEMENT)
    public void approve(Long orderId, String comment) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.PENDING_REVIEW);
        // 狀態由 ReplacementClosedListener 統一設為 CLOSED
        transitionWorkflow(orderId, "CLOSED", "APPROVE", comment);
    }

    /** 退回：PENDING_REVIEW → RETURNED */
    @Transactional
    public void returnOrder(Long orderId, String comment) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.PENDING_REVIEW);

        order.setStatus(ReplacementOrderStatus.RETURNED);
        repo.save(order);

        transitionWorkflow(orderId, "RETURNED", "RETURN", comment);
    }

    /** 補件重送：RETURNED → PENDING_REVIEW */
    @Transactional
    public void resubmit(Long orderId) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.RETURNED);

        order.setStatus(ReplacementOrderStatus.PENDING_REVIEW);
        repo.save(order);

        transitionWorkflow(orderId, "PENDING_REVIEW", "RESUBMIT", "補件重送");
    }

    // ─── 私有輔助方法 ───

    private ReplacementOrder findOrThrow(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.REPLACEMENT_ORDER_NOT_FOUND));
    }

    private void validateStatus(ReplacementOrder order, ReplacementOrderStatus... expected) {
        if (!Set.of(expected).contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.REPLACEMENT_INVALID_STATUS);
        }
    }

    private void transitionWorkflow(Long orderId, String targetStep, String action, String comment) {
        WorkflowInstance instance = workflowService.findByTicket("REPLACEMENT_ORDER", orderId);
        workflowService.transition(
            instance.getId(),
            targetStep,
            action,
            SecurityContextUtils.getCurrentUserId(),
            SecurityContextUtils.getCurrentUsername(),
            comment,
            null);
    }
}
```

#### 新增換裝明細 + 材料管控

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplacementItemService {

    private final ReplacementItemRepository itemRepo;
    private final ReplacementOrderRepository orderRepo;
    private final DeviceRepository deviceRepo;
    private final ApprovedMaterialRepository approvedMaterialRepo;

    /**
     * 新增換裝明細
     * 記錄：哪個燈桿 + 哪個舊設備要被換 + 預計用什麼材料
     */
    @Transactional
    public ReplacementItem addItem(Long orderId, ReplacementItemRequest request) {
        ReplacementOrder order = orderRepo.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.REPLACEMENT_ORDER_NOT_FOUND));
        validateStatus(order, ReplacementOrderStatus.DRAFT,
                             ReplacementOrderStatus.DISPATCHED,
                             ReplacementOrderStatus.IN_PROGRESS);

        // 材料管控：approved_material_id 必須存在且狀態為 ACTIVE
        if (request.getApprovedMaterialId() != null) {
            ApprovedMaterial am = approvedMaterialRepo.findById(request.getApprovedMaterialId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_NOT_APPROVED));
            if (am.getStatus() != ApprovedMaterialStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.MATERIAL_NOT_AVAILABLE);
            }
        }

        // 驗證舊設備屬於該燈桿
        Device oldDevice = deviceRepo.findById(request.getOldDeviceId())
            .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (!request.getParentDeviceId().equals(oldDevice.getParentDeviceId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "舊設備不屬於指定燈桿");
        }

        // 記錄換裝前規格（snapshot）
        ReplacementItem item = ReplacementItem.builder()
            .orderId(orderId)
            .parentDeviceId(request.getParentDeviceId())
            .oldDeviceId(request.getOldDeviceId())
            .newDeviceId(null)  // 完工時才填入
            .beforeDeviceType(oldDevice.getDeviceType().name())
            .beforeSpec(oldDevice.getAttributes())  // JSONB snapshot
            .afterDeviceType(request.getAfterDeviceType())
            .afterSpec(request.getAfterSpec())
            .materialSpecId(request.getMaterialSpecId())
            .approvedMaterialId(request.getApprovedMaterialId())
            .status(ReplacementItemStatus.PENDING)
            .build();

        return itemRepo.save(item);
    }
}
```

#### 自主檢核（06-9：執行實際換裝）

```java
/**
 * SelfCheckRequest DTO
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SelfCheckRequest {
    @NotEmpty
    private List<SelfCheckItemRequest> items;
}

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SelfCheckItemRequest {
    @NotNull
    private Long itemId;
    @NotBlank(message = "新設備代碼為必填（掃描實體標籤）")
    private String deviceCode;    // 新設備代碼（承商掃描實體標籤取得）
    private Long newDeviceId;     // 可選：如果庫存已有對應 device 則帶入
    private String notes;
}
```

```java
/**
 * 承商自主檢核：逐項執行設備替換
 * 每個 item 呼叫 DeviceService.replaceComponent() 完成舊→新設備替換
 * 替換後將 new_device_id 寫回 replacement_items
 *
 * ⚠ selfCheck 放在 ReplacementOrderService 中（需注入 DeviceService + DeviceEventService）
 */
@Transactional
@AuditEvent(AuditEventType.SELF_CHECK_REPLACEMENT)
public void selfCheck(Long orderId, SelfCheckRequest request) {
    ReplacementOrder order = findOrThrow(orderId);
    validateStatus(order, ReplacementOrderStatus.IN_PROGRESS);

    for (var itemCheck : request.getItems()) {
        ReplacementItem item = itemRepo.findById(itemCheck.getItemId())
            .orElseThrow(() -> new BusinessException(ErrorCode.REPLACEMENT_ORDER_NOT_FOUND));

        // 呼叫既有的 replaceComponent：除役舊設備 + 建立新設備 + 記錄 events
        ComponentReplaceRequest replaceReq = ComponentReplaceRequest.builder()
            .oldDeviceId(item.getOldDeviceId())
            .newDevice(buildNewDeviceRequest(item, itemCheck))
            .reason("換裝派工單 " + order.getOrderNumber())
            .build();

        DeviceResponse newDevice = deviceService.replaceComponent(
            item.getParentDeviceId(), replaceReq, deviceEventService);

        // 回寫新設備 ID + 完工資訊
        item.setNewDeviceId(newDevice.getId());
        item.setAfterSpec(newDevice.getAttributes());
        item.setStatus(ReplacementItemStatus.COMPLETED);
        item.setCompletedAt(LocalDateTime.now());
        item.setCompletedBy(SecurityContextUtils.getCurrentUsername());
        item.setNotes(itemCheck.getNotes());
        itemRepo.save(item);
    }

    order.setStatus(ReplacementOrderStatus.SELF_CHECKED);
    repo.save(order);

    transitionWorkflow(orderId, "SELF_CHECKED", "SELF_CHECK", "廠商自主檢核完成");
}

/**
 * 建構新設備 DeviceRequest
 * 從 replacement_item 的規格資訊 + 承商提供的 deviceCode 組裝
 */
private DeviceRequest buildNewDeviceRequest(ReplacementItem item, SelfCheckItemRequest itemCheck) {
    return DeviceRequest.builder()
        .deviceType(DeviceType.valueOf(
            item.getAfterDeviceType() != null ? item.getAfterDeviceType() : item.getBeforeDeviceType()))
        .deviceCode(itemCheck.getDeviceCode())   // 承商掃描實體標籤提供
        .parentDeviceId(item.getParentDeviceId()) // 掛回同一根燈桿
        .attributes(item.getAfterSpec())          // 換裝後的規格
        .build();
}
```

#### E10：換裝結案 Listener

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplacementClosedListener {

    private final ReplacementOrderRepository orderRepo;
    private final ReplacementItemRepository itemRepo;

    /**
     * E10：換裝結案
     * 設備替換已在自主檢核(selfCheck)時由 replaceComponent() 完成。
     * 此 listener 負責：
     * 1. 更新 order 狀態
     * 2. 預留扣庫確認 hook（Phase 5 啟用）
     */
    @EventListener
    @Transactional
    public void onReplacementClosed(WorkflowTransitionEvent event) {
        if (!"REPLACEMENT_REVIEW".equals(event.getInstance().getWorkflowType())) return;
        if (!"CLOSED".equals(event.getTargetStep())) return;

        Long orderId = event.getInstance().getTicketId();
        log.info("E10 事件：換裝派工單 {} 結案", orderId);

        ReplacementOrder order = orderRepo.findById(orderId).orElseThrow();
        order.setStatus(ReplacementOrderStatus.CLOSED);
        orderRepo.save(order);

        // 確認材料扣庫（Phase 5 整合時啟用）
        // issueService.confirmDeduction(order.getId());
    }
}
```

#### E6：換裝派工 → 自動建立領料申請

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplacementNeedMaterialListener {

    private final IssueService issueService;

    /**
     * E6：換裝派工單 DISPATCHED → 自動建立 issue_request
     * 讓承商可以從庫存領取所需材料
     */
    @EventListener
    @Transactional
    public void onReplacementDispatched(WorkflowTransitionEvent event) {
        if (!"REPLACEMENT_REVIEW".equals(event.getInstance().getWorkflowType())) return;
        if (!"DISPATCHED".equals(event.getTargetStep())) return;

        Long orderId = event.getInstance().getTicketId();
        log.info("E6 事件：換裝派工單 {} 已派工，自動建立領料申請", orderId);

        issueService.createFromReplacement(orderId);
    }
}
```

#### IssueService 新增方法（跨模組擴充）

> 在既有的 `material/service/IssueService.java` 中新增，不另開 Service

```java
/**
 * 從換裝派工單自動建立領料申請
 * 由 ReplacementNeedMaterialListener (E6) 呼叫
 */
@Transactional
public IssueRequest createFromReplacement(Long replacementOrderId) {
    IssueRequest request = IssueRequest.builder()
        .requestNumber(generateRequestNumber())
        .replacementOrderId(replacementOrderId)
        .requestedBy(SecurityContextUtils.getCurrentUserId())
        .status(IssueRequestStatus.PENDING)
        .build();
    return issueRequestRepository.save(request);
}
```

#### SystemSettingService 新增方法 + SettingKey 擴充

> 在既有的 `setting/service/SystemSettingService.java` 中新增通用取值方法

```java
// SystemSettingService.java 新增：
@Transactional(readOnly = true)
public String getSetting(String key) {
    return settingRepository.findBySettingKey(key)
        .map(SystemSettingEntity::getSettingValue)
        .orElseThrow(() -> new IllegalStateException("Setting not found: " + key));
}
```

```java
// SettingKey.java 新增：
FRONTEND_BASE_URL("frontend_base_url", "http://localhost:5173");
```

> V46 migration 中需 seed `FRONTEND_BASE_URL` 設定值

### 8-4 API 端點

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| **換裝派工單 CRUD** | | | |
| GET | `/v1/auth/replacement/orders` | REPLACEMENT_VIEW | 分頁列表+篩選+DataScope |
| GET | `/v1/auth/replacement/orders/{id}` | REPLACEMENT_VIEW | 單筆詳情（含明細+流程歷程） |
| POST | `/v1/auth/replacement/orders` | REPLACEMENT_MANAGE | 開立換裝派工單（路徑 B 獨立開立） |
| POST | `/v1/auth/replacement/orders/from-repair/{repairTicketId}` | REPLACEMENT_MANAGE | 從報修觸發開立（路徑 A） |
| PUT | `/v1/auth/replacement/orders/{id}` | REPLACEMENT_MANAGE | 編輯派工單（僅 DRAFT 狀態） |
| **狀態轉換** | | | |
| POST | `/v1/auth/replacement/orders/{id}/dispatch` | REPLACEMENT_MANAGE | 派工：DRAFT → DISPATCHED（觸發 E6 領料） |
| POST | `/v1/auth/replacement/orders/{id}/start-work` | REPLACEMENT_MANAGE | 開工：DISPATCHED → IN_PROGRESS |
| POST | `/v1/auth/replacement/orders/{id}/self-check` | REPLACEMENT_MANAGE | 自主檢核：IN_PROGRESS → SELF_CHECKED（執行 replaceComponent） |
| POST | `/v1/auth/replacement/orders/{id}/submit-review` | REPLACEMENT_MANAGE | 報竣送審：SELF_CHECKED → PENDING_REVIEW |
| POST | `/v1/auth/replacement/orders/{id}/approve` | REPLACEMENT_MANAGE | 審核通過：PENDING_REVIEW → CLOSED（觸發 E10 結案） |
| POST | `/v1/auth/replacement/orders/{id}/return` | REPLACEMENT_MANAGE | 退回：PENDING_REVIEW → RETURNED |
| POST | `/v1/auth/replacement/orders/{id}/resubmit` | REPLACEMENT_MANAGE | 補件重送：RETURNED → PENDING_REVIEW |
| **換裝明細** | | | |
| GET | `/v1/auth/replacement/orders/{id}/items` | REPLACEMENT_VIEW | 換裝明細列表 |
| POST | `/v1/auth/replacement/orders/{id}/items` | REPLACEMENT_MANAGE | 新增換裝明細 |
| PUT | `/v1/auth/replacement/items/{id}` | REPLACEMENT_MANAGE | 編輯換裝明細 |
| DELETE | `/v1/auth/replacement/items/{id}` | REPLACEMENT_MANAGE | 刪除換裝明細 |

### 8-5 號碼牌管理

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LightPoleNumberService {

    private final LightPoleNumberRepository poleNumberRepo;
    private final SystemSettingService systemSettingService;

    /**
     * 產出號碼牌（含 QR Code URL）
     * QR Code 掃描後導向：前端報修頁面 + 帶入設備 ID
     */
    @Transactional
    public LightPoleNumber generate(PoleNumberRequest request) {
        if (poleNumberRepo.existsByTenantIdAndPoleNumber(
                TenantContext.getCurrentTenantId(), request.getPoleNumber())) {
            throw new BusinessException(ErrorCode.POLE_NUMBER_DUPLICATE);
        }

        String qrCodeUrl = buildQrCodeUrl(request.getPoleNumber(), request.getDeviceId());

        return poleNumberRepo.save(LightPoleNumber.builder()
            .poleNumber(request.getPoleNumber())
            .deviceId(request.getDeviceId())
            .qrCodeUrl(qrCodeUrl)
            .issuedAt(LocalDate.now())
            .status(PoleNumberStatus.ACTIVE)
            .build());
    }

    /**
     * QR Code URL 格式：{前端 base URL}/report?device={deviceId}&pole={poleNumber}
     * ⚠ 需在 SystemSettingService 新增 getSetting(key) 方法
     */
    private String buildQrCodeUrl(String poleNumber, Long deviceId) {
        String baseUrl = systemSettingService.getSetting("FRONTEND_BASE_URL");
        return baseUrl + "/report?device=" + deviceId + "&pole=" + poleNumber;
    }
}
```

---

## Step 9：V46 — 選單權限 + 角色綁定

### 9-1 Flyway Migration

**檔案**：`V46__replacement__menus_permissions.sql`

**內容**：
1. Menu（使用 sequence auto-generate，不硬編 ID）：
   - 換裝維護 (DIRECTORY)
   - 換裝派工 (PAGE) — `permission_code = REPLACEMENT_VIEW`
   - 號碼牌管理 (PAGE) — `permission_code = POLE_NUMBER_MANAGE`
2. Permission：REPLACEMENT_VIEW, REPLACEMENT_MANAGE, POLE_NUMBER_MANAGE
3. Role binding

```sql
DO $$
DECLARE
    v_dir_id BIGINT;
    v_page1_id BIGINT;
    v_page2_id BIGINT;
BEGIN
    -- DIRECTORY
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, icon, sort_order, visible)
    VALUES (NULL, '換裝維護', 'DIRECTORY', 'ReplacementManagement', '/replacement', NULL, 'Repeat', 60, true)
    RETURNING menu_id INTO v_dir_id;

    -- PAGE: 換裝派工
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
    VALUES (v_dir_id, '換裝派工', 'PAGE', 'ReplacementOrder', 'orders', 'views/admin/replacement/ReplacementOrderView.vue', 'REPLACEMENT_VIEW', 'FileText', 10, true)
    RETURNING menu_id INTO v_page1_id;

    -- PAGE: 號碼牌管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
    VALUES (v_dir_id, '號碼牌管理', 'PAGE', 'PoleNumber', 'pole-numbers', 'views/admin/replacement/PoleNumberView.vue', 'POLE_NUMBER_MANAGE', 'QrCode', 20, true)
    RETURNING menu_id INTO v_page2_id;

    -- BUTTON permissions (non-visible)
    INSERT INTO menus (parent_id, name, menu_type, permission_code, sort_order, visible)
    VALUES
        (v_page1_id, '換裝派工管理', 'BUTTON', 'REPLACEMENT_MANAGE', 10, false),
        (v_page2_id, '號碼牌操作', 'BUTTON', 'POLE_NUMBER_MANAGE', 10, false);
END $$;

-- Permission entries
INSERT INTO permissions (permission_code, permission_name, description)
VALUES
    ('REPLACEMENT_VIEW', '換裝查看', '查看換裝派工單'),
    ('REPLACEMENT_MANAGE', '換裝管理', '建立/編輯/派工換裝單'),
    ('POLE_NUMBER_MANAGE', '號碼牌管理', '產出/重製號碼牌');

-- Role binding (同 repair 模式)
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.role_id, p.permission_code
FROM roles r CROSS JOIN (VALUES ('REPLACEMENT_VIEW'), ('REPLACEMENT_MANAGE'), ('POLE_NUMBER_MANAGE')) AS p(permission_code)
WHERE r.role_code IN ('ADMIN', 'DEPT_ADMIN');

INSERT INTO role_permissions (role_id, permission_code)
SELECT r.role_id, 'REPLACEMENT_VIEW'
FROM roles r WHERE r.role_code IN ('OPERATOR', 'DEPT_USER', 'FIELD_USER', 'VIEWER');

-- Seed FRONTEND_BASE_URL 系統設定（號碼牌 QR Code 用）
INSERT INTO system_settings (setting_key, setting_value, description, updated_at)
VALUES ('frontend_base_url', 'http://localhost:5173', '前端基礎 URL（號碼牌 QR Code 連結用）', now())
ON CONFLICT (setting_key) DO NOTHING;
```

### 9-2 審計事件

`AuditCategory` 新增 `REPLACEMENT`，`AuditEventType` 新增：
- `CREATE_REPLACEMENT_ORDER`, `UPDATE_REPLACEMENT_ORDER`
- `SELF_CHECK_REPLACEMENT`, `CLOSE_REPLACEMENT`

### 9-3 ErrorCode 新增

```java
// 80xxx: 換裝維護
REPLACEMENT_ORDER_NOT_FOUND("80001", 404, "換裝派工單不存在"),
REPLACEMENT_INVALID_STATUS("80002", 400, "換裝派工單狀態不正確"),
POLE_NUMBER_DUPLICATE("80020", 400, "號碼牌編號重複"),
```

> 材料相關 ErrorCode (85xxx) 已在 Phase 3 定義：MATERIAL_NOT_APPROVED, MATERIAL_NOT_AVAILABLE 等
> ErrorCode 為 5 位字串格式：`ErrorCode(String code, int httpStatus, String message)`

### 9-4 前端

| # | 檔案 | 說明 |
|---|------|------|
| 1 | `src/types/replacement.ts` | ReplacementOrder, ReplacementItem, LightPoleNumber 型別 |
| 2 | `src/api/replacement/index.ts` | 換裝派工 + 明細 + 號碼牌 API |
| 3 | `src/views/admin/replacement/ReplacementOrderView.vue` | 換裝派工列表 + 篩選 + 分頁 |
| 4 | `src/views/admin/replacement/ReplacementOrderDetailView.vue` | 派工單詳情 + 明細 + WorkflowStepper |
| 5 | `src/views/admin/replacement/ReplacementItemDialog.vue` | 換裝明細編輯（選擇燈桿→選舊設備→選合格材料） |
| 6 | `src/views/admin/replacement/SelfCheckView.vue` | 自主檢核表單（逐設備確認+拍照+填入新設備資訊） |
| 7 | `src/views/admin/replacement/PoleNumberView.vue` | 號碼牌管理 + QR Code 預覽 |
| 8 | `src/components/BeforeAfterSpecComparison.vue` | **共用元件**：換裝前後規格比較（雙欄 diff 顯示） |
| 9 | i18n | zh-TW / en / zh-CN 新增 replacement 相關 key |
| 10 | router | 新增 /admin/replacement/* 路由 |

#### 換裝明細 — 設備替換 UI 流程

```
步驟 1：選擇燈桿              步驟 2：選擇要換的子設備
┌─────────────────────┐      ┌─────────────────────┐
│ 搜尋燈桿 🔍          │      │ 燈桿 P-001 的子設備  │
│ ┌───────────────────┐│      │ ┌───────────────────┐│
│ │ P-001 中山北路一段 ││  →   │ │ ☐ L-001 LED燈具   ││ ← 故障設備
│ │ P-002 忠孝東路三段 ││      │ │ ☐ C-001 控制器    ││
│ └───────────────────┘│      │ └───────────────────┘│
└─────────────────────┘      └─────────────────────┘

步驟 3：選擇合格材料            步驟 4：確認
┌─────────────────────┐      ┌─────────────────────────────┐
│ 合格材料 🔍          │      │ 換裝確認                     │
│ ┌───────────────────┐│      │ 燈桿：P-001                 │
│ │ AM-001 B牌 200W   ││  →   │ 舊設備：L-001 LED 150W      │
│ │ AM-002 C牌 200W   ││      │ 新材料：AM-001 B牌 200W     │
│ └───────────────────┘│      │ [確認建立]                   │
└─────────────────────┘      └─────────────────────────────┘
```

#### 換裝前後比較 UI

```
┌──────────────────────┬──────────────────────┐
│     換裝前 ◀ (舊設備)  │     ▶ 換裝後 (新設備) │
├──────────────────────┼──────────────────────┤
│ 設備代碼：L-001       │ 設備代碼：L-025 ✦新   │
│ 設備類型：LED路燈      │ 設備類型：LED路燈      │
│ 瓦數：150W            │ 瓦數：200W ✦變更      │
│ 色溫：4000K           │ 色溫：4000K           │
│ 品牌：A牌             │ 品牌：B牌 ✦變更       │
│ 型號：AL-150          │ 型號：BL-200 ✦變更    │
│ 狀態：DECOMMISSIONED  │ 狀態：ACTIVE          │
├──────────────────────┴──────────────────────┤
│ 使用合格材料：AM-2026-001 B牌 BL-200         │
│ 掛載位置：燈桿 P-001（位置不變）              │
└─────────────────────────────────────────────┘
```

### 9-5 單元測試

| # | 測試類 | 涵蓋範圍 |
|---|--------|----------|
| 1 | `ReplacementOrderControllerTest` | 所有端點 + 權限（含 7 個狀態轉換端點） |
| 2 | `ReplacementOrderServiceTest` | 雙路徑建單 + 完整狀態流轉（dispatch → startWork → selfCheck → submitReview → approve/return/resubmit） |
| 3 | `ReplacementItemServiceTest` | 材料管控驗證 + 燈桿/設備歸屬驗證 + before snapshot |
| 4 | `SelfCheckTest` | 自主檢核 + deviceCode 傳遞 + replaceComponent 呼叫 + new_device_id 回寫 + buildNewDeviceRequest |
| 5 | `ReplacementClosedListenerTest` | E10 事件：結案狀態更新 |
| 6 | `ReplacementNeedMaterialListenerTest` | E6 事件：自動建立 issue_request |
| 7 | `LightPoleNumberServiceTest` | 產出 + 唯一檢查 + QR Code URL 組裝 |

---

## Phase 4 完成標準

- [ ] V45 migration 執行成功，replacement_orders / replacement_items / light_pole_numbers 表建立
- [ ] V46 migration 執行成功，選單權限 + issue_requests FK 補齊 + FRONTEND_BASE_URL seed
- [ ] SystemSettingService.getSetting(key) 通用方法 + SettingKey.FRONTEND_BASE_URL 新增
- [ ] IssueService.createFromReplacement(orderId) 新增
- [ ] AuditCategory.REPLACEMENT + 4 個 AuditEventType 新增
- [ ] ErrorCode 80xxx 系列新增
- [ ] 換裝派工單 CRUD + 分頁篩選 + DataScope 可用
- [ ] 換裝派工單完整狀態流轉可用（dispatch → startWork → selfCheck → submitReview → approve/return/resubmit）
- [ ] 換裝明細 CRUD + 材料管控驗證（必須使用合格材料）可用
- [ ] 換裝明細可指定燈桿 + 舊設備 + 合格材料
- [ ] 自主檢核流程可用（承商提供 deviceCode + 呼叫 replaceComponent 執行實際換裝 + 回寫 new_device_id）
- [ ] E6 事件：派工 → 自動建立 issue_request（驗證）
- [ ] E10 事件：結案 → 更新狀態（驗證）
- [ ] 號碼牌管理（產出/重製/QR Code）可用
- [ ] 前端換裝管理頁面（含詳情+明細+WorkflowStepper）可用
- [ ] 前端號碼牌頁面可用
- [ ] 共用元件 BeforeAfterSpecComparison 可用
- [ ] 所有單元測試通過
- [ ] `mvn test` 全過
