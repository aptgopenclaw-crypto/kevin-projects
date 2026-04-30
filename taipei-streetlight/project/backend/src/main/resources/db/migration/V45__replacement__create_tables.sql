-- ============================================================
-- V45: 換裝維護 — replacement_orders / replacement_items / light_pole_numbers
-- ============================================================

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
