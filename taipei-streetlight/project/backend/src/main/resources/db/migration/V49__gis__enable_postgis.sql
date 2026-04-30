-- =============================================================
-- V49: Enable PostGIS + add geometry columns to devices
-- =============================================================
-- Phase 5C GIS 基礎建設
-- ADR-003: 採用開源 GIS 方案 (PostGIS + OpenLayers)
-- =============================================================

-- 0. Ensure PostGIS functions are accessible (include taipei_streetlight for shared PostGIS types)
SET search_path TO ${flyway:defaultSchema}, taipei_streetlight, public;

-- 1. Enable PostGIS extension (no-op if already installed in another schema)
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Add WGS84 geometry column (SRID 4326) for spatial queries
ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS geom geometry(Point, 4326);

-- 3. Populate geometry from existing lng/lat data
UPDATE devices
SET geom = ST_SetSRID(ST_MakePoint(lng::double precision, lat::double precision), 4326)
WHERE lng IS NOT NULL AND lat IS NOT NULL AND geom IS NULL;

-- 4. Create spatial index (GiST)
CREATE INDEX IF NOT EXISTS idx_devices_geom
    ON devices USING GIST (geom);

-- 5. Create trigger function to auto-sync geom when lng/lat changes
CREATE OR REPLACE FUNCTION fn_devices_sync_geom()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.lng IS NOT NULL AND NEW.lat IS NOT NULL THEN
        NEW.geom := ST_SetSRID(ST_MakePoint(NEW.lng::double precision, NEW.lat::double precision), 4326);
    ELSE
        NEW.geom := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 6. Attach trigger on INSERT and UPDATE
DROP TRIGGER IF EXISTS trg_devices_sync_geom ON devices;
CREATE TRIGGER trg_devices_sync_geom
    BEFORE INSERT OR UPDATE OF lng, lat ON devices
    FOR EACH ROW
    EXECUTE FUNCTION fn_devices_sync_geom();
