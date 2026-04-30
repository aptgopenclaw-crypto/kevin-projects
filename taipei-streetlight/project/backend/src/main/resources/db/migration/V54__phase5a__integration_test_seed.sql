-- ============================================================
-- V54: Phase 5A — 整合測試完整種子資料
-- 新增：人員（OPERATOR/FIELD_USER/MONITOR）掛入既有組織
--       + 設備 + 材料 + 庫存 + 合約
-- 修正：DEPT_USER 加入 FAULT_MANAGE 權限（可建障礙通報）
-- ============================================================
-- 既有組織架構（dept_info）：
--   公燈處 (root)
--     ├── 第一分隊（北區）/ 第二分隊（南區）/ 工程股 / 行政股 / 智慧路燈管理中心
--   協力廠商 (dept_id=11, root)
--     ├── FET (dept_id=12)
--     └── 設備商 (dept_id=13)
-- ============================================================

-- ============================================================
-- 1. RBAC 修正：DEPT_USER 增加 FAULT_MANAGE
-- ============================================================
INSERT INTO role_permissions (role_id, permission_id, tenant_id)
SELECT r.role_id, p.permission_id, NULL
FROM roles r, permissions p
WHERE r.code = 'DEPT_USER' AND p.code = 'FAULT_MANAGE'
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. 組織架構：補建 協力廠商 / FET / 設備商 部門
--    生產環境已由前台建立，但 clean-schema 測試需要
-- ============================================================
INSERT INTO dept_info (dept_id, tenant_id, pid, dept_name, dept_sort, status, hierarchy_path, create_by, create_time)
VALUES
    (11, 'TENANT_A', NULL,   '協力廠商', 20, 1, '/11', 'system', NOW()),
    (12, 'TENANT_A', 11,     'FET',      1,  1, '/11/12', 'system', NOW()),
    (13, 'TENANT_A', 11,     '設備商',   2,  1, '/11/13', 'system', NOW())
ON CONFLICT (dept_id) DO NOTHING;

-- Ensure sequence is advanced past manually-set IDs
SELECT setval('dept_info_dept_id_seq', GREATEST(nextval('dept_info_dept_id_seq'), 14));

-- ============================================================
-- 3. 人員：公燈處新增 OPERATOR / FIELD_USER / MONITOR
--          FET / 設備商 新增人員（掛入既有 dept_id=12, 13）
--    密碼 = Test0123456!
-- ============================================================

INSERT INTO users (user_id, email, password_hash, display_name) VALUES
    -- 公燈處 — 北區
    ('u-squad1-op',    'squad1-op@tpe-light.gov.tw',    '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', '北區維運 周志豪'),
    ('u-squad1-field', 'squad1-field@tpe-light.gov.tw', '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', '北區外勤 蔡文傑'),
    -- 公燈處 — 南區
    ('u-squad2-op',    'squad2-op@tpe-light.gov.tw',    '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', '南區維運 許家瑋'),
    ('u-squad2-field', 'squad2-field@tpe-light.gov.tw', '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', '南區外勤 劉俊宏'),
    -- 公燈處 — 工程股
    ('u-eng-monitor',  'eng-monitor@tpe-light.gov.tw',  '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', '工程監造 謝明達'),
    -- 公燈處 — 行政股
    ('u-adm-warehouse','adm-wh@tpe-light.gov.tw',       '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', '倉管 林淑芬'),
    -- FET
    ('u-fet-mgr',      'fet-mgr@fet.com.tw',            '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', 'FET 專案經理 張雅婷'),
    ('u-fet-op1',      'fet-op1@fet.com.tw',            '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', 'FET 維運工程師 陳柏翰'),
    -- 設備商 (dept_id=13)
    ('u-vendor-mgr',   'vendor-mgr@vendor.com',         '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', '設備商主管 洪啟文'),
    ('u-vendor-field1','vendor-f1@vendor.com',           '$2a$10$kRxROhoc8z/4urhjvkB5x.uHq7hu3hO6QuABUCkEivNGeXnQIWbj6', '設備商工程師 葉建廷')
ON CONFLICT (user_id) DO NOTHING;

-- ============================================================
-- 4. User-Tenant Mapping（角色 + 部門歸屬）
-- ============================================================

-- 公燈處 — 北區 OPERATOR
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-squad1-op', 'TENANT_A', 'ROLE_OPERATOR',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'))
ON CONFLICT DO NOTHING;

-- 公燈處 — 北區 FIELD_USER
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-squad1-field', 'TENANT_A', 'ROLE_FIELD_USER',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'))
ON CONFLICT DO NOTHING;

-- 公燈處 — 南區 OPERATOR
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-squad2-op', 'TENANT_A', 'ROLE_OPERATOR',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第二分隊（南區）'))
ON CONFLICT DO NOTHING;

-- 公燈處 — 南區 FIELD_USER
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-squad2-field', 'TENANT_A', 'ROLE_FIELD_USER',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第二分隊（南區）'))
ON CONFLICT DO NOTHING;

-- 工程股 — MONITOR
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-eng-monitor', 'TENANT_A', 'ROLE_MONITOR',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '工程股'))
ON CONFLICT DO NOTHING;

-- 行政股 — 倉管 OPERATOR
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-adm-warehouse', 'TENANT_A', 'ROLE_OPERATOR',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '行政股'))
ON CONFLICT DO NOTHING;

-- FET — 專案經理 DEPT_ADMIN (dept_id=12 FET)
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-fet-mgr', 'TENANT_A', 'ROLE_DEPT_ADMIN', 12)
ON CONFLICT DO NOTHING;

-- FET — 維運工程師 OPERATOR (dept_id=12 FET)
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-fet-op1', 'TENANT_A', 'ROLE_OPERATOR', 12)
ON CONFLICT DO NOTHING;

-- 設備商 — 主管 DEPT_ADMIN (dept_id=13 設備商)
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-vendor-mgr', 'TENANT_A', 'ROLE_DEPT_ADMIN', 13)
ON CONFLICT DO NOTHING;

-- 設備商 — 工程師 FIELD_USER (dept_id=13 設備商)
INSERT INTO user_tenant_mapping (user_id, tenant_id, role_id, dept_id)
VALUES ('u-vendor-field1', 'TENANT_A', 'ROLE_FIELD_USER', 13)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 5. 合約（FET 得標案 + 設備商硬體維護案）
-- ============================================================
INSERT INTO contracts (tenant_id, contract_code, contract_name, budget_year,
    contractor_name, contractor_contact, asset_category, start_date, end_date, status)
VALUES
('TENANT_A', 'CT-2026-FET-001', '台北市智慧路燈建置暨維運案（FET）', 2026,
    '遠傳電信股份有限公司', '張雅婷 02-7711-5678', 'SMART_LIGHT',
    '2026-01-01', '2030-12-31', 'ACTIVE'),
('TENANT_A', 'CT-2026-VENDOR-001', '路燈硬體維護採購案（設備商）', 2026,
    '設備商', '洪啟文 02-2592-1234', 'LUMINAIRE',
    '2026-01-01', '2027-12-31', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 6. 設備 — 信義路三段路燈 3 支（含迴路 + 配電箱）
-- ============================================================

-- 6.1 迴路用配電箱
INSERT INTO devices (tenant_id, device_type, device_code, device_name, lng, lat, status, dept_id,
    contract_id, installed_at, created_by)
VALUES ('TENANT_A', 'PANEL_BOX', 'PB-XINYI-001', '信義路三段配電箱',
    121.5440000, 25.0330000, 'ACTIVE',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-FET-001'),
    '2024-06-15', 'system')
ON CONFLICT DO NOTHING;

-- 6.2 電力迴路
INSERT INTO circuits (tenant_id, panel_box_device_id, circuit_number, circuit_name, taipower_account, status)
VALUES ('TENANT_A',
    (SELECT id FROM devices WHERE tenant_id = 'TENANT_A' AND device_code = 'PB-XINYI-001'),
    'CIR-XINYI-001', '信義路三段迴路A', 'TP-2024-XINYI-001', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- 6.3 路燈桿 3 支
INSERT INTO devices (tenant_id, device_type, device_code, device_name, lng, lat, status, dept_id,
    contract_id, circuit_id, installed_at, created_by)
VALUES
('TENANT_A', 'POLE', 'POLE-XINYI-001', '信義路三段 #001 燈桿',
    121.5441000, 25.0331000, 'ACTIVE',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-FET-001'),
    (SELECT id FROM circuits WHERE tenant_id = 'TENANT_A' AND circuit_number = 'CIR-XINYI-001'),
    '2024-06-15', 'system'),
('TENANT_A', 'POLE', 'POLE-XINYI-002', '信義路三段 #002 燈桿',
    121.5443000, 25.0332000, 'ACTIVE',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-FET-001'),
    (SELECT id FROM circuits WHERE tenant_id = 'TENANT_A' AND circuit_number = 'CIR-XINYI-001'),
    '2024-06-15', 'system'),
('TENANT_A', 'POLE', 'POLE-XINYI-003', '信義路三段 #003 燈桿',
    121.5445000, 25.0333000, 'ACTIVE',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-FET-001'),
    (SELECT id FROM circuits WHERE tenant_id = 'TENANT_A' AND circuit_number = 'CIR-XINYI-001'),
    '2024-06-15', 'system')
ON CONFLICT DO NOTHING;

-- 6.4 燈具（掛在燈桿下）
INSERT INTO devices (tenant_id, device_type, device_code, device_name, lng, lat, status, dept_id,
    contract_id, circuit_id, parent_device_id, installed_at, created_by)
VALUES
('TENANT_A', 'LUMINAIRE', 'LUM-XINYI-001', '信義路 #001 LED燈具',
    121.5441000, 25.0331000, 'ACTIVE',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-FET-001'),
    (SELECT id FROM circuits WHERE tenant_id = 'TENANT_A' AND circuit_number = 'CIR-XINYI-001'),
    (SELECT id FROM devices WHERE tenant_id = 'TENANT_A' AND device_code = 'POLE-XINYI-001'),
    '2024-06-15', 'system'),
('TENANT_A', 'LUMINAIRE', 'LUM-XINYI-002', '信義路 #002 LED燈具',
    121.5443000, 25.0332000, 'ACTIVE',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-FET-001'),
    (SELECT id FROM circuits WHERE tenant_id = 'TENANT_A' AND circuit_number = 'CIR-XINYI-001'),
    (SELECT id FROM devices WHERE tenant_id = 'TENANT_A' AND device_code = 'POLE-XINYI-002'),
    '2024-06-15', 'system'),
('TENANT_A', 'LUMINAIRE', 'LUM-XINYI-003', '信義路 #003 LED燈具',
    121.5445000, 25.0333000, 'ACTIVE',
    (SELECT dept_id FROM dept_info WHERE tenant_id = 'TENANT_A' AND dept_name = '第一分隊（北區）'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-FET-001'),
    (SELECT id FROM circuits WHERE tenant_id = 'TENANT_A' AND circuit_number = 'CIR-XINYI-001'),
    (SELECT id FROM devices WHERE tenant_id = 'TENANT_A' AND device_code = 'POLE-XINYI-003'),
    '2024-06-15', 'system')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 7. 材料規格 + 倉庫 + 庫存 + 核准材料
-- ============================================================

-- 7.1 倉庫
INSERT INTO warehouses (tenant_id, warehouse_code, warehouse_name, location, status) VALUES
('TENANT_A', 'WH-NORTH', '北區材料倉庫', '臺北市中山區民權東路三段', 'ACTIVE'),
('TENANT_A', 'WH-SOUTH', '南區材料倉庫', '臺北市信義區基隆路二段', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- 7.2 材料規格
INSERT INTO material_specs (tenant_id, spec_code, spec_name, category, unit, status) VALUES
('TENANT_A', 'MS-LED-100W',   'LED路燈燈具 100W',    'LUMINAIRE', 'PCS', 'ACTIVE'),
('TENANT_A', 'MS-LED-150W',   'LED路燈燈具 150W',    'LUMINAIRE', 'PCS', 'ACTIVE'),
('TENANT_A', 'MS-POLE-8M',    '鍍鋅鋼桿 8M',        'POLE',      'PCS', 'ACTIVE'),
('TENANT_A', 'MS-ARM-1.5M',   '燈臂 1.5M',           'ACCESSORY', 'PCS', 'ACTIVE'),
('TENANT_A', 'MS-CTRL-SMART', '智慧控制器 NB-IoT',   'CONTROLLER','PCS', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- 7.3 庫存
INSERT INTO inventory (tenant_id, warehouse_id, material_spec_id, quantity_on_hand, safety_stock) VALUES
('TENANT_A',
    (SELECT id FROM warehouses WHERE tenant_id = 'TENANT_A' AND warehouse_code = 'WH-NORTH'),
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-LED-100W'),
    50, 10),
('TENANT_A',
    (SELECT id FROM warehouses WHERE tenant_id = 'TENANT_A' AND warehouse_code = 'WH-NORTH'),
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-LED-150W'),
    30, 5),
('TENANT_A',
    (SELECT id FROM warehouses WHERE tenant_id = 'TENANT_A' AND warehouse_code = 'WH-NORTH'),
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-POLE-8M'),
    15, 3),
('TENANT_A',
    (SELECT id FROM warehouses WHERE tenant_id = 'TENANT_A' AND warehouse_code = 'WH-NORTH'),
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-ARM-1.5M'),
    40, 8),
('TENANT_A',
    (SELECT id FROM warehouses WHERE tenant_id = 'TENANT_A' AND warehouse_code = 'WH-NORTH'),
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-CTRL-SMART'),
    20, 5),
('TENANT_A',
    (SELECT id FROM warehouses WHERE tenant_id = 'TENANT_A' AND warehouse_code = 'WH-SOUTH'),
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-LED-100W'),
    35, 10)
ON CONFLICT DO NOTHING;

-- 7.4 核准材料（設備商合約綁定）
INSERT INTO approved_materials (tenant_id, material_spec_id, contract_id, material_number,
    approval_date, brand, model, status) VALUES
('TENANT_A',
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-LED-100W'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-VENDOR-001'),
    'AM-2026-001', '2026-01-15', '設備商', 'VD-LED-100W-A', 'ACTIVE'),
('TENANT_A',
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-LED-150W'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-VENDOR-001'),
    'AM-2026-002', '2026-01-15', '設備商', 'VD-LED-150W-A', 'ACTIVE'),
('TENANT_A',
    (SELECT id FROM material_specs WHERE tenant_id = 'TENANT_A' AND spec_code = 'MS-POLE-8M'),
    (SELECT id FROM contracts WHERE tenant_id = 'TENANT_A' AND contract_code = 'CT-2026-VENDOR-001'),
    'AM-2026-003', '2026-01-15', '設備商', 'VD-POLE-8M-G', 'ACTIVE')
ON CONFLICT DO NOTHING;
