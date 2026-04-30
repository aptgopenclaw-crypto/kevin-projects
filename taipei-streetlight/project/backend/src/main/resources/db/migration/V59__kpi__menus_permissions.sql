-- ============================================================
-- V59: KPI 績效管理 — 選單 + 權限 + 角色綁定
-- ============================================================

-- ── 1. Permission entries ───────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_KPI_VIEW',            'KPI_VIEW',            'KPI 查看',     '績效管理', 1),
('PERM_KPI_MANAGE',          'KPI_MANAGE',          'KPI 管理',     '績效管理', 2),
('PERM_KPI_LOCK',            'KPI_LOCK',            'KPI 鎖定',     '績效管理', 3),
('PERM_KPI_UNLOCK',          'KPI_UNLOCK',          'KPI 解鎖',     '績效管理', 4),
('PERM_KPI_CONTRACTOR_VIEW', 'KPI_CONTRACTOR_VIEW', 'KPI 廠商查看', '績效管理', 5)
ON CONFLICT (permission_id) DO NOTHING;

-- ── 2. Menu entries ─────────────────────────────────────────

DO $$
DECLARE
    v_dir_id  BIGINT;
    v_page_id BIGINT;
BEGIN
    -- DIRECTORY: 績效管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, icon, sort_order, visible)
    VALUES (NULL, '績效管理', 'DIRECTORY', 'KpiManagement', '/admin/kpi', NULL, 'DataAnalysis', 4, true)
    RETURNING menu_id INTO v_dir_id;

    -- PAGE 1: KPI 指標管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '指標管理', 'PAGE', 'KpiIndicators', '/admin/kpi/indicators', 'views/admin/kpi/KpiIndicatorView.vue', 'KPI_MANAGE', 10, true);

    -- PAGE 2: 績效數據
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '績效數據', 'PAGE', 'KpiData', '/admin/kpi/data', 'views/admin/kpi/KpiDataView.vue', 'KPI_VIEW', 20, true);

    -- PAGE 3: 績效計算
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '績效計算', 'PAGE', 'KpiCalculate', '/admin/kpi/calculate', 'views/admin/kpi/KpiCalculateView.vue', 'KPI_VIEW', 30, true);

    -- PAGE 4: 績效報表
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '績效報表', 'PAGE', 'KpiReports', '/admin/kpi/reports', 'views/admin/kpi/KpiReportView.vue', 'KPI_VIEW', 40, true);

    -- PAGE 5: 期間管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '期間管理', 'PAGE', 'KpiPeriods', '/admin/kpi/periods', 'views/admin/kpi/KpiPeriodView.vue', 'KPI_LOCK', 50, true)
    RETURNING menu_id INTO v_page_id;

    -- BUTTON: 解鎖按鈕 (需 KPI_UNLOCK 權限)
    INSERT INTO menus (parent_id, name, menu_type, permission_code, sort_order, visible)
    VALUES (v_page_id, 'KPI 解鎖', 'BUTTON', 'KPI_UNLOCK', 10, false);
END $$;

-- ── 3. Role binding ─────────────────────────────────────────

-- ADMIN + DEPT_ADMIN → full KPI access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code IN ('ADMIN', 'DEPT_ADMIN')
  AND p.code IN ('KPI_VIEW', 'KPI_MANAGE', 'KPI_LOCK', 'KPI_UNLOCK')
ON CONFLICT DO NOTHING;

-- OPERATOR → view + manage + lock (no unlock)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'OPERATOR'
  AND p.code IN ('KPI_VIEW', 'KPI_MANAGE', 'KPI_LOCK')
ON CONFLICT DO NOTHING;

-- VIEWER, MONITOR → view only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code IN ('VIEWER', 'MONITOR')
  AND p.code = 'KPI_VIEW'
ON CONFLICT DO NOTHING;

-- FIELD_USER (廠商外包角色) → contractor view
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'FIELD_USER'
  AND p.code = 'KPI_CONTRACTOR_VIEW'
ON CONFLICT DO NOTHING;
