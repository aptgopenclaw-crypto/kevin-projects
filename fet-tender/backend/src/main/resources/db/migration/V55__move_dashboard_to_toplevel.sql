-- ============================================================
-- V55: 將「招標總覽」從招標統計子選單移到主選單第一位
-- ============================================================

-- 移至頂層、排序第一
UPDATE menus
SET parent_id = NULL,
    sort_order = 1
WHERE menu_id = 51;

-- 恢復招標統計下原本的排序
UPDATE menus SET sort_order = 1 WHERE menu_id = 41; -- 招標公告
UPDATE menus SET sort_order = 2 WHERE menu_id = 44; -- AI 智慧查詢
