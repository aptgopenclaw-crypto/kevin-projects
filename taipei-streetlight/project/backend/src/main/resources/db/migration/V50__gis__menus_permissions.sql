-- ============================================================
-- V50: GIS 地圖 — 選單權限
-- ============================================================

DO $$
DECLARE
    v_dir_id BIGINT;
    v_page_id BIGINT;
BEGIN
    -- DIRECTORY: GIS 管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, icon, sort_order, visible)
    VALUES (NULL, 'GIS 地圖', 'DIRECTORY', 'GisManagement', '/admin/gis', NULL, 'MapLocation', 3, true)
    RETURNING menu_id INTO v_dir_id;

    -- PAGE: GIS 地圖
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
    VALUES (v_dir_id, '設備地圖', 'PAGE', 'GisMap', '/admin/gis/map', 'views/admin/gis/GisMapView.vue', 'GIS_VIEW', 'MapLocation', 10, true)
    RETURNING menu_id INTO v_page_id;

    -- BUTTON: GIS 管理權限（未來匯出、編輯等）
    INSERT INTO menus (parent_id, name, menu_type, permission_code, sort_order, visible)
    VALUES (v_page_id, 'GIS 操作', 'BUTTON', 'GIS_MANAGE', 10, false);
END $$;

-- Permission entries
INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_GIS_VIEW',   'GIS_VIEW',   'GIS 查看', 'GIS 地圖', 1),
('PERM_GIS_MANAGE', 'GIS_MANAGE', 'GIS 管理', 'GIS 地圖', 2)
ON CONFLICT (permission_id) DO NOTHING;

-- Role binding: ADMIN + DEPT_ADMIN → all GIS permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('GIS_VIEW', 'GIS_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'DEPT_ADMIN' AND p.code IN ('GIS_VIEW', 'GIS_MANAGE');

-- OPERATOR, DEPT_USER, FIELD_USER, VIEWER → view only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code IN ('OPERATOR', 'DEPT_USER', 'FIELD_USER', 'VIEWER') AND p.code = 'GIS_VIEW';
