-- ============================================================
-- V39: 招標管理 — 新增決標公告選單 + 權限 + 決標爬蟲手動觸發權限
-- Menu ID 45 = 決標公告 (PAGE, under 40)
-- Permission: tender:award:view, tender:award:delete, tender:award:scrape:run
-- ============================================================

-- ── 1. Menu ──────────────────────────────────────────────────

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(45, 40, '決標公告', 'PAGE', 'TenderAwards', '/tender/awards', 'views/tender/TenderAwardView.vue', 'tender:award:view', 'Trophy', 65, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 45));

-- ── 2. Permissions ───────────────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_TENDER_AWARD_VIEW',       'tender:award:view',       '瀏覽決標公告',     '招標管理', 130),
('PERM_TENDER_AWARD_DELETE',     'tender:award:delete',     '刪除決標公告',     '招標管理', 131),
('PERM_TENDER_AWARD_SCRAPE_RUN', 'tender:award:scrape:run', '手動觸發決標爬蟲', '招標管理', 132)
ON CONFLICT (code) DO NOTHING;

-- ── 3. Role bindings ─────────────────────────────────────────

-- ADMIN: 全部決標權限
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'tender:award:view',
      'tender:award:delete',
      'tender:award:scrape:run'
  )
ON CONFLICT DO NOTHING;

-- OPERATOR: 可瀏覽決標公告
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'OPERATOR'
  AND p.code = 'tender:award:view'
ON CONFLICT DO NOTHING;

-- VIEWER: 僅能瀏覽決標公告
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'VIEWER'
  AND p.code = 'tender:award:view'
ON CONFLICT DO NOTHING;
