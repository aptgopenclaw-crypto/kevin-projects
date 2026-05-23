-- ============================================================
-- V37: 招標管理 — 新增 AI 智慧查詢 選單 + 權限
-- Menu ID 44 = AI 智慧查詢 (PAGE, under 40)
-- Permission: tender:chat:use
-- ============================================================

-- ── 1. Menu ──────────────────────────────────────────────────

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(44, 40, 'AI 智慧查詢', 'PAGE', 'TenderAiChat', '/tender/ai-chat', 'views/tender/TenderAiChatView.vue', 'tender:chat:use', 'BrainCircuit', 64, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 44));

-- ── 2. Permission ─────────────────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_TENDER_CHAT_USE', 'tender:chat:use', '使用招標 AI 查詢', '招標管理', 115)
ON CONFLICT (code) DO NOTHING;

-- ── 3. Role bindings ─────────────────────────────────────────

-- ADMIN + OPERATOR + VIEWER: 皆可使用 AI 查詢
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code IN ('ADMIN', 'OPERATOR', 'VIEWER')
  AND p.code = 'tender:chat:use'
ON CONFLICT DO NOTHING;
