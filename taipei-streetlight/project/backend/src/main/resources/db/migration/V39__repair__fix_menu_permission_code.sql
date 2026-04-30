-- =============================================
-- V39: Set permission_code on repair/inspection page menus
--   V36 left permission_code NULL on the PAGE entries,
--   but MenuService.findByPermissionCodeInAndVisibleTrue()
--   requires a matching permission_code to include the menu.
-- =============================================

SET search_path TO ${flyway:defaultSchema}, public;

UPDATE menus
SET permission_code = 'REPAIR_VIEW'
WHERE name = '報修工單'
  AND route_path = '/admin/repair/tickets';

UPDATE menus
SET permission_code = 'INSPECTION_VIEW'
WHERE name = '巡查管理'
  AND route_path = '/admin/repair/inspection';
