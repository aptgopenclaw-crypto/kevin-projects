-- =====================================================
-- V57: GIS 分區範圍表 + 臺北市 12 行政區種子資料
-- 5c5: 分區範圍 + 空間查詢
-- =====================================================

-- 分區範圍表
CREATE TABLE IF NOT EXISTS taipei_streetlight.map_zones (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      VARCHAR(50)  NOT NULL,
    zone_type      VARCHAR(30)  NOT NULL,   -- ADMIN_DISTRICT / SQUAD / TAIPOWER / VENDOR
    zone_code      VARCHAR(50)  NOT NULL,
    zone_name      VARCHAR(100) NOT NULL,
    geom           geometry(Polygon, 4326) NOT NULL,
    properties     JSONB        DEFAULT '{}',
    created_at     TIMESTAMPTZ  DEFAULT NOW(),
    CONSTRAINT uk_map_zones_tenant_type_code UNIQUE (tenant_id, zone_type, zone_code)
);

CREATE INDEX idx_map_zones_geom ON taipei_streetlight.map_zones USING gist(geom);
CREATE INDEX idx_map_zones_tenant_type ON taipei_streetlight.map_zones (tenant_id, zone_type);

-- =====================================================
-- 臺北市 12 行政區 簡化邊界 (WGS84, EPSG:4326)
-- 來源: 政府開放資料 + 簡化為代表性邊界
-- =====================================================

INSERT INTO taipei_streetlight.map_zones (tenant_id, zone_type, zone_code, zone_name, geom, properties)
VALUES
-- 松山區
('TAIPEI', 'ADMIN_DISTRICT', 'songshan', '松山區',
 ST_GeomFromText('POLYGON((121.5500 25.0500, 121.5700 25.0500, 121.5700 25.0620, 121.5500 25.0620, 121.5500 25.0500))', 4326),
 '{"population": 204000, "area_km2": 9.29}'),

-- 信義區
('TAIPEI', 'ADMIN_DISTRICT', 'xinyi', '信義區',
 ST_GeomFromText('POLYGON((121.5550 25.0200, 121.5850 25.0200, 121.5850 25.0450, 121.5550 25.0450, 121.5550 25.0200))', 4326),
 '{"population": 225000, "area_km2": 11.21}'),

-- 大安區
('TAIPEI', 'ADMIN_DISTRICT', 'daan', '大安區',
 ST_GeomFromText('POLYGON((121.5250 25.0200, 121.5550 25.0200, 121.5550 25.0420, 121.5250 25.0420, 121.5250 25.0200))', 4326),
 '{"population": 309000, "area_km2": 11.36}'),

-- 中山區
('TAIPEI', 'ADMIN_DISTRICT', 'zhongshan', '中山區',
 ST_GeomFromText('POLYGON((121.5150 25.0500, 121.5500 25.0500, 121.5500 25.0800, 121.5150 25.0800, 121.5150 25.0500))', 4326),
 '{"population": 227000, "area_km2": 13.68}'),

-- 中正區
('TAIPEI', 'ADMIN_DISTRICT', 'zhongzheng', '中正區',
 ST_GeomFromText('POLYGON((121.5050 25.0200, 121.5350 25.0200, 121.5350 25.0450, 121.5050 25.0450, 121.5050 25.0200))', 4326),
 '{"population": 159000, "area_km2": 7.61}'),

-- 大同區
('TAIPEI', 'ADMIN_DISTRICT', 'datong', '大同區',
 ST_GeomFromText('POLYGON((121.5050 25.0500, 121.5250 25.0500, 121.5250 25.0700, 121.5050 25.0700, 121.5050 25.0500))', 4326),
 '{"population": 126000, "area_km2": 5.68}'),

-- 萬華區
('TAIPEI', 'ADMIN_DISTRICT', 'wanhua', '萬華區',
 ST_GeomFromText('POLYGON((121.4850 25.0200, 121.5150 25.0200, 121.5150 25.0500, 121.4850 25.0500, 121.4850 25.0200))', 4326),
 '{"population": 189000, "area_km2": 8.85}'),

-- 文山區
('TAIPEI', 'ADMIN_DISTRICT', 'wenshan', '文山區',
 ST_GeomFromText('POLYGON((121.5400 24.9800, 121.5900 24.9800, 121.5900 25.0200, 121.5400 25.0200, 121.5400 24.9800))', 4326),
 '{"population": 271000, "area_km2": 31.51}'),

-- 南港區
('TAIPEI', 'ADMIN_DISTRICT', 'nangang', '南港區',
 ST_GeomFromText('POLYGON((121.5700 25.0300, 121.6100 25.0300, 121.6100 25.0600, 121.5700 25.0600, 121.5700 25.0300))', 4326),
 '{"population": 120000, "area_km2": 21.84}'),

-- 內湖區
('TAIPEI', 'ADMIN_DISTRICT', 'neihu', '內湖區',
 ST_GeomFromText('POLYGON((121.5600 25.0600, 121.6100 25.0600, 121.6100 25.0950, 121.5600 25.0950, 121.5600 25.0600))', 4326),
 '{"population": 287000, "area_km2": 31.58}'),

-- 士林區
('TAIPEI', 'ADMIN_DISTRICT', 'shilin', '士林區',
 ST_GeomFromText('POLYGON((121.5000 25.0800, 121.5700 25.0800, 121.5700 25.1300, 121.5000 25.1300, 121.5000 25.0800))', 4326),
 '{"population": 288000, "area_km2": 62.37}'),

-- 北投區
('TAIPEI', 'ADMIN_DISTRICT', 'beitou', '北投區',
 ST_GeomFromText('POLYGON((121.4700 25.1100, 121.5400 25.1100, 121.5400 25.1600, 121.4700 25.1600, 121.4700 25.1100))', 4326),
 '{"population": 256000, "area_km2": 56.82}');
