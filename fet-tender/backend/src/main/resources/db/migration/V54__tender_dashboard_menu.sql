-- ============================================================
-- V54: 新增「招標總覽」Dashboard — 主選單第一個位置 (sort_order=1)
-- Menu ID 51 = 招標總覽 (PAGE, top-level, sort_order=1 最前面)
-- ============================================================

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(51, NULL, '招標總覽', 'PAGE', 'TenderDashboard', '/tender/dashboard', 'views/tender/TenderDashboardView.vue', 'tender:announcement:view', 'DataBoard', 1, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 51));
