-- =============================================================
-- V40: 材料管理 — 基礎表（庫別 + 材料規格 + 廠商 + 庫存 + 合格材料）
-- =============================================================

-- 07-1 庫別
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

CREATE INDEX idx_warehouses_tenant ON warehouses(tenant_id);

-- 07-2 材料規格
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

CREATE INDEX idx_material_specs_tenant ON material_specs(tenant_id);
CREATE INDEX idx_material_specs_category ON material_specs(tenant_id, category);

-- 07-3 廠商
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

CREATE INDEX idx_suppliers_tenant ON suppliers(tenant_id);

-- 07-6 庫存
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

CREATE INDEX idx_inventory_tenant ON inventory(tenant_id);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_id);

COMMENT ON COLUMN inventory.safety_stock IS '安全庫存量：低於此值觸發預警提示';

-- 06-4 審驗合格材料
CREATE TABLE approved_materials (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    material_spec_id    BIGINT          NOT NULL REFERENCES material_specs(id),
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

CREATE INDEX idx_approved_materials_tenant ON approved_materials(tenant_id);
CREATE INDEX idx_approved_materials_spec ON approved_materials(material_spec_id);
