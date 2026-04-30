-- =============================================
-- V30: 04 資產管理 — 設備 + 回路 + 契約 + 設備歷程 + 設備負責人
-- =============================================

-- 1. 契約（無 FK 依賴其他新表）
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

-- 2. 設備主表
CREATE TABLE devices (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    device_type         VARCHAR(30)     NOT NULL,
    device_code         VARCHAR(100)    NOT NULL,
    device_name         VARCHAR(200),

    -- 坐標（三套）
    twd97_x             NUMERIC(12,3),
    twd97_y             NUMERIC(12,3),
    lng                 NUMERIC(11,7),
    lat                 NUMERIC(10,7),
    elevation           NUMERIC(8,3),
    taipower_coord      VARCHAR(100),

    -- 組織歸屬
    dept_id             BIGINT          REFERENCES dept_info(dept_id),
    contract_id         BIGINT          REFERENCES contracts(id),
    property_owner      VARCHAR(200),

    -- 設備狀態
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    installed_at        DATE,
    decommissioned_at   DATE,

    -- 連線拓撲
    parent_device_id    BIGINT          REFERENCES devices(id),
    connectivity_type   VARCHAR(20)     DEFAULT 'NONE',
    network_config      JSONB           DEFAULT '{}',
    last_heartbeat_at   TIMESTAMP,

    -- 電力回路（後補 FK）
    circuit_id          BIGINT,

    -- 專有欄位（依 device_type 不同）
    attributes          JSONB           DEFAULT '{}',

    -- 審計
    created_by          VARCHAR(50),
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_devices_tenant ON devices(tenant_id);
CREATE INDEX idx_devices_type ON devices(tenant_id, device_type);
CREATE INDEX idx_devices_code ON devices(tenant_id, device_code);
CREATE INDEX idx_devices_parent ON devices(parent_device_id);
CREATE INDEX idx_devices_circuit ON devices(circuit_id);
CREATE INDEX idx_devices_status ON devices(tenant_id, status);
CREATE INDEX idx_devices_dept ON devices(dept_id);
CREATE INDEX idx_devices_coords ON devices(lng, lat);
CREATE INDEX idx_devices_attrs ON devices USING GIN (attributes);

COMMENT ON TABLE devices IS '設備主表：燈桿、燈具、分電箱、控制器、附掛物等';
COMMENT ON COLUMN devices.device_type IS 'POLE / LUMINAIRE / PANEL_BOX / CONTROLLER / POWER_EQUIPMENT / ATTACHMENT';
COMMENT ON COLUMN devices.connectivity_type IS 'NONE / DIRECT / GATEWAY';
COMMENT ON COLUMN devices.attributes IS '依 device_type 存放專有欄位（JSONB）';

-- 3. 電力回路
CREATE TABLE circuits (
    id                      BIGSERIAL       PRIMARY KEY,
    tenant_id               VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    panel_box_device_id     BIGINT          REFERENCES devices(id),
    circuit_number          VARCHAR(50)     NOT NULL,
    circuit_name            VARCHAR(200),
    taipower_account        VARCHAR(50),
    usage_type              VARCHAR(50),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT now(),

    UNIQUE(tenant_id, circuit_number)
);

CREATE INDEX idx_circuits_tenant ON circuits(tenant_id);
CREATE INDEX idx_circuits_panel ON circuits(panel_box_device_id);

COMMENT ON TABLE circuits IS '電力回路：一個分電箱可含多條回路，一條回路控制多盞燈';
COMMENT ON COLUMN circuits.panel_box_device_id IS 'Nullable：電池供電 IoT 或未知分電箱時為 NULL';

-- 4. 後補 FK: devices.circuit_id → circuits.id
ALTER TABLE devices ADD CONSTRAINT fk_devices_circuit
    FOREIGN KEY (circuit_id) REFERENCES circuits(id);

-- 5. 設備事件歷程
CREATE TABLE device_events (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    device_id       BIGINT          NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    event_type      VARCHAR(30)     NOT NULL,
    event_date      TIMESTAMP       NOT NULL DEFAULT now(),
    description     TEXT,
    attachments     JSONB           DEFAULT '[]',
    created_by      VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_device_events_device ON device_events(device_id);
CREATE INDEX idx_device_events_type ON device_events(device_id, event_type);

COMMENT ON TABLE device_events IS '設備事件歷程：INSTALL / REPLACE / REPAIR / INSPECT / ADOPT / DECOMMISSION / MATERIAL_CHANGE';
COMMENT ON COLUMN device_events.attachments IS 'JSON 陣列，存放照片 URL：[{"url":"...","desc":"..."}]';

-- 6. 設備負責人（操作權限層）
CREATE TABLE device_managers (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    device_id       BIGINT          NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    user_id         VARCHAR(50)     NOT NULL,
    assigned_at     TIMESTAMP       NOT NULL DEFAULT now(),
    assigned_by     VARCHAR(50),

    UNIQUE(device_id, user_id)
);

CREATE INDEX idx_device_managers_user ON device_managers(user_id);
CREATE INDEX idx_device_managers_device ON device_managers(device_id);

COMMENT ON TABLE device_managers IS '設備負責人：控制誰可對設備執行操作（報修/派工等）';
