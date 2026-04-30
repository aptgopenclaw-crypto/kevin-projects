-- ============================================================
-- V46: 換裝維護 — 選單權限 + 角色綁定 + 系統設定
-- ============================================================

DO $$
DECLARE
    v_dir_id BIGINT;
    v_page1_id BIGINT;
    v_page2_id BIGINT;
BEGIN
    -- DIRECTORY
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, icon, sort_order, visible)
    VALUES (NULL, '換裝維護', 'DIRECTORY', 'ReplacementManagement', '/replacement', NULL, 'Repeat', 60, true)
    RETURNING menu_id INTO v_dir_id;

    -- PAGE: 換裝派工
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
    VALUES (v_dir_id, '換裝派工', 'PAGE', 'ReplacementOrder', 'orders', 'views/admin/replacement/ReplacementOrderView.vue', 'REPLACEMENT_VIEW', 'FileText', 10, true)
    RETURNING menu_id INTO v_page1_id;

    -- PAGE: 號碼牌管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
    VALUES (v_dir_id, '號碼牌管理', 'PAGE', 'PoleNumber', 'pole-numbers', 'views/admin/replacement/PoleNumberView.vue', 'POLE_NUMBER_MANAGE', 'QrCode', 20, true)
    RETURNING menu_id INTO v_page2_id;

    -- BUTTON permissions (non-visible)
    INSERT INTO menus (parent_id, name, menu_type, permission_code, sort_order, visible)
    VALUES
        (v_page1_id, '換裝派工管理', 'BUTTON', 'REPLACEMENT_MANAGE', 10, false),
        (v_page2_id, '號碼牌操作', 'BUTTON', 'POLE_NUMBER_MANAGE', 10, false);
END $$;

-- Permission entries
INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_REPLACEMENT_VIEW',    'REPLACEMENT_VIEW',    '換裝查看', '換裝維護', 1),
('PERM_REPLACEMENT_MANAGE',  'REPLACEMENT_MANAGE',  '換裝管理', '換裝維護', 2),
('PERM_POLE_NUMBER_MANAGE',  'POLE_NUMBER_MANAGE',  '號碼牌管理', '換裝維護', 3)
ON CONFLICT (permission_id) DO NOTHING;

-- Role binding: ADMIN + DEPT_ADMIN → all replacement permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE', 'POLE_NUMBER_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'DEPT_ADMIN' AND p.code IN ('REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE', 'POLE_NUMBER_MANAGE');

-- OPERATOR, DEPT_USER, FIELD_USER, VIEWER → view only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code IN ('OPERATOR', 'DEPT_USER', 'FIELD_USER', 'VIEWER') AND p.code = 'REPLACEMENT_VIEW';

-- Seed FRONTEND_BASE_URL 系統設定（號碼牌 QR Code 用）
INSERT INTO system_settings (tenant_id, setting_key, setting_value, description)
SELECT tenant_id, 'frontend_base_url', 'http://localhost:5173', '前端基礎 URL（號碼牌 QR Code 連結用）'
FROM tenant
ON CONFLICT (tenant_id, setting_key) DO NOTHING;
