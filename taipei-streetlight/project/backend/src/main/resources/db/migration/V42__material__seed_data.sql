-- =============================================================
-- V42: 材料管理 — 種子資料（測試 / 展示用）
-- =============================================================
-- 依賴: V40 (基礎表) + V41 (操作表) + V34 (契約 C-114-001, C-114-002)
-- tenant_id = 'TENANT_A'
-- =============================================================

-- ============================================================
-- 1. 庫別（3 座倉庫）
-- ============================================================
INSERT INTO warehouses (tenant_id, warehouse_code, warehouse_name, location, status, created_at, updated_at)
VALUES
    ('TENANT_A', 'WH-MAIN',  '公燈處主倉庫',       '臺北市信義區市府路1號B2',   'ACTIVE',   now(), now()),
    ('TENANT_A', 'WH-NORTH', '北區分隊材料倉',     '臺北市北投區光明路250號',   'ACTIVE',   now(), now()),
    ('TENANT_A', 'WH-SOUTH', '南區分隊材料倉',     '臺北市文山區木柵路三段50號', 'ACTIVE',   now(), now());

-- ============================================================
-- 2. 材料規格（18 筆，涵蓋全部 6 類別）
-- ============================================================
INSERT INTO material_specs (tenant_id, spec_code, spec_name, category, unit, attributes, status, created_at, updated_at)
VALUES
    -- LUMINAIRE 燈具（6 筆）
    ('TENANT_A', 'LED-150W',   'LED路燈燈具 150W',        'LUMINAIRE',  'PCS', '{"wattage": 150, "color_temp": 4000, "lumen": 21000}',    'ACTIVE', now(), now()),
    ('TENANT_A', 'LED-200W',   'LED路燈燈具 200W',        'LUMINAIRE',  'PCS', '{"wattage": 200, "color_temp": 4000, "lumen": 28000}',    'ACTIVE', now(), now()),
    ('TENANT_A', 'LED-100W',   'LED路燈燈具 100W',        'LUMINAIRE',  'PCS', '{"wattage": 100, "color_temp": 3000, "lumen": 14000}',    'ACTIVE', now(), now()),
    ('TENANT_A', 'LED-250W',   'LED高功率路燈燈具 250W',  'LUMINAIRE',  'PCS', '{"wattage": 250, "color_temp": 5000, "lumen": 35000}',    'ACTIVE', now(), now()),
    ('TENANT_A', 'LED-SOL-60', '太陽能LED燈具 60W',       'LUMINAIRE',  'PCS', '{"wattage": 60, "solar_panel": "100W", "battery": "30Ah"}', 'ACTIVE', now(), now()),
    ('TENANT_A', 'LED-PARK-80','公園景觀LED燈具 80W',     'LUMINAIRE',  'PCS', '{"wattage": 80, "color_temp": 3000, "style": "park"}',     'DEPRECATED', now(), now()),

    -- CONTROLLER 控制器（3 筆）
    ('TENANT_A', 'CTRL-NB01',  '智慧路燈控制器 NB-IoT',   'CONTROLLER', 'PCS', '{"protocol": "NB-IoT", "voltage": "AC220V"}',            'ACTIVE', now(), now()),
    ('TENANT_A', 'CTRL-LORA',  '智慧路燈控制器 LoRa',     'CONTROLLER', 'PCS', '{"protocol": "LoRa", "voltage": "AC220V"}',              'ACTIVE', now(), now()),
    ('TENANT_A', 'CTRL-4G',    '4G遠端控制器',            'CONTROLLER', 'PCS', '{"protocol": "4G", "voltage": "AC220V/DC24V"}',          'ACTIVE', now(), now()),

    -- POLE 燈桿（3 筆）
    ('TENANT_A', 'POLE-6M',    '鍍鋅鋼管燈桿 6M',        'POLE',       'PCS', '{"height": 6, "material": "galvanized_steel", "diameter": "76mm"}',  'ACTIVE', now(), now()),
    ('TENANT_A', 'POLE-8M',    '鍍鋅鋼管燈桿 8M',        'POLE',       'PCS', '{"height": 8, "material": "galvanized_steel", "diameter": "89mm"}',  'ACTIVE', now(), now()),
    ('TENANT_A', 'POLE-10M',   '鍍鋅鋼管燈桿 10M',       'POLE',       'PCS', '{"height": 10, "material": "galvanized_steel", "diameter": "114mm"}','ACTIVE', now(), now()),

    -- POLE_NUMBER 桿號牌（1 筆）
    ('TENANT_A', 'PN-STD',     '標準反光桿號牌',          'POLE_NUMBER','PCS', '{"size": "15x30cm", "reflective": true}',                 'ACTIVE', now(), now()),

    -- CABLE 電纜（3 筆）
    ('TENANT_A', 'CBL-2.0',    'PVC電力電纜 2.0mm²',      'CABLE',      'M',   '{"cross_section": "2.0mm²", "insulation": "PVC"}',       'ACTIVE', now(), now()),
    ('TENANT_A', 'CBL-3.5',    'PVC電力電纜 3.5mm²',      'CABLE',      'M',   '{"cross_section": "3.5mm²", "insulation": "PVC"}',       'ACTIVE', now(), now()),
    ('TENANT_A', 'CBL-5.5',    'XLPE電力電纜 5.5mm²',     'CABLE',      'M',   '{"cross_section": "5.5mm²", "insulation": "XLPE"}',      'ACTIVE', now(), now()),

    -- OTHER 其他（2 筆）
    ('TENANT_A', 'FUSE-15A',   '管狀保險絲 15A',          'OTHER',      'PCS', '{"rating": "15A", "voltage": "250V"}',                   'ACTIVE', now(), now()),
    ('TENANT_A', 'BRKR-20A',   '無熔絲開關 20A',          'OTHER',      'PCS', '{"rating": "20A", "poles": 2}',                          'ACTIVE', now(), now());

-- ============================================================
-- 3. 廠商（4 家）
-- ============================================================
INSERT INTO suppliers (tenant_id, supplier_code, supplier_name, contact_name, contact_phone, contact_email, address, status, created_at, updated_at)
VALUES
    ('TENANT_A', 'SUP-GM',   '光明照明股份有限公司',   '王經理', '02-2771-0001', 'wang@guangming.com.tw',   '臺北市大安區復興南路一段100號',  'ACTIVE', now(), now()),
    ('TENANT_A', 'SUP-FET',  '遠傳電信股份有限公司',   '陳專案', '02-7723-5000', 'chen@fetnet.net',         '臺北市內湖區瑞光路468號',       'ACTIVE', now(), now()),
    ('TENANT_A', 'SUP-POLE', '大同鋼鐵股份有限公司',   '李主任', '02-2596-3000', 'lee@tatung-steel.com.tw', '臺北市中山區中山北路三段22號',   'ACTIVE', now(), now()),
    ('TENANT_A', 'SUP-CBL',  '太平洋電線電纜公司',     '張襄理', '02-2756-8800', 'chang@pacific-cable.com', '臺北市松山區八德路四段678號',    'INACTIVE', now(), now());

-- ============================================================
-- 4. 庫存（DO block：為主倉庫 + 北區建立庫存資料）
-- ============================================================
DO $$
DECLARE
    v_tenant   TEXT := 'TENANT_A';
    v_wh_main  BIGINT;
    v_wh_north BIGINT;
    v_wh_south BIGINT;
    v_spec     RECORD;
BEGIN
    SELECT id INTO v_wh_main  FROM warehouses WHERE tenant_id = v_tenant AND warehouse_code = 'WH-MAIN';
    SELECT id INTO v_wh_north FROM warehouses WHERE tenant_id = v_tenant AND warehouse_code = 'WH-NORTH';
    SELECT id INTO v_wh_south FROM warehouses WHERE tenant_id = v_tenant AND warehouse_code = 'WH-SOUTH';

    -- 主倉庫：全部品項都有庫存
    FOR v_spec IN
        SELECT id, spec_code, category FROM material_specs WHERE tenant_id = v_tenant AND status = 'ACTIVE'
    LOOP
        INSERT INTO inventory (tenant_id, warehouse_id, material_spec_id, quantity_on_hand, safety_stock, updated_at)
        VALUES (
            v_tenant, v_wh_main, v_spec.id,
            CASE
                WHEN v_spec.category = 'LUMINAIRE'   THEN (20 + floor(random() * 30))::INT   -- 20~49
                WHEN v_spec.category = 'CONTROLLER'  THEN (10 + floor(random() * 20))::INT   -- 10~29
                WHEN v_spec.category = 'POLE'        THEN (5  + floor(random() * 15))::INT   -- 5~19
                WHEN v_spec.category = 'CABLE'       THEN (500 + floor(random() * 500))::INT -- 500~999 公尺
                ELSE (15 + floor(random() * 25))::INT                                         -- 15~39
            END,
            CASE
                WHEN v_spec.category = 'LUMINAIRE'   THEN 10
                WHEN v_spec.category = 'CONTROLLER'  THEN 5
                WHEN v_spec.category = 'POLE'        THEN 3
                WHEN v_spec.category = 'CABLE'       THEN 200
                ELSE 10
            END,
            now()
        );
    END LOOP;

    -- 北區倉庫：僅常用燈具 + 控制器
    FOR v_spec IN
        SELECT id, spec_code, category FROM material_specs
        WHERE tenant_id = v_tenant AND status = 'ACTIVE'
          AND spec_code IN ('LED-150W', 'LED-200W', 'LED-100W', 'CTRL-NB01', 'CTRL-LORA', 'FUSE-15A', 'BRKR-20A')
    LOOP
        INSERT INTO inventory (tenant_id, warehouse_id, material_spec_id, quantity_on_hand, safety_stock, updated_at)
        VALUES (
            v_tenant, v_wh_north, v_spec.id,
            CASE
                WHEN v_spec.category = 'LUMINAIRE'  THEN (5 + floor(random() * 10))::INT  -- 5~14
                WHEN v_spec.category = 'CONTROLLER' THEN (3 + floor(random() * 7))::INT   -- 3~9
                ELSE (5 + floor(random() * 10))::INT
            END,
            CASE
                WHEN v_spec.category = 'LUMINAIRE'  THEN 5
                WHEN v_spec.category = 'CONTROLLER' THEN 3
                ELSE 5
            END,
            now()
        );
    END LOOP;

    -- 南區倉庫：少量燈具（製造低庫存預警場景）
    FOR v_spec IN
        SELECT id, spec_code FROM material_specs
        WHERE tenant_id = v_tenant AND spec_code IN ('LED-150W', 'LED-200W')
    LOOP
        INSERT INTO inventory (tenant_id, warehouse_id, material_spec_id, quantity_on_hand, safety_stock, updated_at)
        VALUES (
            v_tenant, v_wh_south, v_spec.id,
            CASE v_spec.spec_code
                WHEN 'LED-150W' THEN 2   -- 低於安全庫存 → 預警
                WHEN 'LED-200W' THEN 1   -- 低於安全庫存 → 預警
            END,
            5,  -- safety_stock = 5，但庫存只有 1~2 → 觸發預警
            now()
        );
    END LOOP;
END $$;

-- ============================================================
-- 5. 審驗合格材料（8 筆，關聯契約 C-114-001）
-- ============================================================
DO $$
DECLARE
    v_tenant      TEXT := 'TENANT_A';
    v_contract_id BIGINT;
    v_spec_id     BIGINT;
BEGIN
    SELECT id INTO v_contract_id FROM contracts WHERE tenant_id = v_tenant AND contract_code = 'C-114-001';

    -- LED-150W 合格材料 ×2
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-150W';
    INSERT INTO approved_materials (tenant_id, material_spec_id, contract_id, material_number, approval_date, batch_number, brand, model, spec_details, status, created_at)
    VALUES
        (v_tenant, v_spec_id, v_contract_id, 'AM-2026-001', '2026-01-15', 'B2026-01', '飛利浦', 'RoadFlair BRP392 150W', '{"lumen": 21000, "CRI": 70, "IP": "IP66"}', 'ACTIVE', now()),
        (v_tenant, v_spec_id, v_contract_id, 'AM-2026-002', '2026-01-15', 'B2026-01', '歐司朗', 'SL20 LED 150W',        '{"lumen": 20500, "CRI": 80, "IP": "IP65"}', 'ACTIVE', now());

    -- LED-200W 合格材料 ×2
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-200W';
    INSERT INTO approved_materials (tenant_id, material_spec_id, contract_id, material_number, approval_date, batch_number, brand, model, spec_details, status, created_at)
    VALUES
        (v_tenant, v_spec_id, v_contract_id, 'AM-2026-003', '2026-02-01', 'B2026-02', '飛利浦', 'RoadFlair BRP392 200W', '{"lumen": 28000, "CRI": 70, "IP": "IP66"}', 'ACTIVE', now()),
        (v_tenant, v_spec_id, v_contract_id, 'AM-2026-004', '2026-02-01', 'B2026-02', '歐司朗', 'SL20 LED 200W',        '{"lumen": 27500, "CRI": 80, "IP": "IP65"}', 'ACTIVE', now());

    -- CTRL-NB01 合格材料 ×1
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'CTRL-NB01';
    INSERT INTO approved_materials (tenant_id, material_spec_id, contract_id, material_number, approval_date, batch_number, brand, model, spec_details, status, created_at)
    VALUES
        (v_tenant, v_spec_id, v_contract_id, 'AM-2026-005', '2026-02-10', 'B2026-03', '遠傳', 'IoT-SLC-NB100', '{"protocol": "NB-IoT", "firmware": "v2.3"}', 'ACTIVE', now());

    -- POLE-8M 合格材料 ×1
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'POLE-8M';
    INSERT INTO approved_materials (tenant_id, material_spec_id, contract_id, material_number, approval_date, batch_number, brand, model, spec_details, status, created_at)
    VALUES
        (v_tenant, v_spec_id, v_contract_id, 'AM-2026-006', '2026-03-01', 'B2026-04', '大同鋼鐵', 'TT-GP-8000', '{"height": 8, "galvanized": true, "wind_resist": "16級"}', 'ACTIVE', now());

    -- 過期材料（展示 EXPIRED 狀態）
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-100W';
    INSERT INTO approved_materials (tenant_id, material_spec_id, contract_id, material_number, approval_date, batch_number, brand, model, spec_details, status, created_at)
    VALUES
        (v_tenant, v_spec_id, v_contract_id, 'AM-2025-010', '2025-03-01', 'B2025-01', '東亞照明', 'EA-LED-100', '{"lumen": 13500}', 'EXPIRED', '2025-03-01');

    -- 撤銷材料（展示 REVOKED 狀態）
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-150W';
    INSERT INTO approved_materials (tenant_id, material_spec_id, contract_id, material_number, approval_date, batch_number, brand, model, spec_details, status, created_at)
    VALUES
        (v_tenant, v_spec_id, v_contract_id, 'AM-2025-011', '2025-06-15', 'B2025-05', '亞洲光電', 'AL-SL150X', '{"lumen": 19000, "defect": "散熱不良"}', 'REVOKED', '2025-06-15');
END $$;

-- ============================================================
-- 6. 採購單（2 筆：已完成 + 草稿）
-- ============================================================
DO $$
DECLARE
    v_tenant      TEXT := 'TENANT_A';
    v_supplier_gm BIGINT;
    v_supplier_pole BIGINT;
    v_contract_id BIGINT;
    v_po1_id      BIGINT;
    v_po2_id      BIGINT;
    v_spec_id     BIGINT;
BEGIN
    SELECT id INTO v_supplier_gm   FROM suppliers WHERE tenant_id = v_tenant AND supplier_code = 'SUP-GM';
    SELECT id INTO v_supplier_pole FROM suppliers WHERE tenant_id = v_tenant AND supplier_code = 'SUP-POLE';
    SELECT id INTO v_contract_id   FROM contracts WHERE tenant_id = v_tenant AND contract_code = 'C-114-001';

    -- PO-1：已完成（燈具採購）
    INSERT INTO purchase_orders (tenant_id, po_number, supplier_id, contract_id, order_date, status, total_amount, notes, created_by, created_at, updated_at)
    VALUES (v_tenant, 'PO-20260301-001', v_supplier_gm, v_contract_id, '2026-03-01', 'COMPLETED', 375000.00,
            '114年度第一批LED燈具補充採購', 'u-tpe-admin', '2026-03-01', '2026-03-15')
    RETURNING id INTO v_po1_id;

    -- PO-1 明細
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-150W';
    INSERT INTO purchase_items (po_id, material_spec_id, quantity, unit_price, notes)
    VALUES (v_po1_id, v_spec_id, 50, 3500.00, '飛利浦 RoadFlair 150W');

    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-200W';
    INSERT INTO purchase_items (po_id, material_spec_id, quantity, unit_price, notes)
    VALUES (v_po1_id, v_spec_id, 30, 4500.00, '飛利浦 RoadFlair 200W');

    -- PO-2：草稿（燈桿採購）
    INSERT INTO purchase_orders (tenant_id, po_number, supplier_id, contract_id, order_date, status, total_amount, notes, created_by, created_at, updated_at)
    VALUES (v_tenant, 'PO-20260415-001', v_supplier_pole, v_contract_id, '2026-04-15', 'DRAFT', 300000.00,
            '114年度燈桿替換採購案', 'u-tpe-admin', '2026-04-15', '2026-04-15')
    RETURNING id INTO v_po2_id;

    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'POLE-8M';
    INSERT INTO purchase_items (po_id, material_spec_id, quantity, unit_price, notes)
    VALUES (v_po2_id, v_spec_id, 20, 12000.00, '大同鋼鐵 TT-GP-8000');

    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'POLE-6M';
    INSERT INTO purchase_items (po_id, material_spec_id, quantity, unit_price, notes)
    VALUES (v_po2_id, v_spec_id, 10, 6000.00, NULL);
END $$;

-- ============================================================
-- 7. 收料紀錄（PO-1 的收料，已入主倉庫）
-- ============================================================
DO $$
DECLARE
    v_tenant   TEXT := 'TENANT_A';
    v_po_id    BIGINT;
    v_wh_id    BIGINT;
    v_spec_id  BIGINT;
BEGIN
    SELECT id INTO v_po_id FROM purchase_orders WHERE tenant_id = v_tenant AND po_number = 'PO-20260301-001';
    SELECT id INTO v_wh_id FROM warehouses WHERE tenant_id = v_tenant AND warehouse_code = 'WH-MAIN';

    -- 第一批收料：LED-150W ×30
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-150W';
    INSERT INTO receiving_records (tenant_id, po_id, warehouse_id, material_spec_id, quantity, received_date, delivery_note, received_by, created_at)
    VALUES (v_tenant, v_po_id, v_wh_id, v_spec_id, 30, '2026-03-10', '第一批到貨，經品管驗收合格', 'u-tpe-operator-n', '2026-03-10');

    -- 第二批收料：LED-150W ×20 + LED-200W ×30
    INSERT INTO receiving_records (tenant_id, po_id, warehouse_id, material_spec_id, quantity, received_date, delivery_note, received_by, created_at)
    VALUES (v_tenant, v_po_id, v_wh_id, v_spec_id, 20, '2026-03-15', '第二批到貨，驗收合格', 'u-tpe-operator-n', '2026-03-15');

    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-200W';
    INSERT INTO receiving_records (tenant_id, po_id, warehouse_id, material_spec_id, quantity, received_date, delivery_note, received_by, created_at)
    VALUES (v_tenant, v_po_id, v_wh_id, v_spec_id, 30, '2026-03-15', '全數到貨驗收合格', 'u-tpe-operator-n', '2026-03-15');
END $$;

-- ============================================================
-- 8. 領料申請 + 出料紀錄（2 筆已完成 + 1 筆待審核）
-- ============================================================
DO $$
DECLARE
    v_tenant    TEXT := 'TENANT_A';
    v_req1_id   BIGINT;
    v_req2_id   BIGINT;
    v_inv_id    BIGINT;
    v_spec_id   BIGINT;
    v_wh_main   BIGINT;
BEGIN
    SELECT id INTO v_wh_main FROM warehouses WHERE tenant_id = v_tenant AND warehouse_code = 'WH-MAIN';

    -- 領料申請 1：已出料（維修用燈具）
    INSERT INTO issue_requests (tenant_id, request_number, repair_ticket_id, replacement_order_id, requested_by, status, created_at, updated_at)
    VALUES (v_tenant, 'IR-20260320-001', NULL, NULL, 'u-tpe-operator-n', 'ISSUED', '2026-03-20', '2026-03-21')
    RETURNING id INTO v_req1_id;

    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-150W';
    SELECT id INTO v_inv_id FROM inventory WHERE tenant_id = v_tenant AND warehouse_id = v_wh_main AND material_spec_id = v_spec_id;
    INSERT INTO issue_records (tenant_id, request_id, inventory_id, material_spec_id, quantity, issued_by, issued_at)
    VALUES (v_tenant, v_req1_id, v_inv_id, v_spec_id, 3, 'u-tpe-operator-n', '2026-03-21');

    -- 領料申請 2：已出料（控制器更換）
    INSERT INTO issue_requests (tenant_id, request_number, repair_ticket_id, replacement_order_id, requested_by, status, created_at, updated_at)
    VALUES (v_tenant, 'IR-20260401-001', NULL, NULL, 'u-tpe-operator-s', 'ISSUED', '2026-04-01', '2026-04-02')
    RETURNING id INTO v_req2_id;

    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'CTRL-NB01';
    SELECT id INTO v_inv_id FROM inventory WHERE tenant_id = v_tenant AND warehouse_id = v_wh_main AND material_spec_id = v_spec_id;
    INSERT INTO issue_records (tenant_id, request_id, inventory_id, material_spec_id, quantity, issued_by, issued_at)
    VALUES (v_tenant, v_req2_id, v_inv_id, v_spec_id, 2, 'u-tpe-operator-s', '2026-04-02');

    -- 領料申請 3：待審核
    INSERT INTO issue_requests (tenant_id, request_number, repair_ticket_id, replacement_order_id, requested_by, status, created_at, updated_at)
    VALUES (v_tenant, 'IR-20260420-001', NULL, NULL, 'u-tpe-field-01', 'PENDING', '2026-04-20', '2026-04-20');
END $$;

-- ============================================================
-- 9. 庫存盤點/調整紀錄（3 筆）
-- ============================================================
DO $$
DECLARE
    v_tenant  TEXT := 'TENANT_A';
    v_inv_id  BIGINT;
    v_wh_main BIGINT;
    v_spec_id BIGINT;
BEGIN
    SELECT id INTO v_wh_main FROM warehouses WHERE tenant_id = v_tenant AND warehouse_code = 'WH-MAIN';

    -- 盤點紀錄：LED-150W 實際盤點
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-150W';
    SELECT id INTO v_inv_id FROM inventory WHERE tenant_id = v_tenant AND warehouse_id = v_wh_main AND material_spec_id = v_spec_id;
    INSERT INTO inventory_adjustments (tenant_id, inventory_id, adjustment_type, quantity_change, reason, adjusted_by, adjusted_at)
    VALUES (v_tenant, v_inv_id, 'COUNT', -2, '季度盤點：帳面與實際差異，疑似登記錯誤', 'u-tpe-operator-n', '2026-04-01');

    -- 轉庫紀錄：從主倉庫調撥 LED-200W 到北區
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-200W';
    SELECT id INTO v_inv_id FROM inventory WHERE tenant_id = v_tenant AND warehouse_id = v_wh_main AND material_spec_id = v_spec_id;
    INSERT INTO inventory_adjustments (tenant_id, inventory_id, adjustment_type, quantity_change, reason, adjusted_by, adjusted_at)
    VALUES (v_tenant, v_inv_id, 'TRANSFER', -5, '調撥至北區分隊倉庫，支援北投區維修需求', 'u-tpe-admin', '2026-04-10');

    -- 修正紀錄：保險絲數量修正
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'FUSE-15A';
    SELECT id INTO v_inv_id FROM inventory WHERE tenant_id = v_tenant AND warehouse_id = v_wh_main AND material_spec_id = v_spec_id;
    INSERT INTO inventory_adjustments (tenant_id, inventory_id, adjustment_type, quantity_change, reason, adjusted_by, adjusted_at)
    VALUES (v_tenant, v_inv_id, 'CORRECTION', 10, '發現未入帳收料 10 只，補正庫存', 'u-tpe-operator-n', '2026-04-15');
END $$;

-- ============================================================
-- 10. 廢品處理紀錄（2 筆）
-- ============================================================
DO $$
DECLARE
    v_tenant  TEXT := 'TENANT_A';
    v_spec_id BIGINT;
BEGIN
    -- 報廢：損壞燈具
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'LED-150W';
    INSERT INTO disposal_records (tenant_id, material_spec_id, quantity, disposal_type, reason, disposed_by, disposed_at)
    VALUES (v_tenant, v_spec_id, 5, 'SCRAP', '維修汰換之損壞燈具，經檢測無法修復，送報廢處理', 'u-tpe-operator-n', '2026-03-25');

    -- 繳庫：退回良品
    SELECT id INTO v_spec_id FROM material_specs WHERE tenant_id = v_tenant AND spec_code = 'CTRL-LORA';
    INSERT INTO disposal_records (tenant_id, material_spec_id, quantity, disposal_type, reason, disposed_by, disposed_at)
    VALUES (v_tenant, v_spec_id, 2, 'RETURN_WAREHOUSE', '北區分隊退回多領之控制器，經測試功能正常繳庫', 'u-tpe-operator-n', '2026-04-05');
END $$;
