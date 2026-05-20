-- ============================================================
-- V40: 招標管理 — 新增廠商得標分析 Dashboard 選單
-- Menu ID 46 = 廠商分析 (PAGE, under 40)
-- Permission: 複用 tender:award:view（Controller 已使用此權限）
-- ============================================================

-- ── 1. Menu ──────────────────────────────────────────────────

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(46, 40, '廠商得標分析', 'PAGE', 'VendorDashboard', '/tender/vendor-dashboard', 'views/tender/VendorDashboardView.vue', 'tender:award:view', 'TrendCharts', 66, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 46));

-- ── 2. Role bindings ─────────────────────────────────────────
-- tender:award:view 已由 V39 綁定至 ADMIN / OPERATOR / VIEWER。
-- 本次無需額外 permission_id，Menu 可見性由前端根據 permission_code 控制。
-- 下方為防呆：確保三個角色都有此權限（若跨 tenant 部署時可能漏掉）

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code IN ('ADMIN', 'OPERATOR', 'VIEWER')
  AND p.code = 'tender:award:view'
ON CONFLICT DO NOTHING;
