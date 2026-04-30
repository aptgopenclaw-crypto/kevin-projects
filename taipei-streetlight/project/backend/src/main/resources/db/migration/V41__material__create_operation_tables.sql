-- =============================================================
-- V41: 材料管理 — 操作表 + 選單 + 權限
-- =============================================================

-- 07-4 採購單
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

CREATE INDEX idx_purchase_orders_tenant ON purchase_orders(tenant_id);

-- 07-4 採購明細
CREATE TABLE purchase_items (
    id               BIGSERIAL       PRIMARY KEY,
    po_id            BIGINT          NOT NULL REFERENCES purchase_orders(id),
    material_spec_id BIGINT          NOT NULL REFERENCES material_specs(id),
    quantity         INT             NOT NULL,
    unit_price       NUMERIC(10,2),
    notes            TEXT
);

-- 07-5 收料紀錄
CREATE TABLE receiving_records (
    id               BIGSERIAL       PRIMARY KEY,
    tenant_id        VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    po_id            BIGINT          REFERENCES purchase_orders(id),
    warehouse_id     BIGINT          NOT NULL REFERENCES warehouses(id),
    material_spec_id BIGINT          NOT NULL REFERENCES material_specs(id),
    quantity         INT             NOT NULL,
    received_date    DATE            NOT NULL DEFAULT CURRENT_DATE,
    delivery_note    VARCHAR(200),
    received_by      VARCHAR(50),
    created_at       TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_receiving_records_tenant ON receiving_records(tenant_id);
CREATE INDEX idx_receiving_records_po ON receiving_records(po_id);

-- 07-8 領料申請（replacement_order_id 暫無 FK，Phase 4 補）
CREATE TABLE issue_requests (
    id                   BIGSERIAL       PRIMARY KEY,
    tenant_id            VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    request_number       VARCHAR(100)    NOT NULL,
    repair_ticket_id     BIGINT          REFERENCES repair_tickets(id),
    replacement_order_id BIGINT,
    requested_by         VARCHAR(50)     NOT NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, request_number)
);

CREATE INDEX idx_issue_requests_tenant ON issue_requests(tenant_id);

COMMENT ON COLUMN issue_requests.status IS 'PENDING / APPROVED / ISSUED / REJECTED';

-- 07-9 出料紀錄
CREATE TABLE issue_records (
    id               BIGSERIAL       PRIMARY KEY,
    tenant_id        VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    request_id       BIGINT          NOT NULL REFERENCES issue_requests(id),
    inventory_id     BIGINT          NOT NULL REFERENCES inventory(id),
    material_spec_id BIGINT          NOT NULL REFERENCES material_specs(id),
    quantity         INT             NOT NULL,
    issued_by        VARCHAR(50),
    issued_at        TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_issue_records_request ON issue_records(request_id);

-- 07-7 庫存調整/盤點
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

CREATE INDEX idx_inventory_adjustments_tenant ON inventory_adjustments(tenant_id);

COMMENT ON COLUMN inventory_adjustments.adjustment_type IS
  'COUNT(盤點) / TRANSFER(轉庫) / CORRECTION(修正) / DISPOSAL(報廢)';

-- 07-10 廢品處理
CREATE TABLE disposal_records (
    id               BIGSERIAL       PRIMARY KEY,
    tenant_id        VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    material_spec_id BIGINT          NOT NULL REFERENCES material_specs(id),
    quantity         INT             NOT NULL,
    disposal_type    VARCHAR(20)     NOT NULL,
    reason           TEXT,
    disposed_by      VARCHAR(50),
    disposed_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_disposal_records_tenant ON disposal_records(tenant_id);

COMMENT ON COLUMN disposal_records.disposal_type IS 'RETURN_WAREHOUSE(繳庫) / SCRAP(報廢)';

-- ── 選單 + 權限 + 角色綁定 ────────────────────────────────

DO $$
DECLARE
    v_dir_id      BIGINT;
    v_spec_id     BIGINT;
    v_inv_id      BIGINT;
    v_po_id       BIGINT;
    v_approved_id BIGINT;
    v_wh_id       BIGINT;
    v_supplier_id BIGINT;
BEGIN
    -- 目錄
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (0, '材料管理', 'DIRECTORY', '/admin/material', NULL, NULL, 'PackageOutlined', 6, true, now(), now())
    RETURNING menu_id INTO v_dir_id;

    -- 材料規格
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (v_dir_id, '材料規格', 'MENU', '/admin/material/specs', 'admin/material/MaterialSpecView', 'MATERIAL_VIEW', NULL, 1, true, now(), now())
    RETURNING menu_id INTO v_spec_id;

    -- 庫存管理
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (v_dir_id, '庫存管理', 'MENU', '/admin/material/inventory', 'admin/material/InventoryView', 'INVENTORY_VIEW', NULL, 2, true, now(), now())
    RETURNING menu_id INTO v_inv_id;

    -- 採購管理
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (v_dir_id, '採購管理', 'MENU', '/admin/material/purchase-orders', 'admin/material/PurchaseOrderView', 'MATERIAL_VIEW', NULL, 3, true, now(), now())
    RETURNING menu_id INTO v_po_id;

    -- 合格材料
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (v_dir_id, '合格材料', 'MENU', '/admin/material/approved-materials', 'admin/material/ApprovedMaterialView', 'MATERIAL_VIEW', NULL, 4, true, now(), now())
    RETURNING menu_id INTO v_approved_id;

    -- 庫別管理
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (v_dir_id, '庫別管理', 'MENU', '/admin/material/warehouses', 'admin/material/WarehouseView', 'MATERIAL_VIEW', NULL, 5, true, now(), now())
    RETURNING menu_id INTO v_wh_id;

    -- 廠商管理
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (v_dir_id, '廠商管理', 'MENU', '/admin/material/suppliers', 'admin/material/SupplierView', 'MATERIAL_VIEW', NULL, 6, true, now(), now())
    RETURNING menu_id INTO v_supplier_id;

    -- 權限按鈕
    INSERT INTO menus (parent_id, name, menu_type, permission_code, sort_order, visible, create_time, update_time)
    VALUES
        (v_spec_id,     '查看材料規格', 'BUTTON', 'MATERIAL_VIEW',    1, false, now(), now()),
        (v_spec_id,     '管理材料規格', 'BUTTON', 'MATERIAL_MANAGE',  2, false, now(), now()),
        (v_inv_id,      '查看庫存',     'BUTTON', 'INVENTORY_VIEW',   1, false, now(), now()),
        (v_inv_id,      '管理庫存',     'BUTTON', 'INVENTORY_MANAGE', 2, false, now(), now());
END $$;

-- ── Permissions ──────────────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_MATERIAL_VIEW',    'MATERIAL_VIEW',    '檢視材料',   '材料管理', 1),
('PERM_MATERIAL_MANAGE',  'MATERIAL_MANAGE',  '管理材料',   '材料管理', 2),
('PERM_INVENTORY_VIEW',   'INVENTORY_VIEW',   '檢視庫存',   '材料管理', 3),
('PERM_INVENTORY_MANAGE', 'INVENTORY_MANAGE', '管理庫存',   '材料管理', 4)
ON CONFLICT (code) DO NOTHING;

-- ── Role binding: ADMIN + DEPT_ADMIN → 全部 ─────────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code IN (
    'MATERIAL_VIEW', 'MATERIAL_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'DEPT_ADMIN' AND p.code IN (
    'MATERIAL_VIEW', 'MATERIAL_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE'
)
ON CONFLICT DO NOTHING;

-- OPERATOR: 材料+庫存全部
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'OPERATOR' AND p.code IN (
    'MATERIAL_VIEW', 'MATERIAL_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE'
)
ON CONFLICT DO NOTHING;

-- FIELD_USER: 檢視
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'FIELD_USER' AND p.code IN (
    'MATERIAL_VIEW', 'INVENTORY_VIEW'
)
ON CONFLICT DO NOTHING;

-- VIEWER / MONITOR: 只有檢視
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code IN ('VIEWER', 'MONITOR') AND p.code IN (
    'MATERIAL_VIEW', 'INVENTORY_VIEW'
)
ON CONFLICT DO NOTHING;
