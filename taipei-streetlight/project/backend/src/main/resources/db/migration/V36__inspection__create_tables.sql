-- =============================================================
-- V36: 巡查管理 + 報修選單權限
-- =============================================================

-- 05-4 巡查任務
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

-- 05-5 巡查紀錄
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

-- ── 選單 + 權限 + 角色綁定 ────────────────────────────────

DO $$
DECLARE
    v_dir_id     BIGINT;
    v_ticket_id  BIGINT;
    v_inspect_id BIGINT;
BEGIN
    -- 目錄
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (0, '報修維護', 'DIRECTORY', '/admin/repair', NULL, NULL, 'ToolOutlined', 5, true, now(), now())
    RETURNING menu_id INTO v_dir_id;

    -- 報修工單
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (v_dir_id, '報修工單', 'MENU', '/admin/repair/tickets', 'admin/repair/RepairTicketView', NULL, NULL, 1, true, now(), now())
    RETURNING menu_id INTO v_ticket_id;

    -- 巡查管理
    INSERT INTO menus (parent_id, name, menu_type, route_path, component, permission_code, icon, sort_order, visible, create_time, update_time)
    VALUES (v_dir_id, '巡查管理', 'MENU', '/admin/repair/inspection', 'admin/repair/InspectionView', NULL, NULL, 2, true, now(), now())
    RETURNING menu_id INTO v_inspect_id;

    -- 權限按鈕
    INSERT INTO menus (parent_id, name, menu_type, permission_code, sort_order, visible, create_time, update_time)
    VALUES
        (v_ticket_id,  '查看報修工單', 'BUTTON', 'REPAIR_VIEW',       1, false, now(), now()),
        (v_ticket_id,  '管理報修工單', 'BUTTON', 'REPAIR_MANAGE',     2, false, now(), now()),
        (v_ticket_id,  '派工',         'BUTTON', 'REPAIR_DISPATCH',   3, false, now(), now()),
        (v_inspect_id, '查看巡查',     'BUTTON', 'INSPECTION_VIEW',   1, false, now(), now()),
        (v_inspect_id, '管理巡查',     'BUTTON', 'INSPECTION_MANAGE', 2, false, now(), now());
END $$;

-- ── Permissions ──────────────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_REPAIR_VIEW',       'REPAIR_VIEW',       '檢視報修工單', '報修維護', 1),
('PERM_REPAIR_MANAGE',     'REPAIR_MANAGE',     '管理報修工單', '報修維護', 2),
('PERM_REPAIR_DISPATCH',   'REPAIR_DISPATCH',   '派工',         '報修維護', 3),
('PERM_INSPECTION_VIEW',   'INSPECTION_VIEW',   '檢視巡查',     '報修維護', 4),
('PERM_INSPECTION_MANAGE', 'INSPECTION_MANAGE', '管理巡查',     '報修維護', 5)
ON CONFLICT (code) DO NOTHING;

-- ── Role binding: ADMIN + DEPT_ADMIN → 全部 ─────────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code IN (
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH', 'INSPECTION_VIEW', 'INSPECTION_MANAGE'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'DEPT_ADMIN' AND p.code IN (
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH', 'INSPECTION_VIEW', 'INSPECTION_MANAGE'
)
ON CONFLICT DO NOTHING;

-- OPERATOR: 報修全部
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'OPERATOR' AND p.code IN (
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH', 'INSPECTION_VIEW', 'INSPECTION_MANAGE'
)
ON CONFLICT DO NOTHING;

-- FIELD_USER: 報修檢視 + 巡查
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'FIELD_USER' AND p.code IN (
    'REPAIR_VIEW', 'INSPECTION_VIEW', 'INSPECTION_MANAGE'
)
ON CONFLICT DO NOTHING;

-- VIEWER: 只有檢視
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code IN ('VIEWER', 'MONITOR') AND p.code IN (
    'REPAIR_VIEW', 'INSPECTION_VIEW'
)
ON CONFLICT DO NOTHING;
