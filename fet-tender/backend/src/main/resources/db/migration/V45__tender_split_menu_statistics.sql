-- ============================================================
-- V45: 拆分招標管理選單 — 新增「招標統計」「決標統計」兩個主功能選單
--
-- 招標統計 (menu_id=48): 招標公告(41), AI 智慧查詢(44)
-- 決標統計 (menu_id=49): 決標公告(45), 廠商得標分析(46)
-- 招標管理 (menu_id=40): 搜尋關鍵字設定(42), 機關過濾設定(43), Solution 競品分析(47)
-- ============================================================

-- ── 1. 新增兩個 DIRECTORY 選單 ────────────────────────────────

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_path, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(48, NULL, '招標統計', 'DIRECTORY', '/tender-stats', 'DataLine', 61, true),
(49, NULL, '決標統計', 'DIRECTORY', '/award-stats',  'Medal',    62, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 49));

-- ── 2. 移動子選單到「招標統計」 ────────────────────────────────

-- 招標公告 → 招標統計
UPDATE menus SET parent_id = 48, sort_order = 1 WHERE menu_id = 41;

-- AI 智慧查詢 → 招標統計
UPDATE menus SET parent_id = 48, sort_order = 2 WHERE menu_id = 44;

-- ── 3. 移動子選單到「決標統計」 ────────────────────────────────

-- 決標公告 → 決標統計
UPDATE menus SET parent_id = 49, sort_order = 1 WHERE menu_id = 45;

-- 廠商得標分析 → 決標統計
UPDATE menus SET parent_id = 49, sort_order = 2 WHERE menu_id = 46;

-- ── 4. 調整招標管理(40)剩餘子選單排序 ────────────────────────

UPDATE menus SET sort_order = 1 WHERE menu_id = 42; -- 搜尋關鍵字設定
UPDATE menus SET sort_order = 2 WHERE menu_id = 43; -- 機關過濾設定
UPDATE menus SET sort_order = 3 WHERE menu_id = 47; -- Solution 競品分析

-- ── 5. 調整三個 DIRECTORY 的 sort_order ──────────────────────

UPDATE menus SET sort_order = 60 WHERE menu_id = 40; -- 招標管理
-- 48 (招標統計) = 61, 49 (決標統計) = 62 已在 INSERT 設定
