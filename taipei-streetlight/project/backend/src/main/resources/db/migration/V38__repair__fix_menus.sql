-- =============================================
-- V38: Fix repair/inspection menu entries
--   - parent_id 0 → NULL for directory
--   - menu_type MENU → PAGE for pages
--   - Add missing route_name
--   - Fix component path (add views/ prefix + .vue suffix)
-- =============================================

SET search_path TO ${flyway:defaultSchema}, public;

-- Fix the directory: parent_id = 0 → NULL, ensure it's a DIRECTORY with route_name
UPDATE menus
SET parent_id  = NULL,
    route_name = 'RepairManagement',
    menu_type  = 'DIRECTORY'
WHERE name = '報修維護'
  AND menu_type = 'DIRECTORY';

-- Fix 報修工單 page
UPDATE menus
SET menu_type  = 'PAGE',
    route_name = 'RepairTicket',
    component  = 'views/admin/repair/RepairTicketView.vue'
WHERE name = '報修工單'
  AND route_path = '/admin/repair/tickets';

-- Fix 巡查管理 page
UPDATE menus
SET menu_type  = 'PAGE',
    route_name = 'InspectionTask',
    component  = 'views/admin/repair/InspectionView.vue'
WHERE name = '巡查管理'
  AND route_path = '/admin/repair/inspection';
