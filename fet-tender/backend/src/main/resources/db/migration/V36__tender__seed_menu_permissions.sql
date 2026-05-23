-- ============================================================
-- V35: 招標公告模組 — Menu + Permissions + Role bindings
-- ============================================================
-- Menu ID 規劃：
--   40 = 招標管理 (DIRECTORY, top-level)
--   41 = 招標公告列表 (PAGE, under 40)
--   42 = 搜尋關鍵字設定 (PAGE, under 40)
--   43 = 機關過濾設定   (PAGE, under 40)
-- ============================================================

-- ── 1. Menus ─────────────────────────────────────────────────────────────────

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_path, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(40, NULL, '招標管理', 'DIRECTORY', '/tender', 'DocumentCopy', 60, true)
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(41, 40, '招標公告',         'PAGE', 'TenderAnnouncements',    '/tender/announcements',         'views/tender/TenderAnnouncementView.vue',    'tender:announcement:view', 'List',     61, true),
(42, 40, '搜尋關鍵字設定',   'PAGE', 'TenderSearchKeywords',   '/tender/search-keywords',       'views/tender/SearchKeywordView.vue',         'tender:config:view',       'Key',      62, true),
(43, 40, '機關過濾設定',     'PAGE', 'TenderAgencyFilters',    '/tender/agency-filters',        'views/tender/AgencyFilterView.vue',          'tender:config:view',       'Filter',   63, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 43));

-- ── 2. Permissions ───────────────────────────────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
-- 招標公告資料
('PERM_TENDER_ANN_VIEW',    'tender:announcement:view',   '瀏覽招標公告',     '招標管理', 100),
('PERM_TENDER_ANN_DELETE',  'tender:announcement:delete', '刪除招標公告',     '招標管理', 101),
-- 關鍵字 / 機關過濾設定
('PERM_TENDER_CFG_VIEW',    'tender:config:view',         '瀏覽招標搜尋設定', '招標管理', 110),
('PERM_TENDER_CFG_EDIT',    'tender:config:edit',         '編輯招標搜尋設定', '招標管理', 111),
-- 手動觸發爬蟲
('PERM_TENDER_SCRAPE_RUN',  'tender:scrape:run',          '手動觸發招標爬蟲', '招標管理', 120)
ON CONFLICT (code) DO NOTHING;

-- ── 3. Role bindings ─────────────────────────────────────────────────────────

-- ADMIN: 全部招標權限
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
      'tender:announcement:view',
      'tender:announcement:delete',
      'tender:config:view',
      'tender:config:edit',
      'tender:scrape:run'
  )
ON CONFLICT DO NOTHING;

-- OPERATOR: 可瀏覽招標公告 + 瀏覽設定
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'OPERATOR'
  AND p.code IN (
      'tender:announcement:view',
      'tender:config:view'
  )
ON CONFLICT DO NOTHING;

-- VIEWER: 僅能瀏覽招標公告
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'VIEWER'
  AND p.code IN ('tender:announcement:view')
ON CONFLICT DO NOTHING;
