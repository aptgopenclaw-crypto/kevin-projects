# Phase 5：整合與閉環 — 跨模組串接 + Seed + 觸發器

> Flyway V44 ｜涵蓋模組：03-07 全部
> 前置條件：Phase 1-4 全部完成（Phase 3=材料管理 V40-41, Phase 4=換裝維護 V42-43）
> 目標：補齊所有跨模組 FK、Seed 全量角色權限、啟用結案自動同步觸發邏輯

---

## 總覽

| Step | Flyway | 範圍 | 產出 |
|------|--------|------|------|
| 10 | V39 | 跨模組 FK 補齊 + workflow seed + role_permissions + 結案同步 + 安全庫存 + 端對端測試 | 整合 migration + 全套 seed |

---

## 10-1 Flyway Migration

**檔案**：`V39__integration__cross_module_fk_and_seed.sql`

### 跨模組 FK 約束補齊

Phase 1-4 各自建表時因依賴順序，部分 FK 尚未加上硬約束（表尚未建立）。
V39 統一補齊：

```sql
-- ============================================================
-- 跨模組 FK 約束
-- ============================================================

-- 以下 FK 已在 Phase 3(V40) 和 Phase 4(V42) 中直接定義，無需補齊：
-- replacement_items.material_spec_id → material_specs (V42 已含)
-- replacement_items.approved_material_id → approved_materials (V42 已含)
-- approved_materials.material_spec_id → material_specs (V40 已含)
-- issue_requests.replacement_order_id → replacement_orders (V42 已含)

-- device_events 補齊 FK（若 Phase 1 建表時未加）
ALTER TABLE device_events
    ADD CONSTRAINT fk_device_events_repair_ticket
    FOREIGN KEY (repair_ticket_id) REFERENCES repair_tickets(id);

ALTER TABLE device_events
    ADD CONSTRAINT fk_device_events_replacement_item
    FOREIGN KEY (replacement_item_id) REFERENCES replacement_items(id);
```

### 簽核中心選單

```sql
-- ============================================================
-- 03 簽核中心 Menu（50-52）
-- ============================================================
INSERT INTO menus (menu_id, menu_name, path, parent_menu_id, sort_order, icon, is_active) VALUES
(50, '簽核中心', '/admin/workflow', NULL, 5, 'ClipboardCheck', true),
(51, '待辦案件', '/admin/workflow/pending', 50, 1, 'ListTodo', true),
(52, '代理人設定', '/admin/workflow/delegate', 50, 2, 'UserCheck', true);
```

### 角色權限完整綁定

```sql
-- ============================================================
-- Role-Permission Binding（完整版）
-- ============================================================

-- ROLE_ADMIN：全部權限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_name = 'ROLE_ADMIN'
  AND p.permission_key IN (
    -- 04 資產
    'DEVICE_VIEW', 'DEVICE_MANAGE', 'CIRCUIT_VIEW', 'CIRCUIT_MANAGE',
    'FAULT_VIEW', 'FAULT_MANAGE', 'DEVICE_EXPORT',
    'CONTRACT_VIEW', 'CONTRACT_MANAGE',
    -- 05 報修
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH',
    'INSPECTION_VIEW', 'INSPECTION_MANAGE',
    -- 06 換裝
    'REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE', 'POLE_NUMBER_MANAGE',
    -- 07 材料
    'MATERIAL_VIEW', 'MATERIAL_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE',
    -- 03 簽核
    'WORKFLOW_VIEW', 'WORKFLOW_MANAGE', 'DELEGATE_MANAGE'
  )
ON CONFLICT DO NOTHING;

-- ROLE_OPERATOR：操作類權限（不含系統設定）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_name = 'ROLE_OPERATOR'
  AND p.permission_key IN (
    'DEVICE_VIEW', 'DEVICE_MANAGE', 'CIRCUIT_VIEW', 'CIRCUIT_MANAGE',
    'FAULT_VIEW', 'FAULT_MANAGE', 'DEVICE_EXPORT',
    'CONTRACT_VIEW',
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH',
    'INSPECTION_VIEW', 'INSPECTION_MANAGE',
    'REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE',
    'MATERIAL_VIEW', 'INVENTORY_VIEW',
    'WORKFLOW_VIEW', 'WORKFLOW_MANAGE', 'DELEGATE_MANAGE'
  )
ON CONFLICT DO NOTHING;

-- ROLE_DEPT_ADMIN：部門管理者
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_name = 'ROLE_DEPT_ADMIN'
  AND p.permission_key IN (
    'DEVICE_VIEW', 'DEVICE_MANAGE', 'CIRCUIT_VIEW',
    'FAULT_VIEW', 'FAULT_MANAGE',
    'CONTRACT_VIEW',
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH',
    'INSPECTION_VIEW',
    'REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE',
    'MATERIAL_VIEW', 'INVENTORY_VIEW',
    'WORKFLOW_VIEW', 'WORKFLOW_MANAGE', 'DELEGATE_MANAGE'
  )
ON CONFLICT DO NOTHING;

-- ROLE_DEPT_USER：部門使用者
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_name = 'ROLE_DEPT_USER'
  AND p.permission_key IN (
    'DEVICE_VIEW', 'CIRCUIT_VIEW', 'FAULT_VIEW',
    'CONTRACT_VIEW',
    'REPAIR_VIEW',
    'INSPECTION_VIEW',
    'REPLACEMENT_VIEW',
    'MATERIAL_VIEW', 'INVENTORY_VIEW',
    'WORKFLOW_VIEW'
  )
ON CONFLICT DO NOTHING;

-- ROLE_FIELD_USER：外勤人員（可完工回報）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_name = 'ROLE_FIELD_USER'
  AND p.permission_key IN (
    'DEVICE_VIEW', 'FAULT_VIEW',
    'REPAIR_VIEW', 'REPAIR_MANAGE',
    'REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE',
    'WORKFLOW_VIEW'
  )
ON CONFLICT DO NOTHING;

-- ROLE_VIEWER：只讀
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_name = 'ROLE_VIEWER'
  AND p.permission_key IN (
    'DEVICE_VIEW', 'CIRCUIT_VIEW', 'FAULT_VIEW',
    'CONTRACT_VIEW',
    'REPAIR_VIEW', 'INSPECTION_VIEW',
    'REPLACEMENT_VIEW',
    'MATERIAL_VIEW', 'INVENTORY_VIEW',
    'WORKFLOW_VIEW'
  )
ON CONFLICT DO NOTHING;

-- ROLE_MONITOR：監控
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.role_name = 'ROLE_MONITOR'
  AND p.permission_key IN (
    'DEVICE_VIEW', 'CIRCUIT_VIEW', 'FAULT_VIEW',
    'REPAIR_VIEW',
    'INVENTORY_VIEW',
    'WORKFLOW_VIEW'
  )
ON CONFLICT DO NOTHING;
```

---

## 10-2 結案自動同步觸發器（啟用完整版）

Phase 2-3 各自預留了 `@EventListener` hook，Phase 5 統一啟用完整版：

### E9 完整版：報修結案 → 資產更新 + 歷程 + 扣庫確認

```java
@Component
public class RepairClosedListener {

    @EventListener
    @Transactional
    public void onRepairClosed(WorkflowTransitionEvent event) {
        if (!"REPAIR_CLOSE".equals(event.getWorkflowType())) return;
        if (!"CLOSED".equals(event.getTargetStep())) return;

        RepairTicket ticket = repairTicketRepo.findById(event.getTicketId()).orElseThrow();

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

        // 3. 若有關聯的領料申請，確認扣庫（Phase 5 啟用）
        List<IssueRequest> issueRequests = issueRequestRepo
            .findByRepairTicketId(ticket.getId());
        for (IssueRequest ir : issueRequests) {
            issueService.confirmDeduction(ir.getId());
        }
    }
}
```

### E10 完整版：換裝結案 → 資產更新 + 歷程 + 扣庫確認

```java
@Component
public class ReplacementClosedListener {

    @EventListener
    @Transactional
    public void onReplacementClosed(WorkflowTransitionEvent event) {
        if (!"REPLACEMENT_REVIEW".equals(event.getWorkflowType())) return;
        if (!"CLOSED".equals(event.getTargetStep())) return;

        ReplacementOrder order = orderRepo.findById(event.getTicketId()).orElseThrow();
        List<ReplacementItem> items = itemRepo.findByOrderId(order.getId());

        for (ReplacementItem item : items) {
            // 1. 正式更新設備資產
            deviceService.updateFromReplacement(item.getDeviceId(), item.getAfterSpec());

            // 2. 寫入 device_events 歷程
            deviceEventService.recordEvent(DeviceEvent.builder()
                .deviceId(item.getDeviceId())
                .eventType(DeviceEventType.REPLACE)
                .description("換裝完成：" + item.getBeforeDeviceType() + " → " + item.getAfterDeviceType())
                .replacementItemId(item.getId())
                .build());
        }

        // 3. 確認材料扣庫（Phase 5 啟用完整版）
        issueService.confirmDeduction(order.getId());

        // 4. 若關聯報修工單，一併結案
        if (order.getRepairTicketId() != null) {
            RepairTicket repair = repairTicketRepo.findById(order.getRepairTicketId()).orElse(null);
            if (repair != null && repair.getStatus() != RepairTicketStatus.CLOSED) {
                // 觸發報修結案流程
                WorkflowInstance instance = workflowService
                    .findByTicket("REPAIR_TICKET", repair.getId());
                workflowService.transition(instance.getId(), "CLOSED",
                    0L, "COMPLETE", "換裝結案自動結案報修", null);
            }
        }
    }
}
```

---

## 10-3 庫存安全量預警（整合通知模組）

```java
@Component
public class LowStockAlertListener {

    private final NotificationService notificationService;

    /**
     * 監聽 E12：庫存低於安全量
     * 推播通知給材料管理員
     */
    @EventListener
    public void onLowStock(LowStockAlertEvent event) {
        // 查詢有 MATERIAL_MANAGE 權限的使用者
        List<Long> materialManagers = userPermissionService
            .findUsersByPermissionAndTenant("MATERIAL_MANAGE", event.getTenantId());

        String message = String.format(
            "⚠ 庫存預警：%s（%s）目前庫存 %d，低於安全量 %d",
            event.getMaterialSpecName(), event.getWarehouseName(),
            event.getQuantityOnHand(), event.getSafetyStock());

        for (Long userId : materialManagers) {
            notificationService.push(userId, "LOW_STOCK_ALERT", message);
        }
    }
}
```

---

## 10-4 端對端整合測試

### 完整生命週期測試（Happy Path）

```java
@SpringBootTest
@Transactional
class FullLifecycleIntegrationTest {

    /**
     * 完整路徑：民眾報修 → 障礙審核 → 報修派工 → 換裝 → 領料 → 完工 → 結案 → 資產更新
     */
    @Test
    void testFullRepairToReplacementLifecycle() {
        // 1. Setup: 建立設備 + 回路 + 契約 + 合格材料 + 庫存
        Device device = createTestDevice();
        Circuit circuit = createTestCircuit(device);
        Contract contract = createTestContract();
        MaterialSpec spec = createTestMaterialSpec();
        ApprovedMaterial approved = createTestApprovedMaterial(spec, contract);
        setupInventory(spec, 100);

        // 2. 建立障礙工單
        FaultTicket fault = faultTicketService.create(FaultTicketRequest.builder()
            .deviceId(device.getId())
            .circuitId(circuit.getId())
            .source("CITIZEN_REPORT")
            .description("路燈不亮")
            .build());
        assertThat(fault.getStatus()).isEqualTo("OPEN");

        // 3. 障礙審核通過 → E1 自動建立 repair_ticket
        WorkflowInstance faultWf = workflowService.findByTicket("FAULT_TICKET", fault.getId());
        workflowService.transition(faultWf.getId(), "REVIEW", operatorId, "SUBMIT", "提交審核", null);
        workflowService.transition(faultWf.getId(), "CONFIRMED", operatorId, "APPROVE", "確認障礙", null);

        // 驗證 repair_ticket 已自動建立
        RepairTicket repair = repairTicketRepo.findByFaultTicketId(fault.getId()).orElseThrow();
        assertThat(repair.getSource()).isEqualTo("FAULT_TICKET");
        assertThat(repair.getStatus()).isEqualTo("PENDING");

        // 驗證：E1 觸發設備狀態 → REPORTED
        assertThat(deviceRepo.findById(device.getId()).get().getStatus())
            .isEqualTo(DeviceStatus.REPORTED);

        // 4. 收案 + 派工
        repairTicketService.accept(repair.getId());
        repairTicketService.dispatch(repair.getId(), DispatchRequest.builder()
            .assignedTo(fieldUserId)
            .assignedOrg("光輝維護公司")
            .contractId(contract.getId())
            .build());
        assertThat(repairTicketRepo.findById(repair.getId()).get().getStatus())
            .isEqualTo("DISPATCHED");

        // 驗證：E4 觸發設備狀態 → UNDER_REPAIR
        assertThat(deviceRepo.findById(device.getId()).get().getStatus())
            .isEqualTo(DeviceStatus.UNDER_REPAIR);

        // 5. 施工中 → 需要換裝 → E5 建立 replacement_order
        repairTicketService.startProgress(repair.getId());
        ReplacementOrder replacement = replacementOrderService.createFromRepair(
            repair.getId(), ReplacementOrderRequest.builder()
                .orderType("REPLACEMENT")
                .build());

        // 6. 新增換裝明細（使用合格材料）
        replacementItemService.addItem(replacement.getId(), ReplacementItemRequest.builder()
            .deviceId(device.getId())
            .afterDeviceType("LUMINAIRE")
            .afterSpec(Map.of("wattage", 200, "color_temp", 4000))
            .approvedMaterialId(approved.getId())
            .materialSpecId(spec.getId())
            .build());

        // 7. 換裝需領料 → E6 建立 issue_request
        IssueRequest issueReq = issueService.createFromReplacement(replacement.getId());

        // 8. 領料審核 + 出庫 → E7 扣庫
        issueService.approve(issueReq.getId());
        issueService.issue(issueReq.getId(), List.of(
            IssueRecordRequest.builder()
                .inventoryId(inventory.getId())
                .materialSpecId(spec.getId())
                .quantity(1)
                .build()));

        // 驗證庫存扣減
        Inventory inv = inventoryRepo.findById(inventory.getId()).orElseThrow();
        assertThat(inv.getQuantityOnHand()).isEqualTo(99);

        // 9. 自主檢核 → E11 預更新資產
        replacementOrderService.selfCheck(replacement.getId(), SelfCheckRequest.builder()
            .items(List.of(new SelfCheckItem(replacementItem.getId(), "檢核通過", null)))
            .build());

        // 10. 報竣送審 → 結案審核通過 → E10 正式更新資產
        WorkflowInstance replWf = workflowService.findByTicket("REPLACEMENT_ORDER", replacement.getId());
        workflowService.transition(replWf.getId(), "PENDING_REVIEW", fieldUserId, "SUBMIT", "報竣", null);
        workflowService.transition(replWf.getId(), "CLOSED", operatorId, "APPROVE", "結案", null);

        // 驗證：設備已更新
        Device updated = deviceRepo.findById(device.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
        // 驗證：JSONB attributes 已更新
        assertThat(updated.getAttributes().get("wattage")).isEqualTo(200);

        // 驗證：device_events 歷程已記錄
        List<DeviceEvent> events = deviceEventRepo.findByDeviceId(device.getId());
        assertThat(events).anyMatch(e -> e.getEventType().equals("REPLACE"));

        // 驗證：庫存扣庫已確認
        assertThat(inventoryRepo.findById(inventory.getId()).get().getQuantityOnHand()).isEqualTo(99);
    }
}
```

### 異常路徑測試

| # | 測試場景 | 驗證點 |
|---|----------|--------|
| 1 | 障礙審核駁回（誤報） | fault_ticket → REJECTED，不建立 repair_ticket |
| 2 | 結案退回補件 | PENDING_REVIEW → RETURNED → COMPLETION_REPORTED → 重新送審 |
| 3 | 改分轉送 | DISPATCHED → TRANSFERRED，建立新 dispatch 給其他單位 |
| 4 | 庫存不足拒絕出庫 | issue → INSUFFICIENT_INVENTORY 例外 |
| 5 | 使用未合格材料 | addItem → MATERIAL_NOT_APPROVED 例外 |
| 6 | 非法狀態轉換 | PENDING → CLOSED → WORKFLOW_INVALID_TRANSITION |
| 7 | 非指定人操作 | 非 assigned_to 嘗試審核 → WORKFLOW_NOT_ASSIGNED_TO_USER |
| 8 | 代理人審核 | 代理期間內，delegate 可代替 delegator 審核 |
| 9 | 關聯障礙合併 | 同回路 ≥3 工單 → 自動建立 fault_correlation |
| 10 | Gateway 離線 | heartbeat timeout → AUTO_ALERT fault_ticket → E1 |

### 跨模組事件驗證矩陣

| 事件 | 觸發條件 | 預期結果 | 測試方法 |
|------|----------|----------|----------|
| E1 | fault_ticket CONFIRMED | repair_ticket 自動建立 | 整合測試 |
| E2 | 同回路 ≥3 工單 | fault_correlation 建立 | 單元測試 |
| E3 | Gateway heartbeat timeout | AUTO_ALERT fault_ticket | 排程測試 |
| E4 | repair_ticket DISPATCHED | workflow_instance 建立 | 整合測試 |
| E5 | 報修需換裝 | replacement_order 建立 | 整合測試 |
| E6 | 換裝需領料 | issue_request 建立 | 整合測試 |
| E7 | issue_request APPROVED | inventory 扣庫 | 整合測試 |
| E8 | 外勤完工回傳 | workflow_step_logs + 附件 | 整合測試 |
| E9 | repair_ticket CLOSED | devices + device_events 更新 | 整合測試 |
| E10 | replacement CLOSED | devices + device_events + inventory 確認 | 整合測試 |
| E11 | 自主檢核 | devices 預更新（provisional） | 單元測試 |
| E12 | quantity < safety_stock | 推播通知 | 排程測試 |
| E13 | 巡查 NEED_REPAIR | fault_ticket 建立 | 單元測試 |

---

## 10-5 前端路由整合

### 完整路由表

```typescript
// router/index.ts — 新增路由
const adminRoutes = [
  // ... 既有路由 (01 使用者管理, 02 公告)

  // 04 資產管理 (Phase 1)
  {
    path: '/admin/asset',
    name: 'AssetManagement',
    meta: { title: 'menu.assetManagement', icon: 'Server', permission: 'DEVICE_VIEW' },
    children: [
      { path: 'devices', name: 'DeviceManagement', component: () => import('@/views/admin/asset/DeviceManagementView.vue'), meta: { permission: 'DEVICE_VIEW' } },
      { path: 'circuits', name: 'CircuitManagement', component: () => import('@/views/admin/asset/CircuitManagementView.vue'), meta: { permission: 'CIRCUIT_VIEW' } },
      { path: 'faults', name: 'FaultTickets', component: () => import('@/views/admin/asset/FaultTicketView.vue'), meta: { permission: 'FAULT_VIEW' } },
      { path: 'contracts', name: 'ContractManagement', component: () => import('@/views/admin/asset/ContractManagementView.vue'), meta: { permission: 'CONTRACT_VIEW' } },
    ]
  },

  // 05 報修維護 (Phase 2)
  {
    path: '/admin/repair',
    name: 'RepairMaintenance',
    meta: { title: 'menu.repairMaintenance', icon: 'Wrench', permission: 'REPAIR_VIEW' },
    children: [
      { path: 'tickets', name: 'RepairTickets', component: () => import('@/views/admin/repair/RepairTicketView.vue'), meta: { permission: 'REPAIR_VIEW' } },
      { path: 'tickets/:id', name: 'RepairTicketDetail', component: () => import('@/views/admin/repair/RepairTicketDetailView.vue'), meta: { permission: 'REPAIR_VIEW' } },
      { path: 'inspections', name: 'InspectionManagement', component: () => import('@/views/admin/repair/InspectionView.vue'), meta: { permission: 'INSPECTION_VIEW' } },
    ]
  },

  // 06 換裝維護 (Phase 4)
  {
    path: '/admin/replacement',
    name: 'ReplacementMaintenance',
    meta: { title: 'menu.replacementMaintenance', icon: 'RefreshCw', permission: 'REPLACEMENT_VIEW' },
    children: [
      { path: 'orders', name: 'ReplacementOrders', component: () => import('@/views/admin/replacement/ReplacementOrderView.vue'), meta: { permission: 'REPLACEMENT_VIEW' } },
      { path: 'orders/:id', name: 'ReplacementOrderDetail', component: () => import('@/views/admin/replacement/ReplacementOrderDetailView.vue'), meta: { permission: 'REPLACEMENT_VIEW' } },
      { path: 'pole-numbers', name: 'PoleNumbers', component: () => import('@/views/admin/replacement/PoleNumberView.vue'), meta: { permission: 'POLE_NUMBER_MANAGE' } },
    ]
  },

  // 07 材料管理 (Phase 3)
  {
    path: '/admin/material',
    name: 'MaterialManagement',
    meta: { title: 'menu.materialManagement', icon: 'Package', permission: 'MATERIAL_VIEW' },
    children: [
      { path: 'specs', name: 'MaterialSpecs', component: () => import('@/views/admin/material/MaterialSpecView.vue'), meta: { permission: 'MATERIAL_VIEW' } },
      { path: 'inventory', name: 'InventoryManagement', component: () => import('@/views/admin/material/InventoryView.vue'), meta: { permission: 'INVENTORY_VIEW' } },
      { path: 'purchasing', name: 'PurchaseManagement', component: () => import('@/views/admin/material/PurchaseOrderView.vue'), meta: { permission: 'MATERIAL_MANAGE' } },
    ]
  },

  // 03 簽核中心 (Phase 1 + Phase 5 整合)
  {
    path: '/admin/workflow',
    name: 'WorkflowCenter',
    meta: { title: 'menu.workflowCenter', icon: 'ClipboardCheck', permission: 'WORKFLOW_VIEW' },
    children: [
      { path: 'pending', name: 'PendingTasks', component: () => import('@/views/admin/workflow/PendingTasksView.vue'), meta: { permission: 'WORKFLOW_VIEW' } },
      { path: 'delegate', name: 'DelegateSettings', component: () => import('@/views/admin/workflow/DelegateSettingsView.vue'), meta: { permission: 'DELEGATE_MANAGE' } },
    ]
  },
];
```

### 側邊欄選單顯示邏輯

```typescript
// 根據使用者權限動態過濾可見選單
const visibleMenus = computed(() => {
  return adminRoutes.filter(route =>
    hasPermission(route.meta?.permission)
  ).map(route => ({
    ...route,
    children: route.children?.filter(child =>
      hasPermission(child.meta?.permission)
    )
  }));
});
```

---

## 10-6 i18n 完整 Key 清單

### zh-TW 新增 Key 總覽

```
menu.assetManagement        = 資產管理
menu.deviceManagement       = 設備管理
menu.circuitManagement      = 回路管理
menu.faultTickets           = 障礙工單
menu.contractManagement     = 契約管理
menu.repairMaintenance      = 報修維護
menu.repairTickets          = 報修工單
menu.inspectionManagement   = 巡查管理
menu.replacementMaintenance = 換裝維護
menu.replacementOrders      = 換裝派工
menu.poleNumbers            = 號碼牌管理
menu.materialManagement     = 材料管理
menu.materialSpecs          = 材料規格
menu.inventoryManagement    = 庫存管理
menu.purchaseManagement     = 採購管理
menu.workflowCenter         = 簽核中心
menu.pendingTasks           = 待辦案件
menu.delegateSettings       = 代理人設定
```

---

## 10-7 Seed 資料（開發/測試環境）

```sql
-- ============================================================
-- 開發/測試環境 Seed（V39 同步插入）
-- ============================================================

-- 範例庫別
INSERT INTO warehouses (tenant_id, warehouse_code, warehouse_name, location) VALUES
('TENANT_A', 'WH-MAIN', '主庫房', '台北市信義區信義路五段7號'),
('TENANT_A', 'WH-NORTH', '北區分庫', '台北市中山區中山北路二段48號'),
('TENANT_A', 'WH-SOUTH', '南區分庫', '台北市大安區復興南路一段390號');

-- 範例材料規格
INSERT INTO material_specs (tenant_id, spec_code, spec_name, category, unit, attributes) VALUES
('TENANT_A', 'MS-LED150', 'LED路燈 150W', 'LUMINAIRE', 'PCS', '{"wattage":150,"color_temp":4000,"lumen":18000}'),
('TENANT_A', 'MS-LED200', 'LED路燈 200W', 'LUMINAIRE', 'PCS', '{"wattage":200,"color_temp":4000,"lumen":24000}'),
('TENANT_A', 'MS-CTRL01', '智慧控制器 Type-A', 'CONTROLLER', 'PCS', '{"protocol":"LoRa","brand":"SmartCity"}'),
('TENANT_A', 'MS-POLE6M', '燈桿 6M 鍍鋅鋼管', 'POLE', 'PCS', '{"height_m":6,"material":"galvanized_steel"}');

-- 範例廠商
INSERT INTO suppliers (tenant_id, supplier_code, supplier_name, contact_name, contact_phone) VALUES
('TENANT_A', 'SUP-001', '光輝照明工程有限公司', '張先生', '02-1234-5678'),
('TENANT_A', 'SUP-002', '智慧城市科技股份有限公司', '李小姐', '02-8765-4321');

-- 範例庫存
INSERT INTO inventory (tenant_id, warehouse_id, material_spec_id, quantity_on_hand, safety_stock) VALUES
('TENANT_A', (SELECT id FROM warehouses WHERE warehouse_code='WH-MAIN' AND tenant_id='TENANT_A'),
             (SELECT id FROM material_specs WHERE spec_code='MS-LED150' AND tenant_id='TENANT_A'), 50, 10),
('TENANT_A', (SELECT id FROM warehouses WHERE warehouse_code='WH-MAIN' AND tenant_id='TENANT_A'),
             (SELECT id FROM material_specs WHERE spec_code='MS-LED200' AND tenant_id='TENANT_A'), 30, 10),
('TENANT_A', (SELECT id FROM warehouses WHERE warehouse_code='WH-MAIN' AND tenant_id='TENANT_A'),
             (SELECT id FROM material_specs WHERE spec_code='MS-CTRL01' AND tenant_id='TENANT_A'), 20, 5);
```

---

## Phase 5 完成標準

- [ ] V39 migration 執行成功，所有 FK 約束、選單、權限、role binding 齊全
- [ ] 全部 7 種角色的權限綁定正確（ADMIN 全有、VIEWER 只讀、FIELD_USER 可完工回報）
- [ ] E9 完整版：報修結案 → 資產更新 + 歷程 + 扣庫確認（端對端驗證）
- [ ] E10 完整版：換裝結案 → 資產更新 + 歷程 + 扣庫確認 + 連動報修結案（端對端驗證）
- [ ] E12：每日安全庫存排程 → 低庫存推播通知（驗證）
- [ ] 端對端整合測試通過（完整 Happy Path）
- [ ] 所有 10 個異常路徑測試通過
- [ ] 所有 13 個跨模組事件驗證通過
- [ ] 前端路由整合完成，側邊欄根據權限動態顯示
- [ ] i18n 全語言 key 完整（zh-TW / en / zh-CN）
- [ ] Seed 資料可用（開發環境）
- [ ] `mvn test` 全過（含整合測試）
- [ ] 前端 `npm run build` 無錯誤
