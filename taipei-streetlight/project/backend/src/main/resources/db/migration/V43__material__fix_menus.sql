-- =============================================================
-- V43: 修正材料管理選單：parent_id / menu_type / component 格式
-- =============================================================

-- 1. 目錄 parent_id: 0 → NULL（與其他頂層目錄一致）
UPDATE menus SET parent_id = NULL WHERE name = '材料管理' AND menu_type = 'DIRECTORY' AND parent_id = 0;

-- 2. 子選單 menu_type: MENU → PAGE（與其他子頁面一致）
UPDATE menus SET menu_type = 'PAGE'
WHERE parent_id = (SELECT menu_id FROM menus WHERE name = '材料管理' AND menu_type = 'DIRECTORY')
  AND menu_type = 'MENU';

-- 3. component 路徑補齊 views/ 前綴 + .vue 後綴
UPDATE menus SET component = 'views/' || component || '.vue'
WHERE parent_id = (SELECT menu_id FROM menus WHERE name = '材料管理' AND menu_type = 'DIRECTORY')
  AND component IS NOT NULL
  AND component NOT LIKE 'views/%';
