-- ============================================================
-- V56: 新增招標公告 / 決標公告匯出權限
-- ============================================================

-- ── 1. Permissions ───────────────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_TENDER_ANN_EXPORT',   'tender:announcement:export', '匯出招標公告', '招標管理', 102),
('PERM_TENDER_AWARD_EXPORT', 'tender:award:export',        '匯出決標公告', '招標管理', 133)
ON CONFLICT (code) DO NOTHING;

-- ── 2. Role bindings ─────────────────────────────────────────
-- 凡擁有瀏覽權限的角色（ADMIN / OPERATOR / VIEWER），一律補上對應的匯出權限

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code IN ('ADMIN', 'OPERATOR', 'VIEWER')
  AND p.code IN ('tender:announcement:export', 'tender:award:export')
ON CONFLICT DO NOTHING;
