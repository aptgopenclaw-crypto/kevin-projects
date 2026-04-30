-- =============================================
-- V47: Fix replacement menu route_path (relative → absolute)
-- =============================================

-- Fix directory route_path
UPDATE menus
SET route_path = '/admin/replacement'
WHERE name = '換裝維護' AND menu_type = 'DIRECTORY';

-- Fix 換裝派工 page
UPDATE menus
SET route_path = '/admin/replacement/orders'
WHERE name = '換裝派工' AND menu_type = 'PAGE';

-- Fix 號碼牌管理 page
UPDATE menus
SET route_path = '/admin/replacement/pole-numbers'
WHERE name = '號碼牌管理' AND menu_type = 'PAGE';
