-- ============================================================
-- V34: 種子資料 — 每個單位 20 座路燈（燈桿 + 燈具 + 控制器）
-- 共 6 單位 × 20 座 = 120 座路燈 = 360 筆設備
-- 另含 1 筆契約、12 條回路（每單位 2 條）
-- ============================================================

SET search_path TO ${flyway:defaultSchema}, public;

-- ============================================================
-- 1. 契約
-- ============================================================
INSERT INTO contracts (tenant_id, contract_code, contract_name, budget_year,
                       procurement_number, contractor_name, contractor_contact,
                       asset_category, quantity, start_date, end_date,
                       warranty_years, status, created_by, created_at, updated_at)
VALUES
    ('TENANT_A', 'C-114-001', '114年度臺北市路燈維護管理契約', 114,
     'P-114-00123', '光明照明股份有限公司', '王經理 02-2771-0001',
     'STREETLIGHT', 5000, '2025-01-01', '2025-12-31',
     1, 'ACTIVE', 'system', now(), now()),
    ('TENANT_A', 'C-114-002', '114年度智慧路燈控制器佈建契約', 114,
     'P-114-00456', '遠傳電信股份有限公司', '陳專案 02-7723-5000',
     'CONTROLLER', 2000, '2025-03-01', '2026-02-28',
     2, 'ACTIVE', 'system', now(), now());

-- ============================================================
-- 2. 回路（每單位 2 條，共 12 條）
-- ============================================================
INSERT INTO circuits (tenant_id, circuit_number, circuit_name,
                      taipower_account, usage_type, status, created_at, updated_at)
VALUES
    -- 公燈處 (dept 5)
    ('TENANT_A', 'CKT-HQ-A', '公燈處本部A迴路', '01-1001-0001', 'LIGHTING', 'ACTIVE', now(), now()),
    ('TENANT_A', 'CKT-HQ-B', '公燈處本部B迴路', '01-1001-0002', 'LIGHTING', 'ACTIVE', now(), now()),
    -- 第一分隊 北區 (dept 6)
    ('TENANT_A', 'CKT-N-A', '北區第一分隊A迴路', '02-2001-0001', 'LIGHTING', 'ACTIVE', now(), now()),
    ('TENANT_A', 'CKT-N-B', '北區第一分隊B迴路', '02-2001-0002', 'LIGHTING', 'ACTIVE', now(), now()),
    -- 第二分隊 南區 (dept 7)
    ('TENANT_A', 'CKT-S-A', '南區第二分隊A迴路', '03-3001-0001', 'LIGHTING', 'ACTIVE', now(), now()),
    ('TENANT_A', 'CKT-S-B', '南區第二分隊B迴路', '03-3001-0002', 'LIGHTING', 'ACTIVE', now(), now()),
    -- 工程股 (dept 8)
    ('TENANT_A', 'CKT-ENG-A', '工程股A迴路', '04-4001-0001', 'LIGHTING', 'ACTIVE', now(), now()),
    ('TENANT_A', 'CKT-ENG-B', '工程股B迴路', '04-4001-0002', 'LIGHTING', 'ACTIVE', now(), now()),
    -- 行政股 (dept 9)
    ('TENANT_A', 'CKT-ADM-A', '行政股A迴路', '05-5001-0001', 'LIGHTING', 'ACTIVE', now(), now()),
    ('TENANT_A', 'CKT-ADM-B', '行政股B迴路', '05-5001-0002', 'LIGHTING', 'ACTIVE', now(), now()),
    -- 智慧路燈管理中心 (dept 10)
    ('TENANT_A', 'CKT-IOT-A', '智慧路燈管理中心A迴路', '06-6001-0001', 'LIGHTING', 'ACTIVE', now(), now()),
    ('TENANT_A', 'CKT-IOT-B', '智慧路燈管理中心B迴路', '06-6001-0002', 'LIGHTING', 'ACTIVE', now(), now());

-- ============================================================
-- 3. 設備種子（DO block：每單位 20 座路燈 × 3 元件）
-- ============================================================
DO $$
DECLARE
    v_tenant       TEXT := 'TENANT_A';
    v_contract_id  BIGINT;
    v_circuit_a_id BIGINT;
    v_circuit_b_id BIGINT;
    v_pole_id      BIGINT;
    v_dept         RECORD;
    v_seq          INT;
    v_pole_code    TEXT;
    v_lng          NUMERIC(11,7);
    v_lat          NUMERIC(10,7);
    v_twd97_x      NUMERIC(12,3);
    v_twd97_y      NUMERIC(12,3);
    v_height       NUMERIC;
    v_wattage      INT;
    v_color_temp   INT;
    v_arm_count    INT;
BEGIN
    -- 取得契約 ID
    SELECT id INTO v_contract_id
    FROM contracts WHERE tenant_id = v_tenant AND contract_code = 'C-114-001';

    -- 逐單位處理
    FOR v_dept IN
        SELECT * FROM (VALUES
            -- dept_id, prefix, dept_name_short, base_lng,     base_lat,     base_twd97_x, base_twd97_y
            (5,  'HQ',  '公燈處',       121.5150000, 25.0350000, 302950.000, 2770850.000),
            (6,  'N',   '北區',         121.5600000, 25.0500000, 307450.000, 2772550.000),
            (7,  'S',   '南區',         121.5400000, 25.0100000, 305450.000, 2768100.000),
            (8,  'ENG', '工程',         121.5100000, 25.0280000, 302450.000, 2770050.000),
            (9,  'ADM', '行政',         121.5300000, 25.0400000, 304450.000, 2771400.000),
            (10, 'IOT', '智慧中心',     121.5680000, 25.0330000, 308250.000, 2770650.000)
        ) AS t(dept_id, prefix, name_short, base_lng, base_lat, base_x, base_y)
    LOOP
        -- 取得該單位的兩條迴路 ID
        SELECT id INTO v_circuit_a_id
        FROM circuits WHERE tenant_id = v_tenant AND circuit_number = 'CKT-' || v_dept.prefix || '-A';

        SELECT id INTO v_circuit_b_id
        FROM circuits WHERE tenant_id = v_tenant AND circuit_number = 'CKT-' || v_dept.prefix || '-B';

        FOR v_seq IN 1..20 LOOP
            -- 座標微調（在基準點 ±0.005° 範圍內散布，約 500m）
            v_lng     := v_dept.base_lng + (random() - 0.5) * 0.010;
            v_lat     := v_dept.base_lat + (random() - 0.5) * 0.010;
            v_twd97_x := v_dept.base_x  + (random() - 0.5) * 1000;
            v_twd97_y := v_dept.base_y  + (random() - 0.5) * 1000;

            -- 隨機化燈桿屬性
            v_height     := (ARRAY[8, 10, 12, 15])[1 + floor(random() * 4)::int];
            v_wattage    := (ARRAY[100, 150, 200, 250])[1 + floor(random() * 4)::int];
            v_color_temp := (ARRAY[3000, 4000, 5000])[1 + floor(random() * 3)::int];
            v_arm_count  := CASE WHEN random() < 0.2 THEN 2 ELSE 1 END;

            v_pole_code := 'SL-' || v_dept.prefix || '-' || LPAD(v_seq::TEXT, 3, '0');

            -- ── 燈桿 (POLE) ──
            INSERT INTO devices (
                tenant_id, device_type, device_code, device_name,
                twd97_x, twd97_y, lng, lat, elevation,
                dept_id, contract_id, property_owner,
                status, installed_at,
                parent_device_id, mount_position, connectivity_type,
                circuit_id, attributes, created_by, created_at, updated_at
            ) VALUES (
                v_tenant, 'POLE', v_pole_code,
                v_dept.name_short || '路燈' || LPAD(v_seq::TEXT, 3, '0'),
                v_twd97_x, v_twd97_y, v_lng, v_lat,
                (5 + random() * 50)::NUMERIC(8,3),
                v_dept.dept_id, v_contract_id, '臺北市政府',
                'ACTIVE', CURRENT_DATE - (random() * 1000)::INT,
                NULL, NULL, 'NONE',
                CASE WHEN v_seq <= 10 THEN v_circuit_a_id ELSE v_circuit_b_id END,
                jsonb_build_object(
                    'height_m',        v_height,
                    'material',        CASE WHEN random() < 0.7 THEN '鍍鋅鋼' ELSE '鋁合金' END,
                    'arm_count',       v_arm_count,
                    'arm_length_m',    CASE WHEN v_arm_count = 1 THEN 2.5 ELSE 2.0 END,
                    'foundation_type', '混凝土基座',
                    'road_name',       (ARRAY[
                        '仁愛路四段', '忠孝東路五段', '信義路三段', '和平東路一段',
                        '復興南路一段', '敦化北路', '南京東路三段', '民生東路四段',
                        '八德路三段', '基隆路一段', '羅斯福路三段', '新生南路一段',
                        '建國南路二段', '光復南路', '松仁路', '市府路',
                        '松德路', '松壽路', '松智路', '永吉路'
                    ])[v_seq]
                ),
                'system', now(), now()
            ) RETURNING id INTO v_pole_id;

            -- ── 燈具 (LUMINAIRE) ──
            INSERT INTO devices (
                tenant_id, device_type, device_code, device_name,
                twd97_x, twd97_y, lng, lat,
                dept_id, contract_id,
                status, installed_at,
                parent_device_id, mount_position, connectivity_type,
                circuit_id, attributes, created_by, created_at, updated_at
            ) VALUES (
                v_tenant, 'LUMINAIRE',
                'LM-' || v_dept.prefix || '-' || LPAD(v_seq::TEXT, 3, '0'),
                v_wattage || 'W LED燈具',
                v_twd97_x, v_twd97_y, v_lng, v_lat,
                v_dept.dept_id, v_contract_id,
                'ACTIVE', CURRENT_DATE - (random() * 500)::INT,
                v_pole_id, 'ARM_1', 'NONE',
                CASE WHEN v_seq <= 10 THEN v_circuit_a_id ELSE v_circuit_b_id END,
                jsonb_build_object(
                    'wattage',       v_wattage,
                    'color_temp_k',  v_color_temp,
                    'light_source',  'LED',
                    'brand',         (ARRAY['飛利浦', '歐司朗', '台達電', '東貝光電'])[1 + floor(random() * 4)::int],
                    'model',         (ARRAY['BRP392', 'STREETLIGHT-200', 'SL-PRO-150', 'TL-LED-250'])[1 + floor(random() * 4)::int],
                    'luminous_flux_lm', v_wattage * 130,
                    'ip_rating',     'IP66',
                    'beam_angle',    (ARRAY[60, 90, 120])[1 + floor(random() * 3)::int]
                ),
                'system', now(), now()
            );

            -- ── 控制器 (CONTROLLER) ──
            INSERT INTO devices (
                tenant_id, device_type, device_code, device_name,
                twd97_x, twd97_y, lng, lat,
                dept_id, contract_id,
                status, installed_at,
                parent_device_id, mount_position, connectivity_type,
                network_config,
                circuit_id, attributes, created_by, created_at, updated_at
            ) VALUES (
                v_tenant, 'CONTROLLER',
                'CT-' || v_dept.prefix || '-' || LPAD(v_seq::TEXT, 3, '0'),
                '智慧路燈控制器',
                v_twd97_x, v_twd97_y, v_lng, v_lat,
                v_dept.dept_id, v_contract_id,
                'ACTIVE', CURRENT_DATE - (random() * 300)::INT,
                v_pole_id, 'CTRL_1',
                (ARRAY['DIRECT', 'GATEWAY'])[1 + floor(random() * 2)::int]::VARCHAR,
                jsonb_build_object(
                    'protocol',  (ARRAY['NB-IoT', 'LoRa', '4G'])[1 + floor(random() * 3)::int],
                    'ip_address', '10.' || (floor(random()*254)+1)::INT || '.' || (floor(random()*254)+1)::INT || '.' || (floor(random()*254)+1)::INT,
                    'port',       (ARRAY[8080, 8443, 502, 5683])[1 + floor(random() * 4)::int]
                ),
                CASE WHEN v_seq <= 10 THEN v_circuit_a_id ELSE v_circuit_b_id END,
                jsonb_build_object(
                    'manufacturer',  (ARRAY['遠傳電信', '中華電信', '研華科技'])[1 + floor(random() * 3)::int],
                    'model',         (ARRAY['FET-SC200', 'CHT-SLC100', 'WISE-4610'])[1 + floor(random() * 3)::int],
                    'firmware_ver',  '2.' || (floor(random() * 5))::INT || '.' || (floor(random() * 10))::INT,
                    'sim_iccid',     '8988' || LPAD((floor(random() * 10000000000)::BIGINT)::TEXT, 16, '0'),
                    'dimming_support', random() < 0.8,
                    'schedule_enabled', true
                ),
                'system', now(), now()
            );

        END LOOP; -- v_seq
    END LOOP; -- v_dept

    RAISE NOTICE 'Seed complete: % devices inserted',
        (SELECT count(*) FROM devices WHERE tenant_id = v_tenant);
END;
$$;

-- ============================================================
-- 4. 驗證統計
-- ============================================================
-- 預期：120 POLE + 120 LUMINAIRE + 120 CONTROLLER = 360 devices
-- SELECT device_type, count(*) FROM devices GROUP BY device_type ORDER BY device_type;
-- SELECT d.dept_id, di.dept_name, count(*) FROM devices d JOIN dept_info di ON di.dept_id = d.dept_id GROUP BY d.dept_id, di.dept_name ORDER BY d.dept_id;
