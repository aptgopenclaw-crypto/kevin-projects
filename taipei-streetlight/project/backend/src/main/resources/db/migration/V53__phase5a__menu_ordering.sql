-- ============================================================
-- V53: Phase 5A — 選單排序統一 (Menu Ordering Normalization)
-- ============================================================
-- Reorder top-level directories to match business flow:
--   公告欄 → 資產管理 → 報修維護 → 換裝維護 → 材料管理
--   → 簽核管理 → GIS 地圖 → 用戶管理 → 系統管理 → 稽核中心

-- ── Top-level menu sort_order ──────────────────────────────

-- 公告欄 (standalone PAGE, not directory)
UPDATE menus SET sort_order = 5   WHERE menu_id = 34;    -- 公告欄

-- Core business modules
UPDATE menus SET sort_order = 10  WHERE menu_id = 35;    -- 資產管理
UPDATE menus SET sort_order = 20  WHERE menu_id = 59;    -- 報修維護
UPDATE menus SET sort_order = 30  WHERE menu_id = 88;    -- 換裝維護
UPDATE menus SET sort_order = 40  WHERE menu_id = 67;    -- 材料管理
UPDATE menus SET sort_order = 50  WHERE menu_id = 40;    -- 簽核管理
UPDATE menus SET sort_order = 60  WHERE menu_id = 93;    -- GIS 地圖

-- System administration
UPDATE menus SET sort_order = 70  WHERE menu_id = 1;     -- 用戶管理
UPDATE menus SET sort_order = 80  WHERE menu_id = 10;    -- 系統管理
UPDATE menus SET sort_order = 90  WHERE menu_id = 20;    -- 稽核中心
