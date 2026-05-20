-- ============================================================
-- V41: 招標管理 — 新增 Solution 競品分析選單
-- Menu ID 47 = Solution 競品分析 (PAGE, under 40)
-- 所有角色皆可存取（no permission_code 限制，路由層不加 guard）
-- ============================================================

-- ── 1. Menu ──────────────────────────────────────────────────

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(47, 40, 'Solution 競品分析', 'PAGE', 'SolutionCompetitor', '/tender/solution-competitor', 'views/tender/SolutionCompetitorView.vue', NULL, 'DataAnalysis', 67, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 47));
