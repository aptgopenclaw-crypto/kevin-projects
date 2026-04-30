-- =============================================
-- V32: 04 障礙工單 + 關聯偵測 + 選單 + 權限 + 角色綁定
-- =============================================

-- 1. 關聯障礙表（先建，fault_tickets FK 指向它）
CREATE TABLE fault_correlations (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    root_cause_type     VARCHAR(30)     NOT NULL,
    root_cause_id       BIGINT          NOT NULL,
    affected_count      INT             NOT NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DETECTED',
    detected_at         TIMESTAMP       NOT NULL DEFAULT now(),
    confirmed_at        TIMESTAMP,
    resolved_at         TIMESTAMP,
    resolution_note     TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_fault_correlations_tenant ON fault_correlations(tenant_id);
CREATE INDEX idx_fault_correlations_type ON fault_correlations(root_cause_type, root_cause_id);

COMMENT ON TABLE fault_correlations IS '關聯障礙：CIRCUIT / PANEL_BOX / GATEWAY / POWER_OUTAGE';
COMMENT ON COLUMN fault_correlations.status IS 'DETECTED / CONFIRMED / RESOLVED';

-- 2. 障礙工單表
CREATE TABLE fault_tickets (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    device_id           BIGINT          REFERENCES devices(id),
    circuit_id          BIGINT          REFERENCES circuits(id),
    correlation_id      BIGINT          REFERENCES fault_correlations(id),
    source              VARCHAR(30)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    priority            VARCHAR(10)     DEFAULT 'NORMAL',
    description         TEXT,
    reported_by         VARCHAR(50),
    reported_at         TIMESTAMP       NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMP,
    resolved_by         VARCHAR(50),
    resolution_note     TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_fault_tickets_tenant ON fault_tickets(tenant_id);
CREATE INDEX idx_fault_tickets_device ON fault_tickets(device_id);
CREATE INDEX idx_fault_tickets_circuit ON fault_tickets(circuit_id);
CREATE INDEX idx_fault_tickets_status ON fault_tickets(tenant_id, status);
CREATE INDEX idx_fault_tickets_correlation ON fault_tickets(correlation_id);

COMMENT ON TABLE fault_tickets IS '障礙工單：CITIZEN_REPORT / PATROL / AUTO_ALERT';
COMMENT ON COLUMN fault_tickets.status IS 'OPEN / IN_PROGRESS / RESOLVED / MERGED';
COMMENT ON COLUMN fault_tickets.priority IS 'LOW / NORMAL / HIGH / URGENT';

-- =============================================
-- Menu: 資產管理群組 + 子頁面
-- =============================================
INSERT INTO menus (menu_id, parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(35, NULL, '資產管理', 'DIRECTORY', 'AssetManagement', '/admin/asset', NULL, NULL, 'Box', 30, true),
(36, 35, '設備管理', 'PAGE', 'DeviceManagement', '/admin/asset/devices', 'views/admin/asset/DeviceManagementView.vue', 'DEVICE_VIEW', 'Cpu', 10, true),
(37, 35, '回路管理', 'PAGE', 'CircuitManagement', '/admin/asset/circuits', 'views/admin/asset/CircuitManagementView.vue', 'CIRCUIT_VIEW', 'Zap', 20, true),
(38, 35, '障礙工單', 'PAGE', 'FaultTickets', '/admin/asset/faults', 'views/admin/asset/FaultTicketView.vue', 'FAULT_VIEW', 'AlertTriangle', 30, true),
(39, 35, '契約管理', 'PAGE', 'ContractManagement', '/admin/asset/contracts', 'views/admin/asset/ContractManagementView.vue', 'CONTRACT_VIEW', 'FileText', 40, true),
(40, NULL, '簽核管理', 'DIRECTORY', 'WorkflowManagement', '/admin/workflow', NULL, NULL, 'ClipboardCheck', 35, true),
(41, 40, '待辦案件', 'PAGE', 'PendingTasks', '/admin/workflow/pending', 'views/admin/workflow/PendingTasksView.vue', 'WORKFLOW_VIEW', 'ListChecks', 10, true),
(42, 40, '代理人設定', 'PAGE', 'DelegateSettings', '/admin/workflow/delegates', 'views/admin/workflow/DelegateSettingsView.vue', 'DELEGATE_MANAGE', 'UserCheck', 20, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 42));

-- =============================================
-- Permissions
-- =============================================
INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_DEVICE_VIEW',     'DEVICE_VIEW',     '檢視設備',     '資產管理', 1),
('PERM_DEVICE_MANAGE',   'DEVICE_MANAGE',   '管理設備',     '資產管理', 2),
('PERM_CIRCUIT_VIEW',    'CIRCUIT_VIEW',    '檢視回路',     '資產管理', 3),
('PERM_CIRCUIT_MANAGE',  'CIRCUIT_MANAGE',  '管理回路',     '資產管理', 4),
('PERM_FAULT_VIEW',      'FAULT_VIEW',      '檢視障礙工單', '資產管理', 5),
('PERM_FAULT_MANAGE',    'FAULT_MANAGE',    '管理障礙工單', '資產管理', 6),
('PERM_DEVICE_EXPORT',   'DEVICE_EXPORT',   '匯出設備資產', '資產管理', 7),
('PERM_CONTRACT_VIEW',   'CONTRACT_VIEW',   '檢視契約',     '資產管理', 8),
('PERM_CONTRACT_MANAGE', 'CONTRACT_MANAGE', '管理契約',     '資產管理', 9),
('PERM_WORKFLOW_VIEW',   'WORKFLOW_VIEW',   '檢視待辦案件', '簽核管理', 10),
('PERM_DELEGATE_MANAGE', 'DELEGATE_MANAGE', '管理代理人',   '簽核管理', 11)
ON CONFLICT (code) DO NOTHING;

-- =============================================
-- Role binding: ADMIN + DEPT_ADMIN → 全部
-- =============================================
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code IN (
    'DEVICE_VIEW', 'DEVICE_MANAGE', 'CIRCUIT_VIEW', 'CIRCUIT_MANAGE',
    'FAULT_VIEW', 'FAULT_MANAGE', 'DEVICE_EXPORT', 'CONTRACT_VIEW', 'CONTRACT_MANAGE',
    'WORKFLOW_VIEW', 'DELEGATE_MANAGE'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'DEPT_ADMIN' AND p.code IN (
    'DEVICE_VIEW', 'DEVICE_MANAGE', 'CIRCUIT_VIEW', 'CIRCUIT_MANAGE',
    'FAULT_VIEW', 'FAULT_MANAGE', 'DEVICE_EXPORT', 'CONTRACT_VIEW', 'CONTRACT_MANAGE',
    'WORKFLOW_VIEW', 'DELEGATE_MANAGE'
)
ON CONFLICT DO NOTHING;

-- OPERATOR: 設備/回路檢視 + 障礙管理 + 簽核 + 匯出
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'OPERATOR' AND p.code IN (
    'DEVICE_VIEW', 'CIRCUIT_VIEW', 'FAULT_VIEW', 'FAULT_MANAGE',
    'DEVICE_EXPORT', 'WORKFLOW_VIEW', 'CONTRACT_VIEW'
)
ON CONFLICT DO NOTHING;

-- FIELD_USER: 檢視 + 障礙管理 + 簽核
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'FIELD_USER' AND p.code IN (
    'DEVICE_VIEW', 'CIRCUIT_VIEW', 'FAULT_VIEW', 'FAULT_MANAGE', 'WORKFLOW_VIEW'
)
ON CONFLICT DO NOTHING;

-- VIEWER / MONITOR: 只有檢視
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code IN ('VIEWER', 'MONITOR') AND p.code IN (
    'DEVICE_VIEW', 'CIRCUIT_VIEW', 'FAULT_VIEW', 'CONTRACT_VIEW', 'WORKFLOW_VIEW'
)
ON CONFLICT DO NOTHING;
