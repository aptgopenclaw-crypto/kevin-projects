-- ============================================================
-- V52: Phase 5A — 角色權限完整矩陣 (Full Role-Permission Matrix)
-- ============================================================
-- 1. Create missing WORKFLOW_MANAGE permission
-- 2. Delete orphaned legacy permissions (DEVICE_CREATE, DEVICE_UPDATE, USER_MANAGE)
-- 3. Rebuild role_permissions for all 7 non-SUPER_ADMIN roles
-- 4. Add ANNOUNCEMENT_VIEW to roles that should see announcements

-- ── Step 1: Create missing permissions ──────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_WORKFLOW_MANAGE', 'WORKFLOW_MANAGE', '簽核操作', '簽核管理', 2)
ON CONFLICT (permission_id) DO NOTHING;

-- ── Step 2: Clean up orphaned legacy permissions ────────────

-- Remove bindings first, then permissions
DELETE FROM role_permissions
WHERE permission_id IN ('PERM_DEVICE_CREATE', 'PERM_DEVICE_UPDATE', 'PERM_USER_MANAGE');

DELETE FROM permissions
WHERE permission_id IN ('PERM_DEVICE_CREATE', 'PERM_DEVICE_UPDATE', 'PERM_USER_MANAGE');

-- ── Step 3: Rebuild complete role_permissions matrix ────────
-- Strategy: delete all global (tenant_id IS NULL) bindings for
-- non-SUPER_ADMIN roles, then re-insert the authoritative matrix.
-- SUPER_ADMIN bypasses all checks in code, no bindings needed.

DELETE FROM role_permissions
WHERE role_id != 'ROLE_SUPER_ADMIN'
  AND (tenant_id IS NULL);

-- ─── ROLE_ADMIN: full access to everything ─────────────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    -- 02 系統管理
    'USER_LIST', 'USER_CREATE', 'USER_UPDATE', 'USER_DISABLE',
    'DEPT_LIST', 'DEPT_CREATE', 'DEPT_UPDATE', 'DEPT_DELETE',
    'MENU_LIST', 'MENU_CREATE', 'MENU_UPDATE', 'MENU_DELETE',
    'ROLE_LIST', 'ROLE_UPDATE',
    'AUDIT_LIST', 'LOG_SUMMARY_VIEW',
    'SYSTEM_SETTINGS_VIEW', 'SYSTEM_SETTINGS_MANAGE',
    'ANNOUNCEMENT_VIEW', 'ANNOUNCEMENT_MANAGE',
    -- 03 簽核
    'WORKFLOW_VIEW', 'WORKFLOW_MANAGE', 'DELEGATE_MANAGE',
    -- 04 資產
    'DEVICE_VIEW', 'DEVICE_MANAGE', 'CIRCUIT_VIEW', 'CIRCUIT_MANAGE',
    'FAULT_VIEW', 'FAULT_MANAGE', 'DEVICE_EXPORT',
    'CONTRACT_VIEW', 'CONTRACT_MANAGE',
    -- 05 報修
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH',
    'INSPECTION_VIEW', 'INSPECTION_MANAGE',
    -- 06 換裝
    'REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE', 'POLE_NUMBER_MANAGE',
    -- 07 材料
    'MATERIAL_VIEW', 'MATERIAL_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE',
    -- GIS
    'GIS_VIEW', 'GIS_MANAGE'
  )
ON CONFLICT DO NOTHING;

-- ─── ROLE_DEPT_ADMIN: department admin ─────────────────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'DEPT_ADMIN'
  AND p.code IN (
    -- 02 系統管理 (部門級)
    'USER_LIST', 'USER_CREATE', 'USER_UPDATE', 'USER_DISABLE',
    'DEPT_LIST', 'DEPT_CREATE', 'DEPT_UPDATE', 'DEPT_DELETE',
    'ROLE_LIST',
    'AUDIT_LIST', 'LOG_SUMMARY_VIEW',
    'SYSTEM_SETTINGS_VIEW',
    'ANNOUNCEMENT_VIEW', 'ANNOUNCEMENT_MANAGE',
    -- 03 簽核
    'WORKFLOW_VIEW', 'WORKFLOW_MANAGE', 'DELEGATE_MANAGE',
    -- 04 資產
    'DEVICE_VIEW', 'DEVICE_MANAGE', 'CIRCUIT_VIEW', 'CIRCUIT_MANAGE',
    'FAULT_VIEW', 'FAULT_MANAGE', 'DEVICE_EXPORT',
    'CONTRACT_VIEW', 'CONTRACT_MANAGE',
    -- 05 報修
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH',
    'INSPECTION_VIEW', 'INSPECTION_MANAGE',
    -- 06 換裝
    'REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE', 'POLE_NUMBER_MANAGE',
    -- 07 材料
    'MATERIAL_VIEW', 'MATERIAL_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE',
    -- GIS
    'GIS_VIEW', 'GIS_MANAGE'
  )
ON CONFLICT DO NOTHING;

-- ─── ROLE_OPERATOR: operations staff ───────────────────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'OPERATOR'
  AND p.code IN (
    -- 02 系統管理 (limited)
    'USER_LIST',
    'AUDIT_LIST',
    'ANNOUNCEMENT_VIEW',
    -- 03 簽核
    'WORKFLOW_VIEW', 'WORKFLOW_MANAGE', 'DELEGATE_MANAGE',
    -- 04 資產
    'DEVICE_VIEW', 'DEVICE_MANAGE', 'CIRCUIT_VIEW', 'CIRCUIT_MANAGE',
    'FAULT_VIEW', 'FAULT_MANAGE', 'DEVICE_EXPORT',
    'CONTRACT_VIEW',
    -- 05 報修
    'REPAIR_VIEW', 'REPAIR_MANAGE', 'REPAIR_DISPATCH',
    'INSPECTION_VIEW', 'INSPECTION_MANAGE',
    -- 06 換裝
    'REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE',
    -- 07 材料
    'MATERIAL_VIEW', 'MATERIAL_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE',
    -- GIS
    'GIS_VIEW'
  )
ON CONFLICT DO NOTHING;

-- ─── ROLE_FIELD_USER: field workers ────────────────────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'FIELD_USER'
  AND p.code IN (
    -- 02 系統管理 (minimal)
    'ANNOUNCEMENT_VIEW',
    -- 03 簽核
    'WORKFLOW_VIEW',
    -- 04 資產
    'DEVICE_VIEW', 'CIRCUIT_VIEW',
    'FAULT_VIEW', 'FAULT_MANAGE',
    -- 05 報修 (can submit completion reports)
    'REPAIR_VIEW', 'REPAIR_MANAGE',
    'INSPECTION_VIEW', 'INSPECTION_MANAGE',
    -- 06 換裝 (can submit completion reports)
    'REPLACEMENT_VIEW', 'REPLACEMENT_MANAGE',
    -- 07 材料
    'MATERIAL_VIEW', 'INVENTORY_VIEW',
    -- GIS
    'GIS_VIEW'
  )
ON CONFLICT DO NOTHING;

-- ─── ROLE_VIEWER: read-only ────────────────────────────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'VIEWER'
  AND p.code IN (
    -- 02 系統管理 (read-only)
    'USER_LIST', 'DEPT_LIST',
    'AUDIT_LIST',
    'ANNOUNCEMENT_VIEW',
    -- 03 簽核
    'WORKFLOW_VIEW',
    -- 04 資產
    'DEVICE_VIEW', 'CIRCUIT_VIEW',
    'FAULT_VIEW', 'DEVICE_EXPORT',
    'CONTRACT_VIEW',
    -- 05 報修
    'REPAIR_VIEW', 'INSPECTION_VIEW',
    -- 06 換裝
    'REPLACEMENT_VIEW',
    -- 07 材料
    'MATERIAL_VIEW', 'INVENTORY_VIEW',
    -- GIS
    'GIS_VIEW'
  )
ON CONFLICT DO NOTHING;

-- ─── ROLE_MONITOR: supervision/auditing ────────────────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'MONITOR'
  AND p.code IN (
    -- 02 系統管理
    'ANNOUNCEMENT_VIEW',
    -- 03 簽核
    'WORKFLOW_VIEW',
    -- 04 資產
    'DEVICE_VIEW', 'CIRCUIT_VIEW',
    'FAULT_VIEW', 'DEVICE_EXPORT',
    'CONTRACT_VIEW',
    -- 05 報修
    'REPAIR_VIEW', 'INSPECTION_VIEW',
    -- 06 換裝
    'REPLACEMENT_VIEW',
    -- 07 材料
    'MATERIAL_VIEW', 'INVENTORY_VIEW',
    -- GIS
    'GIS_VIEW'
  )
ON CONFLICT DO NOTHING;

-- ─── ROLE_DEPT_USER: department member (limited) ───────────

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'DEPT_USER'
  AND p.code IN (
    -- 02 系統管理 (minimal)
    'ANNOUNCEMENT_VIEW',
    -- 03 簽核
    'WORKFLOW_VIEW',
    -- 04 資產
    'DEVICE_VIEW', 'CIRCUIT_VIEW',
    'FAULT_VIEW',
    'CONTRACT_VIEW',
    -- 05 報修
    'REPAIR_VIEW', 'INSPECTION_VIEW',
    -- 06 換裝
    'REPLACEMENT_VIEW',
    -- 07 材料
    'MATERIAL_VIEW', 'INVENTORY_VIEW',
    -- GIS
    'GIS_VIEW'
  )
ON CONFLICT DO NOTHING;
