# 跨模組統一設計：03 簽核 ╳ 04 資產 ╳ 05 報修 ╳ 06 換裝 ╳ 07 材料

> 版本：v1.0 ｜日期：2026-04-22
> 本文件定義模組 03-07 的**統一資料模型、工單生命週期、跨模組事件流、實作優先序**。

---

## 1. 全域生命週期總覽

```
                        ┌─────────── 主動偵測 ──────────┐
                        │  心跳掃描 / SIM 到期 / 關聯分析 │
                        └──────────┬────────────────────┘
                                   ↓ AUTO_ALERT
 民眾報修 ──→ ┌──────────────────────────────────┐
 巡檢通報 ──→ │  04 fault_tickets（障礙申告）      │
 1999陳情 ──→ │  source: CITIZEN / PATROL / AUTO  │
              └──────────┬───────────────────────┘
                         │
          ┌──────────────↓──────────────┐
          │  03 workflow: FAULT_REVIEW   │
          │  審核：確認障礙 / 駁回(誤報)  │
          │         / 合併(關聯障礙)      │
          └──────────────┬──────────────┘
                         │ 審核通過
                         ↓
              ┌──────────────────────────────────┐
              │  05 repair_tickets（報修派工）      │
              │  立案 → 派工 → 施工              │
              └──────────┬───────────────────────┘
                         │
            ┌────────────┤ 需要換裝？
            │ 否         │ 是
            ↓            ↓
      原地維修    ┌──────────────────────────────────┐
        │        │  06 replacement_orders（換裝派工）  │
        │        │  材料管控 → 施工 → 自主檢核        │
        │        └──────────┬───────────────────────┘
        │                   │
        │          ┌────────↓────────┐
        │          │ 07 領料出庫      │
        │          │ inventory 扣庫   │
        │          └────────┬────────┘
        │                   │
        └───────┬───────────┘
                │ 施工完成
                ↓
     ┌──────────────────────────────┐
     │  外勤人員拍照回傳              │
     │  (維修前/中/後 + GPS + 時間)   │
     └──────────────┬───────────────┘
                    ↓
     ┌──────────────────────────────────┐
     │  03 workflow: REPAIR_CLOSE_REVIEW │
     │  派工人審核結案 / 退回補件         │
     └──────────────┬───────────────────┘
                    │ 審核通過
                    ↓
     ┌──────────────────────────────────┐
     │  04 devices 自動同步更新資產       │
     │  04 device_events 寫入歷程        │
     │  07 inventory 確認扣庫             │
     └──────────────────────────────────┘
```

---

## 2. 統一 ERD

### 2-1 表清單與模組歸屬

| # | 表名 | 歸屬模組 | 說明 | 多租戶 |
|---|------|----------|------|--------|
| **03 簽核引擎** | | | | |
| 1 | `workflow_definitions` | 03 | 流程定義（案件類型 → 步驟範本） | 否（全域） |
| 2 | `workflow_steps_template` | 03 | 流程步驟範本（定義每種流程有哪些步驟） | 否（全域） |
| 3 | `workflow_instances` | 03 | 流程實例（與具體工單綁定） | 是 |
| 4 | `workflow_step_logs` | 03 | 流程步驟操作歷程（每次審核/退回/派工） | 是 |
| 5 | `delegate_settings` | 03 | 代理人設定 | 是 |
| **04 資產管理** | | | | |
| 6 | `devices` | 04 | 設備主表 | 是 |
| 7 | `circuits` | 04 | 電力回路 | 是 |
| 8 | `device_events` | 04 | 設備事件統一歷程（跨 05/06 寫入） | 是 |
| 9 | `fault_tickets` | 04 | 障礙申告工單 | 是 |
| 10 | `fault_correlations` | 04 | 關聯障礙歸因 | 是 |
| 11 | `contracts` | 04 | 契約資訊 | 是 |
| **05 報修維護** | | | | |
| 12 | `repair_tickets` | 05 | 報修維護工單 | 是 |
| 13 | `repair_dispatches` | 05 | 派工紀錄 | 是 |
| 14 | `ticket_attachments` | 05 | 工單附件（共用：fault/repair/replacement） | 是 |
| 15 | `inspection_tasks` | 05 | 巡查任務 | 是 |
| 16 | `inspection_records` | 05 | 巡查紀錄 | 是 |
| **06 換裝維護** | | | | |
| 17 | `replacement_orders` | 06 | 換裝派工單 | 是 |
| 18 | `replacement_items` | 06 | 換裝作業明細（逐設備） | 是 |
| 19 | `light_pole_numbers` | 06 | 路燈號碼牌編號管理 | 是 |
| 20 | `approved_materials` | 06/07 | 經審驗合格材料清單 | 是 |
| **07 材料管理** | | | | |
| 21 | `warehouses` | 07 | 庫別 | 是 |
| 22 | `material_specs` | 07 | 材料規格 | 是 |
| 23 | `suppliers` | 07 | 供應/承攬廠商 | 是 |
| 24 | `purchase_orders` | 07 | 採購單 | 是 |
| 25 | `purchase_items` | 07 | 採購明細 | 是 |
| 26 | `receiving_records` | 07 | 收料紀錄 | 是 |
| 27 | `inventory` | 07 | 庫存（庫別 + 材料） | 是 |
| 28 | `inventory_adjustments` | 07 | 庫存調整/盤點 | 是 |
| 29 | `issue_requests` | 07 | 領料申請 | 是 |
| 30 | `issue_records` | 07 | 出料紀錄 | 是 |
| 31 | `disposal_records` | 07 | 廢品處理紀錄 | 是 |

### 2-2 跨模組 FK 關聯圖

```
                     ┌─────────────────────────────────────────────────┐
                     │              03 Workflow Engine                  │
                     │                                                 │
                     │  workflow_definitions ──→ workflow_steps_template │
                     │         │                                       │
                     │         ↓                                       │
                     │  workflow_instances ──→ workflow_step_logs       │
                     │    │  ticket_type                               │
                     │    │  ticket_id                                 │
                     │    │  assigned_to ──→ users                     │
                     │                                                 │
                     │  delegate_settings                              │
                     │    │  delegator_id ──→ users                    │
                     │    │  delegate_id ──→ users                     │
                     └────┬────────────────────────┬───────────────────┘
                          │                        │
      ticket_type/id 多態 FK                       │
     ┌────────────────────┼────────────────────────┤
     ↓                    ↓                        ↓
┌─────────────┐  ┌──────────────┐  ┌────────────────────┐
│04 fault_    │  │05 repair_    │  │06 replacement_     │
│  tickets    │  │  tickets     │  │  orders            │
│             │  │              │  │                    │
│ device_id──→│  │ fault_ticket │  │ repair_ticket_id──→│
│  devices    │  │  _id ──→ 04  │  │  05 repair_tickets │
│             │  │              │  │                    │
│ circuit_id──→│  │ device_id──→│  │ contract_id ──→    │
│  circuits   │  │  devices     │  │  04 contracts      │
│             │  │              │  │                    │
│correlation_ │  │ contract_id──→│  └────────┬───────────┘
│ id ──→ 04   │  │  04 contracts │           │
│fault_       │  │              │           ↓
│correlations │  └──────┬───────┘  ┌────────────────────┐
└─────────────┘         │          │06 replacement_items │
                        │          │                    │
                        │          │ device_id ──→ 04   │
                        │          │ material_spec_id──→│
                        │          │  07 material_specs  │
                        │          │ approved_material  │
                        │          │  _id ──→ 06/07     │
                        │          └────────┬───────────┘
                        │                   │
           ┌────────────┘                   │ 完工扣庫
           ↓                                ↓
   ┌──────────────┐              ┌────────────────────┐
   │05 repair_    │              │07 issue_records     │
   │ dispatches   │              │                    │
   │              │              │ replacement_item   │
   │ assigned_to──→ users        │  _id ──→ 06        │
   │ contractor_id ──→           │ inventory_id ──→   │
   │  04 contracts/suppliers     │  07 inventory       │
   └──────────────┘              └────────────────────┘

   ┌────────────────┐            ┌────────────────────┐
   │04 device_events│            │05 ticket_attachments│
   │                │            │                    │
   │ device_id ──→  │            │ ticket_type (多態)  │
   │  04 devices    │            │ ticket_id           │
   │                │            │                    │
   │ repair_ticket  │            │ file_url, gps_lat  │
   │  _id ──→ 05   │            │ gps_lng, taken_at   │
   │ replacement    │            └────────────────────┘
   │  _item_id ──→06│
   └────────────────┘
```

### 2-3 核心表 DDL

> 以下僅列出 **新增** 的表（04 的 devices / circuits / fault_tickets / fault_correlations / device_events 已在 4-3 x-plan 定義，此處不重複）。

#### 03 簽核引擎

```sql
-- ============================================================
-- 03-1 流程定義
-- ============================================================
CREATE TABLE workflow_definitions (
    id              BIGSERIAL       PRIMARY KEY,
    workflow_type   VARCHAR(50)     NOT NULL UNIQUE,
    workflow_name   VARCHAR(200)    NOT NULL,
    description     TEXT,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now()
);

COMMENT ON TABLE workflow_definitions IS '流程定義：每種案件類型對應一個流程';
COMMENT ON COLUMN workflow_definitions.workflow_type IS
  'FAULT_REVIEW / REPAIR_DISPATCH / REPAIR_CLOSE / REPLACEMENT_REVIEW / ASSET_CHANGE';

INSERT INTO workflow_definitions (workflow_type, workflow_name, description) VALUES
('FAULT_REVIEW',       '障礙確認審核', '障礙申告 → 審核確認 → 通過/駁回/合併'),
('REPAIR_DISPATCH',    '報修派工流程', '立案 → 派工 → 施工'),
('REPAIR_CLOSE',       '報修結案審核', '完工送審 → 審核 → 結案/退回'),
('REPLACEMENT_REVIEW', '換裝審核流程', '自主檢核 → 報竣審核 → 結案審核'),
('ASSET_CHANGE',       '資產異動審核', '異動申請 → 審核 → 生效');

-- ============================================================
-- 03-2 流程步驟範本
-- ============================================================
CREATE TABLE workflow_steps_template (
    id                  BIGSERIAL       PRIMARY KEY,
    workflow_type       VARCHAR(50)     NOT NULL REFERENCES workflow_definitions(workflow_type),
    step_order          INT             NOT NULL,
    step_code           VARCHAR(50)     NOT NULL,
    step_name           VARCHAR(200)    NOT NULL,
    required_role       VARCHAR(50),
    auto_action         VARCHAR(50),
    timeout_hours       INT,
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(workflow_type, step_order)
);

COMMENT ON COLUMN workflow_steps_template.auto_action IS
  '自動動作：AUTO_CREATE_REPAIR / AUTO_SYNC_ASSET / AUTO_DEDUCT_INVENTORY / NULL(人工)';

-- ============================================================
-- 03-3 流程實例（與具體工單綁定）
-- ============================================================
CREATE TABLE workflow_instances (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    workflow_type   VARCHAR(50)     NOT NULL REFERENCES workflow_definitions(workflow_type),
    ticket_type     VARCHAR(50)     NOT NULL,
    ticket_id       BIGINT          NOT NULL,
    current_step    VARCHAR(50)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    assigned_to     BIGINT,
    started_at      TIMESTAMP       NOT NULL DEFAULT now(),
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_wf_instances_tenant ON workflow_instances(tenant_id);
CREATE INDEX idx_wf_instances_ticket ON workflow_instances(ticket_type, ticket_id);
CREATE INDEX idx_wf_instances_assigned ON workflow_instances(assigned_to, status);
CREATE INDEX idx_wf_instances_status ON workflow_instances(tenant_id, status);

COMMENT ON TABLE workflow_instances IS '流程實例：每張工單建立時綁定一個流程實例';
COMMENT ON COLUMN workflow_instances.ticket_type IS 'FAULT_TICKET / REPAIR_TICKET / REPLACEMENT_ORDER / ASSET_CHANGE';
COMMENT ON COLUMN workflow_instances.status IS 'ACTIVE / COMPLETED / CANCELLED';

-- ============================================================
-- 03-4 流程步驟操作歷程
-- ============================================================
CREATE TABLE workflow_step_logs (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    instance_id         BIGINT          NOT NULL REFERENCES workflow_instances(id),
    step_code           VARCHAR(50)     NOT NULL,
    action              VARCHAR(30)     NOT NULL,
    actor_id            BIGINT          NOT NULL,
    actor_name          VARCHAR(100),
    original_assignee_id BIGINT,
    is_delegated        BOOLEAN         NOT NULL DEFAULT false,
    comment             TEXT,
    attachments         JSONB           DEFAULT '[]',
    before_snapshot     JSONB,
    after_snapshot      JSONB,
    acted_at            TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_wf_step_logs_instance ON workflow_step_logs(instance_id);
CREATE INDEX idx_wf_step_logs_actor ON workflow_step_logs(actor_id);

COMMENT ON TABLE workflow_step_logs IS '每次審核操作的完整歷程';
COMMENT ON COLUMN workflow_step_logs.action IS
  'SUBMIT / APPROVE / REJECT / RETURN / DISPATCH / MERGE / COMPLETE / CANCEL';
COMMENT ON COLUMN workflow_step_logs.original_assignee_id IS
  '代理簽核時，記錄原始被代理人（主管）的 user_id；非代理時為 NULL';
COMMENT ON COLUMN workflow_step_logs.is_delegated IS
  '是否為代理簽核，true 時 comment 自動前綴 [代理簽核]';
COMMENT ON COLUMN workflow_step_logs.attachments IS
  '[{"url":"...","desc":"維修後","gps_lat":25.04,"gps_lng":121.52,"taken_at":"..."}]';

-- ============================================================
-- 03-5 代理人設定
-- ============================================================
CREATE TABLE delegate_settings (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    delegator_id    BIGINT          NOT NULL,
    delegate_id     BIGINT          NOT NULL,
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    reason          VARCHAR(200),
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT chk_delegate_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_delegate_not_self CHECK (delegator_id != delegate_id)
);

CREATE INDEX idx_delegate_tenant ON delegate_settings(tenant_id);
CREATE INDEX idx_delegate_active ON delegate_settings(delegate_id, is_active)
    WHERE is_active = true;
CREATE INDEX idx_delegate_delegator ON delegate_settings(delegator_id, start_date, end_date);

COMMENT ON TABLE delegate_settings IS '代理人設定：代理期間待辦自動轉派，end_date 必填且不可為 NULL（禁止無限期代理）';
COMMENT ON COLUMN delegate_settings.reason IS '代理原因（如：出差、休假）';
```

#### 04 契約管理（新增）

```sql
-- ============================================================
-- 04 契約資訊
-- ============================================================
CREATE TABLE contracts (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    contract_code       VARCHAR(100)    NOT NULL,
    contract_name       VARCHAR(300)    NOT NULL,
    budget_year         INT,
    procurement_number  VARCHAR(100),
    contractor_name     VARCHAR(200),
    contractor_contact  VARCHAR(200),
    asset_category      VARCHAR(50),
    quantity            INT,
    start_date          DATE,
    end_date            DATE,
    acceptance_date     DATE,
    warranty_years      INT,
    warranty_expiry     DATE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    attributes          JSONB           DEFAULT '{}',
    created_by          VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, contract_code)
);

CREATE INDEX idx_contracts_tenant ON contracts(tenant_id);
CREATE INDEX idx_contracts_status ON contracts(tenant_id, status);

COMMENT ON TABLE contracts IS '契約資訊：預算年度、廠商、保固期限等';
COMMENT ON COLUMN contracts.status IS 'ACTIVE / EXPIRED / TERMINATED';
```

#### 05 報修維護

```sql
-- ============================================================
-- 05-1 報修維護工單
-- ============================================================
CREATE TABLE repair_tickets (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    ticket_number       VARCHAR(50)     NOT NULL,
    fault_ticket_id     BIGINT          REFERENCES fault_tickets(id),
    device_id           BIGINT          REFERENCES devices(id),
    circuit_id          BIGINT          REFERENCES circuits(id),
    contract_id         BIGINT          REFERENCES contracts(id),

    -- 報修資訊
    source              VARCHAR(30)     NOT NULL,
    reporter_name       VARCHAR(100),
    reporter_phone      VARCHAR(50),
    reporter_email      VARCHAR(200),
    report_address      TEXT,
    report_description  TEXT,
    reported_at         TIMESTAMP       NOT NULL DEFAULT now(),

    -- 維護資訊
    fault_category      VARCHAR(50),
    fault_cause         VARCHAR(50),
    repair_description  TEXT,
    completed_at        TIMESTAMP,

    -- 狀態
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    priority            VARCHAR(10)     DEFAULT 'NORMAL',
    dept_id             BIGINT          REFERENCES dept_info(dept_id),

    -- 審計
    created_by          VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, ticket_number)
);

CREATE INDEX idx_repair_tickets_tenant ON repair_tickets(tenant_id);
CREATE INDEX idx_repair_tickets_fault ON repair_tickets(fault_ticket_id);
CREATE INDEX idx_repair_tickets_device ON repair_tickets(device_id);
CREATE INDEX idx_repair_tickets_status ON repair_tickets(tenant_id, status);
CREATE INDEX idx_repair_tickets_dept ON repair_tickets(dept_id);

COMMENT ON TABLE repair_tickets IS '報修維護工單：由障礙申告審核通過後建立，或外部系統(1999)直接立案';
COMMENT ON COLUMN repair_tickets.source IS
  'FAULT_TICKET(障礙轉立) / CITIZEN_WEB(民眾網頁) / EXTERNAL_1999(陳情系統) / PATROL(巡檢) / PHONE(電話)';
COMMENT ON COLUMN repair_tickets.status IS
  'PENDING(未收案) / ACCEPTED(已收案) / DISPATCHED(已派工) / IN_PROGRESS(處理中) / '
  'COMPLETION_REPORTED(完工回報) / PENDING_REVIEW(完工審核中) / RETURNED(退回補件) / '
  'TRANSFERRED(改分轉送) / TRACKING(追蹤中) / CLOSED(結案)';

-- ============================================================
-- 05-2 派工紀錄
-- ============================================================
CREATE TABLE repair_dispatches (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    repair_ticket_id    BIGINT          NOT NULL REFERENCES repair_tickets(id),
    contract_id         BIGINT          REFERENCES contracts(id),
    assigned_to         BIGINT,
    assigned_org        VARCHAR(200),
    dispatch_note       TEXT,
    dispatched_at       TIMESTAMP       NOT NULL DEFAULT now(),
    dispatched_by       BIGINT          NOT NULL,
    due_date            DATE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DISPATCHED',
    created_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_repair_dispatches_ticket ON repair_dispatches(repair_ticket_id);
CREATE INDEX idx_repair_dispatches_assigned ON repair_dispatches(assigned_to);

COMMENT ON TABLE repair_dispatches IS '派工紀錄：一張報修單可能多次派工（退回重派）';
COMMENT ON COLUMN repair_dispatches.dispatched_by IS '派工人 user_id，完工後由此人審核結案';

-- ============================================================
-- 05-3 工單附件（共用：fault / repair / replacement）
-- ============================================================
CREATE TABLE ticket_attachments (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    ticket_type     VARCHAR(30)     NOT NULL,
    ticket_id       BIGINT          NOT NULL,
    file_type       VARCHAR(20)     NOT NULL,
    file_url        VARCHAR(500)    NOT NULL,
    file_name       VARCHAR(300),
    file_size       BIGINT,
    description     VARCHAR(500),
    gps_lat         NUMERIC(10,7),
    gps_lng         NUMERIC(11,7),
    taken_at        TIMESTAMP,
    phase           VARCHAR(20),
    scan_status     VARCHAR(20)     DEFAULT 'PENDING',
    uploaded_by     VARCHAR(50),
    uploaded_at     TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_attachments_ticket ON ticket_attachments(ticket_type, ticket_id);

COMMENT ON TABLE ticket_attachments IS '工單附件：照片/影片/錄音/文件，跨工單類型共用';
COMMENT ON COLUMN ticket_attachments.ticket_type IS 'FAULT_TICKET / REPAIR_TICKET / REPLACEMENT_ORDER';
COMMENT ON COLUMN ticket_attachments.file_type IS 'PHOTO / VIDEO / AUDIO / DOCUMENT';
COMMENT ON COLUMN ticket_attachments.phase IS 'BEFORE(維修前) / DURING(維修中) / AFTER(維修後) / REPORT(報修時)';
COMMENT ON COLUMN ticket_attachments.scan_status IS 'PENDING / CLEAN / INFECTED（病毒掃描結果）';

-- ============================================================
-- 05-4 巡查任務
-- ============================================================
CREATE TABLE inspection_tasks (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    task_name       VARCHAR(200)    NOT NULL,
    task_type       VARCHAR(20)     NOT NULL,
    schedule_cron   VARCHAR(100),
    start_date      DATE,
    end_date        DATE,
    area_scope      JSONB           DEFAULT '{}',
    dept_id         BIGINT          REFERENCES dept_info(dept_id),
    assigned_to     BIGINT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_inspection_tasks_tenant ON inspection_tasks(tenant_id);

COMMENT ON TABLE inspection_tasks IS '巡查任務：單次(ONE_TIME) / 定期(RECURRING)';
COMMENT ON COLUMN inspection_tasks.area_scope IS '巡查範圍：{deptIds:[], circuitIds:[], polygon:[...]}';

-- ============================================================
-- 05-5 巡查紀錄
-- ============================================================
CREATE TABLE inspection_records (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    task_id             BIGINT          NOT NULL REFERENCES inspection_tasks(id),
    inspector_id        BIGINT          NOT NULL,
    inspection_date     TIMESTAMP       NOT NULL DEFAULT now(),
    device_id           BIGINT          REFERENCES devices(id),
    result              VARCHAR(20)     NOT NULL,
    notes               TEXT,
    attachments         JSONB           DEFAULT '[]',
    fault_ticket_id     BIGINT          REFERENCES fault_tickets(id),
    created_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_inspection_records_task ON inspection_records(task_id);
CREATE INDEX idx_inspection_records_device ON inspection_records(device_id);

COMMENT ON TABLE inspection_records IS '巡查紀錄：每次巡查一筆';
COMMENT ON COLUMN inspection_records.result IS 'NORMAL / ABNORMAL / NEED_REPAIR';
```

#### 06 換裝維護

```sql
-- ============================================================
-- 06-1 換裝派工單
-- ============================================================
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

CREATE INDEX idx_replacement_orders_tenant ON replacement_orders(tenant_id);
CREATE INDEX idx_replacement_orders_repair ON replacement_orders(repair_ticket_id);
CREATE INDEX idx_replacement_orders_status ON replacement_orders(tenant_id, status);

COMMENT ON TABLE replacement_orders IS '換裝派工單：由報修維護觸發或獨立開立';
COMMENT ON COLUMN replacement_orders.order_type IS
  'NEW_INSTALL(新設) / REPLACEMENT(換裝) / RELOCATION(遷移) / '
  'DECOMMISSION(停用) / ADJUSTMENT(調整) / SHADE_INSTALL(加裝遮光罩)';
COMMENT ON COLUMN replacement_orders.status IS
  'DRAFT(草稿) / DISPATCHED(已派工) / IN_PROGRESS(施工中) / '
  'SELF_CHECKED(自主檢核) / PENDING_REVIEW(報竣審核) / '
  'RETURNED(退回補件) / CLOSED(結案)';

-- ============================================================
-- 06-2 換裝作業明細（逐設備）
-- ============================================================
CREATE TABLE replacement_items (
    id                      BIGSERIAL       PRIMARY KEY,
    tenant_id               VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    order_id                BIGINT          NOT NULL REFERENCES replacement_orders(id),
    device_id               BIGINT          NOT NULL REFERENCES devices(id),

    -- 換裝前
    before_device_type      VARCHAR(30),
    before_spec             JSONB           DEFAULT '{}',

    -- 換裝後
    after_device_type       VARCHAR(30),
    after_spec              JSONB           DEFAULT '{}',
    material_spec_id        BIGINT,
    approved_material_id    BIGINT,

    -- 結果
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    completed_at            TIMESTAMP,
    completed_by            VARCHAR(50),
    notes                   TEXT,
    created_at              TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_replacement_items_order ON replacement_items(order_id);
CREATE INDEX idx_replacement_items_device ON replacement_items(device_id);

COMMENT ON TABLE replacement_items IS '換裝明細：一張派工單可含多設備，每設備記錄換裝前後規格';
COMMENT ON COLUMN replacement_items.material_spec_id IS 'FK → material_specs.id（使用的材料規格）';
COMMENT ON COLUMN replacement_items.approved_material_id IS 'FK → approved_materials.id（使用的合格材料編號）';

-- ============================================================
-- 06-3 路燈號碼牌管理
-- ============================================================
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

CREATE INDEX idx_pole_numbers_device ON light_pole_numbers(device_id);

COMMENT ON TABLE light_pole_numbers IS '路燈號碼牌：編號 + QR Code 連結報修頁面';

-- ============================================================
-- 06-4 經審驗合格材料清單（06/07 共用）
-- ============================================================
CREATE TABLE approved_materials (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    material_spec_id    BIGINT          NOT NULL,
    contract_id         BIGINT          REFERENCES contracts(id),
    material_number     VARCHAR(100)    NOT NULL,
    approval_date       DATE            NOT NULL,
    batch_number        VARCHAR(100),
    brand               VARCHAR(200),
    model               VARCHAR(200),
    spec_details        JSONB           DEFAULT '{}',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, material_number)
);

CREATE INDEX idx_approved_materials_spec ON approved_materials(material_spec_id);
CREATE INDEX idx_approved_materials_contract ON approved_materials(contract_id);

COMMENT ON TABLE approved_materials IS '審驗合格材料：06-4 批次匯入，06-5 換裝時必須使用';
COMMENT ON COLUMN approved_materials.spec_details IS
  '燈具：{color_temp, lumen, voltage, current, wattage} / 控制器：{brand, model}';
```

#### 07 材料管理

```sql
-- ============================================================
-- 07-1 庫別
-- ============================================================
CREATE TABLE warehouses (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    warehouse_code  VARCHAR(50)     NOT NULL,
    warehouse_name  VARCHAR(200)    NOT NULL,
    location        VARCHAR(500),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, warehouse_code)
);

-- ============================================================
-- 07-2 材料規格
-- ============================================================
CREATE TABLE material_specs (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    spec_code       VARCHAR(100)    NOT NULL,
    spec_name       VARCHAR(300)    NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    unit            VARCHAR(20)     NOT NULL DEFAULT 'PCS',
    attributes      JSONB           DEFAULT '{}',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, spec_code)
);

COMMENT ON COLUMN material_specs.category IS
  'LUMINAIRE(燈具) / CONTROLLER(控制器) / POLE(燈桿) / POLE_NUMBER(號碼牌) / CABLE / OTHER';

-- ============================================================
-- 07-3 供應/承攬廠商
-- ============================================================
CREATE TABLE suppliers (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    supplier_code   VARCHAR(100)    NOT NULL,
    supplier_name   VARCHAR(300)    NOT NULL,
    contact_name    VARCHAR(100),
    contact_phone   VARCHAR(50),
    contact_email   VARCHAR(200),
    address         TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, supplier_code)
);

-- ============================================================
-- 07-4 採購單 + 明細
-- ============================================================
CREATE TABLE purchase_orders (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    po_number       VARCHAR(100)    NOT NULL,
    supplier_id     BIGINT          REFERENCES suppliers(id),
    contract_id     BIGINT          REFERENCES contracts(id),
    order_date      DATE            NOT NULL DEFAULT CURRENT_DATE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    total_amount    NUMERIC(12,2),
    notes           TEXT,
    created_by      VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, po_number)
);

CREATE TABLE purchase_items (
    id              BIGSERIAL       PRIMARY KEY,
    po_id           BIGINT          NOT NULL REFERENCES purchase_orders(id),
    material_spec_id BIGINT         NOT NULL REFERENCES material_specs(id),
    quantity        INT             NOT NULL,
    unit_price      NUMERIC(10,2),
    notes           TEXT
);

-- ============================================================
-- 07-5 收料紀錄
-- ============================================================
CREATE TABLE receiving_records (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    po_id           BIGINT          REFERENCES purchase_orders(id),
    warehouse_id    BIGINT          NOT NULL REFERENCES warehouses(id),
    material_spec_id BIGINT         NOT NULL REFERENCES material_specs(id),
    quantity        INT             NOT NULL,
    received_date   DATE            NOT NULL DEFAULT CURRENT_DATE,
    delivery_note   VARCHAR(200),
    received_by     VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT now()
);

-- ============================================================
-- 07-6 庫存
-- ============================================================
CREATE TABLE inventory (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    warehouse_id        BIGINT          NOT NULL REFERENCES warehouses(id),
    material_spec_id    BIGINT          NOT NULL REFERENCES material_specs(id),
    quantity_on_hand    INT             NOT NULL DEFAULT 0,
    safety_stock        INT             NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, warehouse_id, material_spec_id)
);

COMMENT ON COLUMN inventory.safety_stock IS '安全庫存量：低於此值觸發預警提示';

-- ============================================================
-- 07-7 庫存調整/盤點
-- ============================================================
CREATE TABLE inventory_adjustments (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    inventory_id    BIGINT          NOT NULL REFERENCES inventory(id),
    adjustment_type VARCHAR(20)     NOT NULL,
    quantity_change INT             NOT NULL,
    reason          TEXT,
    adjusted_by     VARCHAR(50),
    adjusted_at     TIMESTAMP       NOT NULL DEFAULT now()
);

COMMENT ON COLUMN inventory_adjustments.adjustment_type IS
  'COUNT(盤點) / TRANSFER(轉庫) / CORRECTION(修正) / DISPOSAL(報廢)';

-- ============================================================
-- 07-8 領料申請
-- ============================================================
CREATE TABLE issue_requests (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    request_number      VARCHAR(100)    NOT NULL,
    repair_ticket_id    BIGINT          REFERENCES repair_tickets(id),
    replacement_order_id BIGINT         REFERENCES replacement_orders(id),
    requested_by        VARCHAR(50)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, request_number)
);

COMMENT ON COLUMN issue_requests.status IS 'PENDING / APPROVED / ISSUED / REJECTED';

-- ============================================================
-- 07-9 出料紀錄
-- ============================================================
CREATE TABLE issue_records (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    request_id      BIGINT          NOT NULL REFERENCES issue_requests(id),
    inventory_id    BIGINT          NOT NULL REFERENCES inventory(id),
    material_spec_id BIGINT         NOT NULL REFERENCES material_specs(id),
    quantity        INT             NOT NULL,
    issued_by       VARCHAR(50),
    issued_at       TIMESTAMP       NOT NULL DEFAULT now()
);

-- ============================================================
-- 07-10 廢品處理
-- ============================================================
CREATE TABLE disposal_records (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    material_spec_id BIGINT         NOT NULL REFERENCES material_specs(id),
    quantity        INT             NOT NULL,
    disposal_type   VARCHAR(20)     NOT NULL,
    reason          TEXT,
    disposed_by     VARCHAR(50),
    disposed_at     TIMESTAMP       NOT NULL DEFAULT now()
);

COMMENT ON COLUMN disposal_records.disposal_type IS 'RETURN_WAREHOUSE(繳庫) / SCRAP(報廢)';
```

---

## 3. Workflow 狀態機

### 3-1 障礙申告流程（FAULT_REVIEW）

```
           ┌──────┐
           │ OPEN │ ← 民眾報修 / 巡檢通報 / 系統自動告警
           └──┬───┘
              │ 提交審核
              ↓
         ┌─────────┐
         │ REVIEW  │ ← 承辦人審核（03 workflow_instances.assigned_to）
         └──┬──┬───┘
            │  │
    通過 ───┘  └─── 駁回
    │                │
    ↓                ↓
┌──────────┐   ┌──────────┐
│ CONFIRMED│   │ REJECTED │  結束（誤報）
└──┬───────┘   └──────────┘
   │
   │ 自動動作：建立 repair_ticket
   ↓
┌────────────────┐
│ REPAIR_CREATED │  結束（工單轉到 05）
└────────────────┘

例外：關聯障礙合併
   REVIEW → MERGED（correlation_id 指向 fault_correlation）
```

**關鍵角色**：

| 步驟 | 執行者 | 說明 |
|------|--------|------|
| OPEN | 系統/報修人 | 建立申告 |
| REVIEW | 承辦人（OPERATOR / DEPT_ADMIN） | 審核是否為真實障礙 |
| CONFIRMED | 系統 | 審核通過，自動建立 repair_ticket |

### 3-2 報修派工 + 結案流程（REPAIR_DISPATCH + REPAIR_CLOSE）

```
                 ┌─────────┐
                 │ PENDING │ ← fault_ticket 審核通過自動建立 / 外部系統(1999)直接立案
                 └──┬──────┘
                    │ 受理收案
                    ↓
              ┌──────────┐
              │ ACCEPTED │
              └──┬───────┘
                 │ 派工（指定廠商/維護人員 + 設定 dispatched_by）
                 ↓
            ┌─────────────┐
            │ DISPATCHED  │ ← repair_dispatches 寫入
            └──┬──────────┘
               │ 廠商/外勤開始施工
               ↓
          ┌──────────────┐      需要換裝？ ──→ 建立 replacement_order (06)
          │ IN_PROGRESS  │ ←──────────────→ 需要領料？ ──→ 建立 issue_request (07)
          └──┬───────────┘
             │ 施工完成，外勤拍照回傳
             ↓
     ┌─────────────────────┐
     │ COMPLETION_REPORTED │ ← 外勤上傳 維修前/中/後 照片（含 GPS + 時間）
     └──┬──────────────────┘     workflow_step_logs.attachments 記錄
        │ 送審（回傳給 dispatched_by）
        ↓
    ┌────────────────┐
    │ PENDING_REVIEW │ ← 派工人（dispatched_by）收到待辦通知
    └──┬─────────┬───┘
       │         │
  通過 ┘         └ 退回
   │                │
   ↓                ↓
┌────────┐    ┌──────────┐
│ CLOSED │    │ RETURNED │
└────────┘    └──┬───────┘
                 │ 補件後重送
                 ↓
            回到 COMPLETION_REPORTED

特殊路徑：
  DISPATCHED → TRANSFERRED（改分/轉送其他單位）
  任意進行中 → TRACKING（追蹤中，暫無法結案）
```

**關鍵角色**：

| 步驟 | 執行者 | 說明 |
|------|--------|------|
| PENDING | 系統自動 | 工單建立 |
| ACCEPTED | 維護人員（OPERATOR） | 收案 |
| DISPATCHED | 承辦人（OPERATOR / DEPT_ADMIN） | 派工，**記錄 dispatched_by** |
| IN_PROGRESS | 外勤人員（FIELD_USER） | 施工中 |
| COMPLETION_REPORTED | 外勤人員（FIELD_USER） | **拍照 + GPS + 回傳** |
| PENDING_REVIEW | **dispatched_by**（當初派工的人） | **審核結案** |
| CLOSED | 系統 | 結案後觸發：資產更新 + 歷程記錄 |

### 3-3 換裝審核流程（REPLACEMENT_REVIEW）

```
      ┌────────┐
      │ DRAFT  │ ← 開立換裝派工單
      └──┬─────┘
         │ 派工
         ↓
   ┌─────────────┐
   │ DISPATCHED  │ ← 指派維護廠商
   └──┬──────────┘
      │ 施工（使用合格材料）
      ↓
  ┌──────────────┐
  │ IN_PROGRESS  │ ← 06-5 材料管控：必須選擇 approved_materials
  └──┬───────────┘     07 issue_records 扣庫
     │ 廠商自主檢核完成
     ↓
 ┌───────────────┐
 │ SELF_CHECKED  │ ← 06-9 廠商自主檢核，可預先更新資產
 └──┬────────────┘
    │ 報竣送審
    ↓
┌────────────────┐
│ PENDING_REVIEW │ ← 機關審核
└──┬─────────┬───┘
   │         │
  通過       退回
   │         │
   ↓         ↓
┌────────┐ ┌──────────┐
│ CLOSED │ │ RETURNED │ → 補件重送 → PENDING_REVIEW
└────────┘ └──────────┘

CLOSED 後觸發：
  → 04 devices 正式更新資產（覆蓋自主檢核的預更新）
  → 04 device_events 寫入 REPLACE 歷程
  → 07 inventory 確認扣庫
```

### 3-4 資產異動審核流程（ASSET_CHANGE）

```
  ┌────────┐
  │ DRAFT  │ ← 4-4 加帳/除帳/變更申請
  └──┬─────┘
     │ 送審
     ↓
┌────────────────┐
│ PENDING_REVIEW │ ← 機關審核（04-4 要求的應備文件）
└──┬─────────┬───┘
   │         │
  通過       退回
   │         │
   ↓         ↓
┌──────────┐ ┌──────────┐
│ APPROVED │ │ RETURNED │ → 補件重送
└──┬───────┘ └──────────┘
   │
   │ 自動：更新 devices 表
   ↓
┌──────────┐
│ APPLIED  │  結束（資產已變更）
└──────────┘
```

---

## 4. 跨模組事件流

### 4-1 事件總覽

| # | 觸發事件 | 來源模組 | 目標模組 | 自動動作 |
|---|----------|----------|----------|----------|
| E1 | 障礙申告審核通過 | 04 | 05+04 | 自動建立 repair_ticket + **devices.status → REPORTED** |
| E2 | 關聯障礙偵測 | 04 | 04 | 合併工單 → fault_correlation，推播通知 |
| E3 | Gateway 離線告警 | 04 | 04→05 | 建立 AUTO_ALERT fault_ticket → E1 |
| E4 | 報修派工 | 05 | 03+04 | 建立 workflow_instance(REPAIR_DISPATCH) + **devices.status → UNDER_REPAIR** |
| E5 | 施工需換裝 | 05 | 06 | 建立 replacement_order（關聯 repair_ticket_id） |
| E6 | 換裝需領料 | 06 | 07 | 建立 issue_request（關聯 replacement_order_id） |
| E7 | 領料審核通過 | 07 | 07 | inventory 扣庫，issue_records 寫入 |
| E8 | 外勤完工回傳 | 05 | 03 | workflow_step_logs 寫入（含照片附件），通知 dispatched_by |
| E9 | 結案審核通過 | 03 | 04 | **devices.status → ACTIVE** + device_events 歷程 |
| E10 | 換裝結案審核通過 | 03 | 04+07 | devices 正式更新 + inventory 確認扣庫 |
| E11 | 廠商自主檢核 | 06 | 04 | devices **預更新**（標記 provisional） |
| E12 | 庫存低於安全量 | 07 | 02(通知) | 推播安全庫存預警通知 |
| E13 | 巡檢發現異常 | 05 | 04 | 建立 fault_ticket（source=PATROL） |

### 4-2 事件序列圖（最完整路徑）

```
外勤人員   民眾    系統      04障礙   03簽核   05報修   06換裝   07材料   04資產
  │        │       │         │        │        │        │        │        │
  │        │  報修  │         │        │        │        │        │        │
  │        ├──────→│  建立    │        │        │        │        │        │
  │        │       ├────────→│        │        │        │        │        │
  │        │       │  fault  │        │        │        │        │        │
  │        │       │  ticket │        │        │        │        │        │
  │        │       │         │ E1:審核 │        │        │        │        │
  │        │       │         ├───────→│        │        │        │        │
  │        │       │         │  通過   │ 建repair│        │        │        │
  │        │       │         │        ├───────→│        │        │        │
  │        │       │         │        │ E4:派工 │        │        │        │
  │        │       │         │        │←───────┤        │        │        │
  │        │       │         │        │        │ E5:需換裝│        │        │
  │  收到派工通知   │         │        │        ├───────→│        │        │
  │←───────┤       │         │        │        │        │ E6:領料 │        │
  │        │       │         │        │        │        ├───────→│        │
  │        │       │         │        │        │        │  E7:扣庫│        │
  │        │       │         │        │        │        │←───────┤        │
  │  施工   │       │         │        │        │        │        │        │
  │──────→ │       │         │        │        │        │        │        │
  │  拍照回傳│      │         │        │        │        │        │        │
  ├────────────────────────────────→E8│        │        │        │        │
  │        │       │         │        │        │        │        │        │
  │        │       │    派工人審核結案   │        │        │        │        │
  │        │       │         │  E9    │        │        │ E10    │        │
  │        │       │         │←───────┤        │        │        │  更新   │
  │        │       │         │        ├────────────────────────────────→ │
  │        │       │         │        │        │        │        │ devices│
```

### 4-3 結案自動同步資產邏輯（E9/E10）

對應需求 05-12：「完工送審後自動同步更新資產資訊」

```java
// 觸發時機：03 workflow REPAIR_CLOSE 審核通過
// 或：03 workflow REPLACEMENT_REVIEW 結案審核通過

void onTicketClosed(RepairTicket ticket) {
    // 1. 恢復設備狀態
    Device device = deviceRepo.findById(ticket.getDeviceId());
    device.setStatus(DeviceStatus.ACTIVE);  // 維修中 → 正常

    // 2. 若有換裝，更新設備規格
    if (ticket.hasReplacementOrder()) {
        for (ReplacementItem item : ticket.getReplacementItems()) {
            device.setAttributes(item.getAfterSpec());
            // 更新燈具瓦數、型號等
        }
    }

    // 3. 寫入 device_events 歷程
    DeviceEvent event = new DeviceEvent();
    event.setDeviceId(device.getId());
    event.setEventType(hasReplacement ? "REPLACE" : "REPAIR");
    event.setDescription(ticket.getRepairDescription());
    // attachments 從 workflow_step_logs 複製
    event.setAttachments(ticket.getCompletionAttachments());

    // 4. 確認材料扣庫（07）
    if (ticket.hasIssueRequests()) {
        inventoryService.confirmDeduction(ticket.getIssueRequests());
    }
}
```

**退回補件時的資產回滾**（05-12 要求）：

```
結案審核通過 → 資產已同步更新
     ↓
  發現問題，退回重做
     ↓
  before_snapshot（workflow_step_logs.before_snapshot）用於判斷：
  「送審人員可選擇是否再次同步」→ 前端 checkbox 讓送審人決定
     ↓
  若選擇同步：以新的 after_snapshot 再次覆蓋
  若不同步：保留當前資產狀態
```

---

## 5. 共用實體歸屬

### 5-1 擁有權定義

| 表 | 擁有模組 | 其他模組操作方式 |
|---|---|---|
| `devices` | **04** | 05/06 透過 E9/E10 事件間接更新（不直接寫入） |
| `device_events` | **04** | 05/06 結案時透過 DeviceEventService 寫入 |
| `circuits` | **04** | 05 查詢用（fault_ticket.circuit_id 自動回填） |
| `fault_tickets` | **04** | 05 只讀（repair_ticket.fault_ticket_id FK） |
| `contracts` | **04** | 05/06/07 查詢用 + FK 引用 |
| `repair_tickets` | **05** | 06 引用（replacement_order.repair_ticket_id FK） |
| `ticket_attachments` | **05** | 04/06 寫入（多態 ticket_type） |
| `replacement_orders` | **06** | 07 引用（issue_request.replacement_order_id FK） |
| `approved_materials` | **06** | 07 引用（material_spec_id FK） |
| `inventory` | **07** | 06 查詢庫存、05/06 結案時扣庫 |
| `workflow_*` | **03** | 04/05/06 透過 WorkflowService API 操作 |

### 5-2 Workflow Service 統一介面

所有模組透過 03 的 `WorkflowService` 操作簽核流程，不直接讀寫 workflow 表：

```java
public interface WorkflowService {

    /** 為工單建立流程實例 */
    WorkflowInstance createInstance(String workflowType, String ticketType,
                                    Long ticketId, Long assignedTo);

    /** 提交到下一步 */
    void submit(Long instanceId, Long actorId, String comment,
                List<AttachmentDto> attachments);

    /** 審核通過 */
    void approve(Long instanceId, Long actorId, String comment);

    /** 審核駁回 */
    void reject(Long instanceId, Long actorId, String comment);

    /** 退回補件 */
    void returnForRevision(Long instanceId, Long actorId, String comment);

    /** 派工（指定下一步執行者） */
    void dispatch(Long instanceId, Long actorId, Long assigneeTo, String comment);

    /** 查詢待辦 */
    Page<WorkflowInstance> getMyPending(Long userId, Pageable pageable);

    /** 查詢某工單的完整流程歷程 */
    List<WorkflowStepLog> getHistory(String ticketType, Long ticketId);
}
```

---

## 6. 審計事件擴充

在現有 `AuditEventType` / `AuditCategory` 基礎上新增：

```java
// AuditCategory 新增
ASSET,          // 資產管理
MAINTENANCE,    // 報修維護
REPLACEMENT,    // 換裝維護
MATERIAL,       // 材料管理
WORKFLOW        // 簽核流程

// AuditEventType 新增
// -- 04 資產
CREATE_DEVICE, UPDATE_DEVICE, DELETE_DEVICE, EXPORT_DEVICE,
CREATE_CIRCUIT, UPDATE_CIRCUIT,
CREATE_CONTRACT, UPDATE_CONTRACT,
CREATE_FAULT_TICKET, UPDATE_FAULT_TICKET, MERGE_FAULT_TICKET,

// -- 05 報修
CREATE_REPAIR_TICKET, UPDATE_REPAIR_TICKET, CLOSE_REPAIR_TICKET,
DISPATCH_REPAIR, COMPLETE_REPAIR,

// -- 06 換裝
CREATE_REPLACEMENT_ORDER, UPDATE_REPLACEMENT_ORDER,
SELF_CHECK_REPLACEMENT, CLOSE_REPLACEMENT,

// -- 07 材料
CREATE_MATERIAL_SPEC, ISSUE_MATERIAL, RECEIVE_MATERIAL,
ADJUST_INVENTORY, DISPOSE_MATERIAL,

// -- 03 簽核
WORKFLOW_SUBMIT, WORKFLOW_APPROVE, WORKFLOW_REJECT,
WORKFLOW_RETURN, WORKFLOW_DISPATCH
```

---

## 7. ErrorCode 擴充

在現有 ErrorCode 基礎上新增（60xxx ~ 90xxx）：

```java
// 60xxx — 04 資產
DEVICE_NOT_FOUND(60001),
DEVICE_CODE_DUPLICATE(60002),
DEVICE_CIRCULAR_REFERENCE(60003),
DEVICE_HAS_CHILDREN(60004),
DEVICE_HAS_OPEN_FAULTS(60005),
CIRCUIT_NOT_FOUND(60010),
CIRCUIT_HAS_DEVICES(60011),
CONTRACT_NOT_FOUND(60020),
FAULT_TICKET_NOT_FOUND(60030),
FAULT_TICKET_ALREADY_MERGED(60031),

// 70xxx — 05 報修
REPAIR_TICKET_NOT_FOUND(70001),
REPAIR_TICKET_INVALID_STATUS(70002),
DISPATCH_NOT_FOUND(70010),
INSPECTION_TASK_NOT_FOUND(70020),

// 80xxx — 06 換裝
REPLACEMENT_ORDER_NOT_FOUND(80001),
REPLACEMENT_INVALID_STATUS(80002),
MATERIAL_NOT_APPROVED(80010),
MATERIAL_NOT_AVAILABLE(80011),
POLE_NUMBER_DUPLICATE(80020),

// 85xxx — 07 材料
MATERIAL_SPEC_NOT_FOUND(85001),
INSUFFICIENT_INVENTORY(85002),
WAREHOUSE_NOT_FOUND(85003),
SUPPLIER_NOT_FOUND(85004),

// 90xxx — 03 簽核
WORKFLOW_INSTANCE_NOT_FOUND(90001),
WORKFLOW_INVALID_TRANSITION(90002),
WORKFLOW_NOT_ASSIGNED_TO_USER(90003),
WORKFLOW_SELF_APPROVAL_NOT_ALLOWED(90004),
DELEGATE_PERIOD_OVERLAP(90010),
DELEGATE_SELF_NOT_ALLOWED(90011),
DELEGATE_END_DATE_REQUIRED(90012)
```

---

## 8. 權限與選單擴充

### 8-1 新增權限

```sql
-- 03 簽核
INSERT INTO permissions (permission_key, permission_name, description) VALUES
('WORKFLOW_VIEW',    '流程檢視', '查看工作流程狀態與歷程'),
('WORKFLOW_MANAGE',  '流程管理', '審核、退回、派工等操作'),
('DELEGATE_MANAGE',  '代理人管理', '設定/取消代理人');

-- 04 資產（已在 4-3 x-plan 定義：DEVICE_*, CIRCUIT_*, FAULT_*, DEVICE_EXPORT）
-- 新增：
INSERT INTO permissions (permission_key, permission_name, description) VALUES
('CONTRACT_VIEW',   '契約檢視', '查看契約資訊'),
('CONTRACT_MANAGE', '契約管理', '新增、編輯、刪除契約');

-- 05 報修
INSERT INTO permissions (permission_key, permission_name, description) VALUES
('REPAIR_VIEW',     '報修檢視', '查看報修工單'),
('REPAIR_MANAGE',   '報修管理', '立案、派工、結案'),
('REPAIR_DISPATCH', '報修派工', '派工給廠商/維護人員'),
('INSPECTION_VIEW', '巡查檢視', '查看巡查任務'),
('INSPECTION_MANAGE','巡查管理', '新增/編輯巡查任務');

-- 06 換裝
INSERT INTO permissions (permission_key, permission_name, description) VALUES
('REPLACEMENT_VIEW',   '換裝檢視', '查看換裝派工單'),
('REPLACEMENT_MANAGE', '換裝管理', '開立、編輯換裝派工單'),
('POLE_NUMBER_MANAGE', '號碼牌管理', '產出/重製路燈號碼牌');

-- 07 材料
INSERT INTO permissions (permission_key, permission_name, description) VALUES
('MATERIAL_VIEW',      '材料檢視', '查看材料規格與庫存'),
('MATERIAL_MANAGE',    '材料管理', '採購、收料、出料、盤點'),
('INVENTORY_VIEW',     '庫存檢視', '查看庫存總覽'),
('INVENTORY_MANAGE',   '庫存管理', '庫存調整、盤點');
```

### 8-2 選單結構

```
(已有) 35 資產管理
├── 36 設備管理           DEVICE_VIEW
├── 37 回路管理           CIRCUIT_VIEW
├── 38 障礙工單           FAULT_VIEW
├── 39 契約管理           CONTRACT_VIEW         (新增)

(新增) 40 報修維護
├── 41 報修工單           REPAIR_VIEW
├── 42 巡查管理           INSPECTION_VIEW

(新增) 43 換裝維護
├── 44 換裝派工           REPLACEMENT_VIEW
├── 45 號碼牌管理         POLE_NUMBER_MANAGE

(新增) 46 材料管理
├── 47 材料規格           MATERIAL_VIEW
├── 48 庫存管理           INVENTORY_VIEW
├── 49 採購管理           MATERIAL_MANAGE

(新增) 50 簽核中心
├── 51 待辦案件           WORKFLOW_VIEW
├── 52 代理人設定         DELEGATE_MANAGE
```

---

## 9. 實作優先序

### Phase 1：地基層（V30 ~ V32）

```
Step 1: V30 — 04 devices + circuits + device_events + contracts
        (已在 4-3 x-plan 定義的 DDL)
Step 2: V31 — 03 workflow_definitions + steps_template + instances + step_logs + delegate
Step 3: V32 — 04 fault_tickets + fault_correlations + menu/permission
```

**理由**：devices 是所有模組的基礎；workflow 是所有審核的基礎；fault_tickets 是生命週期的起點。

### Phase 2：報修派工（V33 ~ V34）

```
Step 4: V33 — 05 repair_tickets + repair_dispatches + ticket_attachments
Step 5: V34 — 05 inspection_tasks + inspection_records + menu/permission
```

**理由**：報修是最高頻的操作，也是連接障礙與換裝的橋樑。

### Phase 3：材料管理（V40 ~ V41）

```
Step 6: V40 — 07 warehouses + material_specs + suppliers + inventory + approved_materials
Step 7: V41 — 07 purchase_orders + purchase_items + receiving_records
                + issue_requests + issue_records + disposal_records
                + inventory_adjustments + menu/permission
```

### Phase 4：換裝維護（V42 ~ V43）

```
Step 8: V42 — 06 replacement_orders + replacement_items + light_pole_numbers
Step 9: V43 — 06 menu/permission + 跨模組 FK 補齊 (issue_requests)
```

### Phase 5：整合與閉環（V44）

```
Step 10: V44 — 跨模組 FK 約束補齊 + seed workflow_definitions + role_permissions
         + 結案自動同步資產觸發器 + 庫存安全量預警
```

### 後端 Package 結構

```
com.taipei.iot/
├── workflow/          ← 03 簽核引擎（跨模組共用）
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── repository/
│   └── service/
├── device/            ← 04 資產管理（已在 4-3 x-plan 定義）
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── repository/
│   └── service/
├── repair/            ← 05 報修維護
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── repository/
│   └── service/
├── replacement/       ← 06 換裝維護
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enums/
│   ├── repository/
│   └── service/
└── material/          ← 07 材料管理
    ├── controller/
    ├── dto/
    ├── entity/
    ├── enums/
    ├── repository/
    └── service/
```

---

## 10. 跨模組服務依賴圖

```
           ┌────────────────────────────────────┐
           │         WorkflowService (03)        │
           │  所有模組透過此 Service 操作簽核     │
           └────┬───────┬───────┬───────┬───────┘
                │       │       │       │
     ┌──────────┘       │       │       └──────────┐
     ↓                  ↓       ↓                  ↓
┌─────────────┐  ┌──────────┐  ┌──────────────┐  ┌──────────────┐
│FaultTicket  │  │RepairTicket│  │Replacement   │  │IssueRequest  │
│Service (04) │  │Service (05)│  │Service (06)  │  │Service (07)  │
└──────┬──────┘  └─────┬─────┘  └──────┬───────┘  └──────┬───────┘
       │               │               │                  │
       │       ┌───────┘               │                  │
       ↓       ↓                       ↓                  ↓
┌──────────────────┐           ┌──────────────┐   ┌──────────────┐
│DeviceService (04)│           │Approved       │   │InventoryService│
│DeviceEventService│           │MaterialService│   │  (07)        │
│CircuitService    │           │  (06)        │   └──────────────┘
│ContractService   │           └──────────────┘
└──────────────────┘

依賴方向（不可反向）：
  07 → 06 → 05 → 04
       03 被所有模組依賴（但 03 不依賴任何業務模組）
```

---

## 11. 技術選型：簽核引擎

### 11-1 候選方案

| | BPMN 引擎（Flowable / Camunda） | 自行開發（有限狀態機 FSM） |
|---|---|---|
| **定位** | 通用工作流引擎，支持 BPMN 2.0 完整規範 | 針對本專案需求的輕量狀態機 |
| **引入代價** | ~15MB JAR + 40 張 ACT_\* 表 + 引擎配置 | 4 張表 + ~500 行 Service 代碼 |
| **學習曲線** | 高 — BPMN 2.0 規範、引擎 API、流程設計器、部署機制 | 低 — 標準 Spring Service + enum 狀態轉換 |
| **擅長場景** | 並行閘道、會簽、動態子流程、業務人員拖拉畫流程 | 線性 + 簡單分支、流程固定或極少變更 |
| **多租戶支援** | **痛點** — ACT_\* 表不走 Hibernate Filter，需 hack 或 schema-per-tenant | 天然適配 — `TenantAware` + `@Filter("tenantFilter")` 一致 |
| **與現有架構** | 需橋接：Flowable 有自己的 User/Group 模型，要適配現有 RBAC | 直接用現有 `users` + `roles` + `permissions` |
| **流程可視化** | 內建 BPMN diagram 渲染 | Element Plus `<el-steps>` stepper 元件即可 |
| **維護成本** | 引擎升級、breaking changes、版本相容性 | 自己的代碼自己控制 |

### 11-2 流程複雜度評估

先盤點 5 種流程是否需要 BPMN 的進階能力：

| 流程 | 步驟數 | 並行閘道 | 會簽 | 動態路由 | 子流程 | 定時邊界事件 |
|------|--------|----------|------|----------|--------|-------------|
| FAULT_REVIEW | 3 | ✗ | ✗ | ✗ | ✗ | ✗ |
| REPAIR_DISPATCH | 6 | ✗ | ✗ | ✗ | ✗ | ✗ |
| REPAIR_CLOSE | 3 | ✗ | ✗ | ✗ | ✗ | ✗ |
| REPLACEMENT_REVIEW | 5 | ✗ | ✗ | ✗ | ✗ | ✗ |
| ASSET_CHANGE | 3 | ✗ | ✗ | ✗ | ✗ | ✗ |

**結論：全部都是線性流程 + 簡單分支（通過/退回），無任何 BPMN 進階特性需求。**

### 11-3 決策：自行開發（FSM 狀態機）

**選擇理由**：

1. **流程不需要 BPMN 的能力** — 並行閘道、會簽、動態路由、訊號事件一個都用不到
2. **多租戶是最大的坑** — 現有架構是 `@Filter("tenantFilter")` + shared schema，Flowable/Camunda 的 40+ 張 ACT_\* 表不走 Hibernate Filter，適配成本遠超自行開發
3. **與現有架構零摩擦** — 直接用 `TenantAware`、`TenantScopedRepository`、`@AuditEvent`、`DataScopeHelper` 等現有機制
4. **代碼量極小** — 核心狀態機 ~200 行，跨模組事件用 Spring `@EventListener` 解耦

### 11-4 實作架構

**核心：狀態轉換表 + Spring Event 解耦**

```java
@Service
public class WorkflowServiceImpl implements WorkflowService {

    // 狀態轉換規則（每種 workflow 一張靜態 Map）
    private static final Map<String, Map<String, Set<String>>> TRANSITIONS = Map.of(
        "FAULT_REVIEW", Map.of(
            "OPEN",       Set.of("REVIEW"),
            "REVIEW",     Set.of("CONFIRMED", "REJECTED", "MERGED")
        ),
        "REPAIR_CLOSE", Map.of(
            "COMPLETION_REPORTED", Set.of("PENDING_REVIEW"),
            "PENDING_REVIEW",     Set.of("CLOSED", "RETURNED"),
            "RETURNED",           Set.of("COMPLETION_REPORTED")
        )
        // ... 其他流程
    );

    @Transactional
    public void transition(Long instanceId, String targetStep,
                           Long actorId, String action, String comment,
                           List<AttachmentDto> attachments) {

        WorkflowInstance instance = repo.findById(instanceId)
            .orElseThrow(() -> new BusinessException(WORKFLOW_INSTANCE_NOT_FOUND));

        // 1. 驗證轉換合法性
        Set<String> allowed = TRANSITIONS
            .get(instance.getWorkflowType())
            .getOrDefault(instance.getCurrentStep(), Set.of());
        if (!allowed.contains(targetStep)) {
            throw new BusinessException(WORKFLOW_INVALID_TRANSITION);
        }

        // 2. 驗證操作權限（是否是 assigned_to 或其代理人）
        validateActorPermission(instance, actorId);

        // 3. 記錄操作歷程（含 before/after snapshot）
        WorkflowStepLog log = WorkflowStepLog.builder()
            .instanceId(instanceId)
            .stepCode(targetStep)
            .action(action)
            .actorId(actorId)
            .comment(comment)
            .attachments(attachments)
            .beforeSnapshot(captureSnapshot(instance))
            .build();
        stepLogRepo.save(log);

        // 4. 更新流程狀態
        instance.setCurrentStep(targetStep);
        if (isTerminalStep(instance.getWorkflowType(), targetStep)) {
            instance.setStatus("COMPLETED");
            instance.setCompletedAt(LocalDateTime.now());
        }
        repo.save(instance);

        // 5. 發布事件（跨模組自動動作由 @EventListener 處理）
        eventPublisher.publishEvent(
            new WorkflowTransitionEvent(instance, targetStep, action));
    }
}
```

**跨模組自動動作（Spring @EventListener 解耦）**：

```java
// 05 repair 模組 — 監聽障礙審核通過
@Component
public class FaultApprovedListener {

    @EventListener
    @Transactional
    public void onFaultConfirmed(WorkflowTransitionEvent event) {
        if ("FAULT_REVIEW".equals(event.getWorkflowType())
                && "CONFIRMED".equals(event.getTargetStep())) {
            // E1: 自動建立 repair_ticket
            repairTicketService.createFromFault(event.getTicketId());
        }
    }
}

// 04 device 模組 — 監聽結案審核通過
@Component
public class RepairClosedListener {

    @EventListener
    @Transactional
    public void onRepairClosed(WorkflowTransitionEvent event) {
        if ("REPAIR_CLOSE".equals(event.getWorkflowType())
                && "CLOSED".equals(event.getTargetStep())) {
            // E9: 資產同步更新 + 歷程記錄
            deviceSyncService.syncFromRepairTicket(event.getTicketId());
        }
    }
}
```

**前端可視化（Element Plus Steps）**：

```vue
<!-- 不需要 BPMN diagram renderer，stepper 元件即可 -->
<el-steps :active="currentStepIndex" finish-status="success" align-center>
  <el-step v-for="step in workflowHistory" :key="step.id"
    :title="step.stepName"
    :description="`${step.actorName} · ${formatDate(step.actedAt)}`"
    :status="getStepStatus(step)" />
</el-steps>
```

### 11-5 未來演進觸發點

若未來需求出現以下情境，再評估引入 BPMN 引擎：

| 觸發條件 | 說明 |
|----------|------|
| **業務人員自行設計流程** | 需拖拉式流程設計器，非工程師改 code |
| **會簽需求** | 多人同時審核，全部/多數通過才往下走 |
| **動態審核層級** | 根據金額/設備等級自動決定需幾層審核 |
| **跨租戶協作** | 不同租戶間的流程串接（如市府↔區處） |

在此之前，FSM 方案的 4 張表 + ~500 行核心代碼足以支撐所有需求。

---

## 附錄：決策記錄

| # | 決策 | 理由 |
|---|------|------|
| D1 | fault_ticket 與 repair_ticket **分兩張表** | 職責不同（偵測 vs 處理）；05-6 狀態比 04 多；05-2 需介接 1999 外部來源直接建 repair_ticket |
| D2 | device_events **統一歷程表** | 04 換裝歷程、05 報修歷程、06 換裝歷程全寫同一張，用 event_type 區分 |
| D3 | ticket_attachments **共用附件表** | 多態 FK（ticket_type + ticket_id），避免每個模組各建附件表 |
| D4 | workflow **統一簽核引擎** | 03 定義流程範本，所有模組透過 WorkflowService API 操作，不各自管狀態 |
| D5 | contracts 歸屬 **04** | 契約是資產的屬性維度，05/06/07 只是引用 |
| D6 | approved_materials 歸屬 **06/07 共用** | 06-4 匯入、06-5 管控使用、07 關聯材料規格 |
| D7 | 結案自動同步資產 | 05-12 明確要求；觸發點在 03 workflow 審核通過時 |
| D8 | 派工人審核結案 | 使用者描述的流程：派工人(dispatched_by) 收到外勤回傳後審核 |
| D9 | 雙路徑障礙偵測 | 電力（被動/報修驅動）+ 通訊（主動/心跳掃描）獨立偵測，已在 spec/04 D-5 詳述 |
| D10 | 實作優先序：04 → 03 → 05 → 06 → 07 | 依賴鏈決定；devices 是地基，workflow 是骨架 |
| D11 | 簽核引擎選擇**自行開發 FSM**，不用 BPMN | 5 種流程皆為線性 + 簡單分支；多租戶 `@Filter` 與 Flowable ACT_\* 表不相容；代碼量 ~500 行遠低於引擎適配成本 |
| D12 | **Graceful Degradation**：所有 FK / 拓撲 / 回路欄位 nullable | IoT 形態多元（電池/市電、SIM/Gateway），系統不假定所有欄位有值。有資料就啟用對應功能（回路偵測、GW 心跳、SIM 到期），沒資料就跳過，不影響核心工單流程。`circuits.panel_box_device_id` 改 nullable，UNIQUE 改為 `(tenant_id, circuit_number)`。詳見 4-3 x-plan D-6 |
