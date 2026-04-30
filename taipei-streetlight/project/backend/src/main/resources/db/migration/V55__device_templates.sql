-- =====================================================
-- V55: device_templates — 設備動態欄位 Schema 定義
-- =====================================================

CREATE TABLE device_templates (
    id          BIGSERIAL       PRIMARY KEY,
    tenant_id   VARCHAR(50)     NOT NULL REFERENCES tenant(tenant_id),
    device_type VARCHAR(30)     NOT NULL,
    schema      JSONB           NOT NULL DEFAULT '{"fields":[]}',
    version     INTEGER         NOT NULL DEFAULT 1,
    created_by  VARCHAR(50),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ,
    UNIQUE (tenant_id, device_type)
);

COMMENT ON TABLE device_templates IS '設備類型動態欄位 Schema 定義';
COMMENT ON COLUMN device_templates.schema IS '欄位定義 JSON: { fields: [{ key, title, type, required, ... }] }';

-- ── 預設種子資料 (TENANT_A) ──

INSERT INTO device_templates (tenant_id, device_type, schema, created_by) VALUES

-- POLE（燈桿）
('TENANT_A', 'POLE', '{
  "fields": [
    { "key": "height",      "title": "桿高(m)",   "type": "number",  "required": true,  "minimum": 0, "maximum": 30 },
    { "key": "material",    "title": "材質",       "type": "select",  "required": true,  "options": ["鋼管","鋁合金","木桿","水泥桿","複合材料"] },
    { "key": "armCount",    "title": "燈臂數",     "type": "number",  "required": false, "minimum": 0, "maximum": 6 },
    { "key": "armLength",   "title": "臂長(m)",    "type": "number",  "required": false, "minimum": 0 },
    { "key": "formFactor",  "title": "桿型",       "type": "select",  "required": false, "options": ["直桿","弧型桿","T型桿","Y型桿"] },
    { "key": "baseType",    "title": "基座類型",   "type": "select",  "required": false, "options": ["法蘭","直埋","側壁"] }
  ]
}', 'SYSTEM'),

-- LUMINAIRE（燈具）
('TENANT_A', 'LUMINAIRE', '{
  "fields": [
    { "key": "wattage",      "title": "瓦數(W)",       "type": "number",  "required": true,  "minimum": 0, "maximum": 1000 },
    { "key": "colorTemp",    "title": "色溫(K)",       "type": "number",  "required": true,  "minimum": 2000, "maximum": 7000 },
    { "key": "brand",        "title": "品牌",          "type": "text",    "required": false },
    { "key": "model",        "title": "型號",          "type": "text",    "required": false },
    { "key": "ratedLumens",  "title": "額定光通量(lm)", "type": "number",  "required": false, "minimum": 0 },
    { "key": "controlType",  "title": "控制方式",       "type": "select",  "required": false, "options": ["電子","傳統","智慧"] }
  ]
}', 'SYSTEM'),

-- PANEL_BOX（分電箱）
('TENANT_A', 'PANEL_BOX', '{
  "fields": [
    { "key": "capacity",      "title": "容量",       "type": "text",   "required": true },
    { "key": "breakerCount",  "title": "斷路器數",   "type": "number", "required": false, "minimum": 0, "maximum": 50 },
    { "key": "voltageLevel",  "title": "電壓等級",   "type": "select", "required": false, "options": ["110V","220V","380V"] },
    { "key": "enclosureType", "title": "箱體類型",   "type": "select", "required": false, "options": ["壁掛式","落地式","桿掛式"] }
  ]
}', 'SYSTEM'),

-- CONTROLLER（控制器）
('TENANT_A', 'CONTROLLER', '{
  "fields": [
    { "key": "protocol",   "title": "通訊協定",   "type": "select", "required": true,  "options": ["DALI","0-10V","Zigbee","LoRa","NB-IoT"] },
    { "key": "firmware",   "title": "韌體版本",   "type": "text",   "required": false },
    { "key": "simIccid",   "title": "SIM ICCID",  "type": "text",   "required": false },
    { "key": "ipAddress",  "title": "IP 位址",    "type": "text",   "required": false }
  ]
}', 'SYSTEM'),

-- POWER_EQUIPMENT（電力設備）
('TENANT_A', 'POWER_EQUIPMENT', '{
  "fields": [
    { "key": "ratedPower",   "title": "額定功率(kVA)", "type": "number", "required": false, "minimum": 0 },
    { "key": "phaseType",    "title": "相數",          "type": "select", "required": false, "options": ["單相","三相"] },
    { "key": "meterNumber",  "title": "電錶編號",      "type": "text",   "required": false }
  ]
}', 'SYSTEM'),

-- ATTACHMENT（附掛物）
('TENANT_A', 'ATTACHMENT', '{
  "fields": [
    { "key": "attachType",   "title": "附掛類型",  "type": "select", "required": true,  "options": ["CCTV","WiFi AP","環境感測器","廣告旗幟","充電樁","其他"] },
    { "key": "ownerUnit",    "title": "所屬單位",  "type": "text",   "required": false },
    { "key": "contractNo",   "title": "合約編號",  "type": "text",   "required": false }
  ]
}', 'SYSTEM');
