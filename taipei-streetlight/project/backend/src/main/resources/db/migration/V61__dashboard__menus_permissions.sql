-- ============================================================
-- V61: 儀表板 Dashboard — 選單 + 權限 + 角色綁定
-- ============================================================

-- ── 1. Permission entries ───────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_DASHBOARD_VIEW',   'DASHBOARD_VIEW',   '儀表板查看', '儀表板', 1),
('PERM_DASHBOARD_MANAGE', 'DASHBOARD_MANAGE', '儀表板管理', '儀表板', 2)
ON CONFLICT (permission_id) DO NOTHING;

-- ── 2. Menu entries ─────────────────────────────────────────

DO $$
DECLARE
    v_page_id BIGINT;
BEGIN
    -- PAGE: 儀表板 (頂層頁面，非 DIRECTORY)
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
    VALUES (NULL, '儀表板', 'PAGE', 'Dashboard', '/admin/dashboard', 'views/admin/dashboard/DashboardView.vue', 'DASHBOARD_VIEW', 'LayoutDashboard', 0, true)
    RETURNING menu_id INTO v_page_id;

    -- BUTTON: 版面管理 (需 DASHBOARD_MANAGE 權限)
    INSERT INTO menus (parent_id, name, menu_type, permission_code, sort_order, visible)
    VALUES (v_page_id, '版面管理', 'BUTTON', 'DASHBOARD_MANAGE', 10, false);
END $$;

-- ── 3. Role binding ─────────────────────────────────────────

-- 所有角色 → DASHBOARD_VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code IN ('ADMIN', 'DEPT_ADMIN', 'OPERATOR', 'VIEWER', 'MONITOR', 'FIELD_USER')
  AND p.code = 'DASHBOARD_VIEW'
ON CONFLICT DO NOTHING;

-- ADMIN + DEPT_ADMIN → DASHBOARD_MANAGE (版面管理)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code IN ('ADMIN', 'DEPT_ADMIN')
  AND p.code = 'DASHBOARD_MANAGE'
ON CONFLICT DO NOTHING;
