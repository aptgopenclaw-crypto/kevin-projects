-- ============================================================
-- V70: 智慧路燈 (IoT) Phase 7a — 權限 + 選單 + 角色綁定
--       + devices 表 IoT 擴充欄位
-- ============================================================

-- ── 1. devices 表 IoT 擴充 ──────────────────────────────────

ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_token      VARCHAR(200) UNIQUE;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS auth_type          VARCHAR(20);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS firmware_version   VARCHAR(50);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_telemetry_at  TIMESTAMP;
-- format_id FK 會在 V71 telemetry_formats 建表後加入

-- ── 2. Permission entries ───────────────────────────────────

INSERT INTO permissions (permission_id, code, name, group_name, sort_order) VALUES
('PERM_IOT_VIEW',    'IOT_VIEW',    '智慧路燈查看', '智慧路燈', 1),
('PERM_IOT_MANAGE',  'IOT_MANAGE',  '智慧路燈管理', '智慧路燈', 2),
('PERM_IOT_DIMMING', 'IOT_DIMMING', '調光控制',     '智慧路燈', 3)
ON CONFLICT (permission_id) DO NOTHING;

-- ── 3. Menu entries ─────────────────────────────────────────

DO $$
DECLARE
    v_dir_id BIGINT;
BEGIN
    -- DIRECTORY: 智慧路燈
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, icon, sort_order, visible)
    VALUES (NULL, '智慧路燈', 'DIRECTORY', 'SmartIoT', '/admin/iot', NULL, 'Wifi', 5, true)
    RETURNING menu_id INTO v_dir_id;

    -- PAGE: 即時監控地圖
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '即時監控地圖', 'PAGE', 'IoTMap', '/admin/iot/map', 'views/admin/iot/IoTMapView.vue', 'IOT_VIEW', 10, true);

    -- PAGE: IoT 設備管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, 'IoT 設備管理', 'PAGE', 'IoTDevices', '/admin/iot/devices', 'views/admin/iot/IoTDeviceListView.vue', 'IOT_MANAGE', 20, true);

    -- PAGE: Telemetry Format 管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, 'Telemetry Format', 'PAGE', 'TelemetryFormats', '/admin/iot/telemetry-formats', 'views/admin/iot/TelemetryFormatView.vue', 'IOT_MANAGE', 30, true);

    -- PAGE: 事件規則管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '事件規則管理', 'PAGE', 'EventRules', '/admin/iot/event-rules', 'views/admin/iot/EventRuleView.vue', 'IOT_MANAGE', 40, true);

    -- PAGE: 告警管理
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '告警管理', 'PAGE', 'Alerts', '/admin/iot/alerts', 'views/admin/iot/AlertListView.vue', 'IOT_VIEW', 50, true);

    -- PAGE: 調光控制
    INSERT INTO menus (parent_id, name, menu_type, route_name, route_path, component, permission_code, sort_order, visible)
    VALUES (v_dir_id, '調光控制', 'PAGE', 'Dimming', '/admin/iot/dimming', 'views/admin/iot/DimmingView.vue', 'IOT_DIMMING', 60, true);
END $$;

-- ── 4. Role binding ─────────────────────────────────────────

-- ADMIN → 全部 IoT 權限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN ('IOT_VIEW', 'IOT_MANAGE', 'IOT_DIMMING')
ON CONFLICT DO NOTHING;

-- DEPT_ADMIN → IOT_VIEW + IOT_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'DEPT_ADMIN'
  AND p.code IN ('IOT_VIEW', 'IOT_MANAGE')
ON CONFLICT DO NOTHING;

-- OPERATOR → IOT_VIEW + IOT_DIMMING
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'OPERATOR'
  AND p.code IN ('IOT_VIEW', 'IOT_DIMMING')
ON CONFLICT DO NOTHING;

-- MONITOR → IOT_VIEW (唯讀)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r, permissions p
WHERE r.code = 'MONITOR'
  AND p.code = 'IOT_VIEW'
ON CONFLICT DO NOTHING;
