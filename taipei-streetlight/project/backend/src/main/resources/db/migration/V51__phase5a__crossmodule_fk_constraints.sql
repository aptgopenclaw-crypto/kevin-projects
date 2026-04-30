-- =============================================================
-- V51: Phase 5A — 跨模組 FK 約束補齊
-- =============================================================
-- WBS: 1.5.1.1
-- 目的: 補齊 Phase 1~4 各模組之間因建表先後而缺少的 FK 約束
-- =============================================================

-- ─── 1. device_events 新增跨模組追溯欄位 ───

-- 1a. 新增 repair_ticket_id 欄位（追溯哪一張報修工單觸發了設備事件）
ALTER TABLE device_events
    ADD COLUMN IF NOT EXISTS repair_ticket_id BIGINT;

ALTER TABLE device_events
    ADD CONSTRAINT fk_device_events_repair_ticket
    FOREIGN KEY (repair_ticket_id) REFERENCES repair_tickets(id);

CREATE INDEX IF NOT EXISTS idx_device_events_repair_ticket
    ON device_events(repair_ticket_id);

-- 1b. 新增 replacement_item_id 欄位（追溯哪一筆換裝明細觸發了設備事件）
ALTER TABLE device_events
    ADD COLUMN IF NOT EXISTS replacement_item_id BIGINT;

ALTER TABLE device_events
    ADD CONSTRAINT fk_device_events_replacement_item
    FOREIGN KEY (replacement_item_id) REFERENCES replacement_items(id);

CREATE INDEX IF NOT EXISTS idx_device_events_replacement_item
    ON device_events(replacement_item_id);

COMMENT ON COLUMN device_events.repair_ticket_id IS '關聯報修工單 ID（E9 結案時寫入）';
COMMENT ON COLUMN device_events.replacement_item_id IS '關聯換裝明細 ID（E10 結案時寫入）';

-- ─── 2. workflow_instances 加入檢查約束 ───

-- ticket_type 限制合法值（雖然是多型 FK 無法硬約束，但限定 enum 值）
ALTER TABLE workflow_instances
    DROP CONSTRAINT IF EXISTS chk_wf_ticket_type;

ALTER TABLE workflow_instances
    ADD CONSTRAINT chk_wf_ticket_type
    CHECK (ticket_type IN ('FAULT_TICKET', 'REPAIR_TICKET', 'REPLACEMENT_ORDER', 'ISSUE_REQUEST', 'ASSET_CHANGE'));

-- ─── 3. ticket_attachments 加入檢查約束 ───

-- 確認 ticket_attachments 的 ticket_type 合法
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'ticket_attachments'
    ) THEN
        EXECUTE 'ALTER TABLE ticket_attachments DROP CONSTRAINT IF EXISTS chk_attachment_ticket_type';
        EXECUTE 'ALTER TABLE ticket_attachments ADD CONSTRAINT chk_attachment_ticket_type
                  CHECK (ticket_type IN (''FAULT_TICKET'', ''REPAIR_TICKET'', ''REPLACEMENT_ORDER'', ''INSPECTION''))';
    END IF;
END $$;

-- ─── 4. 跨模組 NOT NULL 強化（確保資料完整性）───

-- repair_tickets.fault_ticket_id 允許 NULL（主動建單不經過障礙流程）
-- replacement_orders.repair_ticket_id 允許 NULL（獨立換裝建單）
-- 以上已在原始 migration 正確設定，不需變更

-- ─── 5. 補齊缺失的 index ───

-- fault_tickets: 常用查詢加 index
CREATE INDEX IF NOT EXISTS idx_fault_tickets_status
    ON fault_tickets(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_fault_tickets_device
    ON fault_tickets(device_id);

-- repair_tickets: 跨模組查詢
CREATE INDEX IF NOT EXISTS idx_repair_tickets_fault
    ON repair_tickets(fault_ticket_id);

CREATE INDEX IF NOT EXISTS idx_repair_tickets_status
    ON repair_tickets(tenant_id, status);

-- replacement_orders: 跨模組查詢
CREATE INDEX IF NOT EXISTS idx_replacement_orders_repair
    ON replacement_orders(repair_ticket_id);

CREATE INDEX IF NOT EXISTS idx_replacement_orders_status
    ON replacement_orders(tenant_id, status);

-- issue_requests: 跨模組查詢
CREATE INDEX IF NOT EXISTS idx_issue_requests_replacement
    ON issue_requests(replacement_order_id);

CREATE INDEX IF NOT EXISTS idx_issue_requests_repair
    ON issue_requests(repair_ticket_id);

-- workflow_instances: 跨模組查詢
CREATE INDEX IF NOT EXISTS idx_wf_instances_workflow_status
    ON workflow_instances(workflow_type, status);
