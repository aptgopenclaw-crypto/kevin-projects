-- =============================================================
-- V44: 補齊材料管理選單 route_name（前端路由需要）
-- =============================================================

UPDATE menus SET route_name = 'MaterialSpec'      WHERE route_path = '/admin/material/specs';
UPDATE menus SET route_name = 'Inventory'          WHERE route_path = '/admin/material/inventory';
UPDATE menus SET route_name = 'PurchaseOrder'      WHERE route_path = '/admin/material/purchase-orders';
UPDATE menus SET route_name = 'ApprovedMaterial'   WHERE route_path = '/admin/material/approved-materials';
UPDATE menus SET route_name = 'Warehouse'          WHERE route_path = '/admin/material/warehouses';
UPDATE menus SET route_name = 'Supplier'           WHERE route_path = '/admin/material/suppliers';
