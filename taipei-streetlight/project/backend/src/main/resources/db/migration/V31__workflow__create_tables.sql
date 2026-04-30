-- =============================================
-- V31: 03 簽核引擎 — 流程定義 + 步驟範本 + 流程實例 + 操作歷程 + 代理人
-- =============================================

-- 1. 流程定義（全域表，不含 tenant_id）
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

-- 2. 流程步驟範本（全域表）
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

-- 3. 流程實例（租戶隔離）
CREATE TABLE workflow_instances (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    workflow_type   VARCHAR(50)     NOT NULL REFERENCES workflow_definitions(workflow_type),
    ticket_type     VARCHAR(50)     NOT NULL,
    ticket_id       BIGINT          NOT NULL,
    current_step    VARCHAR(50)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    assigned_to     VARCHAR(50),
    creator_id      VARCHAR(50)     NOT NULL,
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
COMMENT ON COLUMN workflow_instances.creator_id IS '起案人 user_id，用於自審防護';

-- 4. 流程步驟操作歷程（租戶隔離）
CREATE TABLE workflow_step_logs (
    id                      BIGSERIAL       PRIMARY KEY,
    tenant_id               VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    instance_id             BIGINT          NOT NULL REFERENCES workflow_instances(id),
    step_code               VARCHAR(50)     NOT NULL,
    action                  VARCHAR(30)     NOT NULL,
    actor_id                VARCHAR(50)     NOT NULL,
    actor_name              VARCHAR(100),
    original_assignee_id    VARCHAR(50),
    is_delegated            BOOLEAN         NOT NULL DEFAULT false,
    comment                 TEXT,
    attachments             JSONB           DEFAULT '[]',
    before_snapshot         JSONB,
    after_snapshot          JSONB,
    acted_at                TIMESTAMP       NOT NULL DEFAULT now()
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

-- 5. 代理人設定（租戶隔離）
CREATE TABLE delegate_settings (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    delegator_id    VARCHAR(50)     NOT NULL,
    delegate_id     VARCHAR(50)     NOT NULL,
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

COMMENT ON TABLE delegate_settings IS '代理人設定：代理期間待辦自動轉派，end_date 必填（禁止無限期代理）';
COMMENT ON COLUMN delegate_settings.reason IS '代理原因（如：出差、休假）';

-- =============================================
-- Seed: 流程定義
-- =============================================
INSERT INTO workflow_definitions (workflow_type, workflow_name, description) VALUES
('FAULT_REVIEW',        '障礙申告審核',   '民眾報案 / 巡檢 / 系統告警 → 審核確認 → 自動建立報修單'),
('REPAIR_DISPATCH',     '報修派工流程',   '報修單收案 → 派工 → 施工 → 完工回報'),
('REPAIR_CLOSE',        '報修結案審核',   '完工回報 → 審核 → 結案（自動同步資產）'),
('REPLACEMENT_REVIEW',  '換裝報竣審核',   '派工 → 施工 → 自主檢核 → 報竣審核 → 結案'),
('ASSET_CHANGE',        '資產異動審核',   '草稿 → 主管審核 → 核准 → 生效（自動同步資產）')
ON CONFLICT (workflow_type) DO NOTHING;

-- =============================================
-- Seed: 流程步驟範本
-- =============================================

-- FAULT_REVIEW 流程
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('FAULT_REVIEW', 1, 'OPEN',      '申告建立',   NULL,       NULL),
('FAULT_REVIEW', 2, 'REVIEW',    '審核確認',   'OPERATOR', NULL),
('FAULT_REVIEW', 3, 'CONFIRMED', '確認通過',   NULL,       'AUTO_CREATE_REPAIR'),
('FAULT_REVIEW', 4, 'REJECTED',  '駁回(誤報)', NULL,       NULL),
('FAULT_REVIEW', 5, 'MERGED',    '合併(關聯)', NULL,       NULL)
ON CONFLICT (workflow_type, step_order) DO NOTHING;

-- REPAIR_DISPATCH 流程
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('REPAIR_DISPATCH', 1, 'PENDING',              '未收案',   NULL,         NULL),
('REPAIR_DISPATCH', 2, 'ACCEPTED',             '已收案',   'OPERATOR',   NULL),
('REPAIR_DISPATCH', 3, 'DISPATCHED',           '已派工',   'OPERATOR',   NULL),
('REPAIR_DISPATCH', 4, 'IN_PROGRESS',          '處理中',   'FIELD_USER', NULL),
('REPAIR_DISPATCH', 5, 'COMPLETION_REPORTED',  '完工回報', 'FIELD_USER', NULL),
('REPAIR_DISPATCH', 6, 'TRANSFERRED',          '改分轉送', 'OPERATOR',   NULL)
ON CONFLICT (workflow_type, step_order) DO NOTHING;

-- REPAIR_CLOSE 流程
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('REPAIR_CLOSE', 1, 'PENDING_REVIEW', '完工審核中', NULL, NULL),
('REPAIR_CLOSE', 2, 'CLOSED',         '結案',       NULL, 'AUTO_SYNC_ASSET'),
('REPAIR_CLOSE', 3, 'RETURNED',       '退回補件',   NULL, NULL)
ON CONFLICT (workflow_type, step_order) DO NOTHING;

-- REPLACEMENT_REVIEW 流程
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('REPLACEMENT_REVIEW', 1, 'DRAFT',          '草稿',     NULL,         NULL),
('REPLACEMENT_REVIEW', 2, 'DISPATCHED',     '已派工',   'OPERATOR',   NULL),
('REPLACEMENT_REVIEW', 3, 'IN_PROGRESS',    '施工中',   'FIELD_USER', NULL),
('REPLACEMENT_REVIEW', 4, 'SELF_CHECKED',   '自主檢核', NULL,         NULL),
('REPLACEMENT_REVIEW', 5, 'PENDING_REVIEW', '報竣審核', 'OPERATOR',   NULL),
('REPLACEMENT_REVIEW', 6, 'CLOSED',         '結案',     NULL,         'AUTO_SYNC_ASSET'),
('REPLACEMENT_REVIEW', 7, 'RETURNED',       '退回補件', NULL,         NULL)
ON CONFLICT (workflow_type, step_order) DO NOTHING;

-- ASSET_CHANGE 流程
INSERT INTO workflow_steps_template (workflow_type, step_order, step_code, step_name, required_role, auto_action) VALUES
('ASSET_CHANGE', 1, 'DRAFT',          '草稿',     NULL,         NULL),
('ASSET_CHANGE', 2, 'PENDING_REVIEW', '審核中',   'DEPT_ADMIN', NULL),
('ASSET_CHANGE', 3, 'APPROVED',       '核准',     NULL,         NULL),
('ASSET_CHANGE', 4, 'APPLIED',        '已生效',   NULL,         'AUTO_SYNC_ASSET'),
('ASSET_CHANGE', 5, 'RETURNED',       '退回補件', NULL,         NULL)
ON CONFLICT (workflow_type, step_order) DO NOTHING;
