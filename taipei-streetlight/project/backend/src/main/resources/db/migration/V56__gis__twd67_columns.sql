-- V56: Add TWD67 coordinate columns for 台電坐標 conversion support
-- Decision: New twd67_x/twd67_y numeric columns alongside existing twd97_x/twd97_y
-- Keep taipower_coord as original 台電 reference code

-- 1. Ensure EPSG:3826 (TWD97 TM2 Zone 121) and EPSG:3828 (TWD67 TM2 Zone 121) are in spatial_ref_sys
INSERT INTO spatial_ref_sys (srid, auth_name, auth_srid, proj4text, srtext)
VALUES (3826, 'EPSG', 3826,
  '+proj=tmerc +lat_0=0 +lon_0=121 +k=0.9999 +x_0=250000 +y_0=0 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs',
  'PROJCS["TWD97 / TM2 zone 121",GEOGCS["TWD97",DATUM["Taiwan_Datum_1997",SPHEROID["GRS 1980",6378137,298.257222101]],PRIMEM["Greenwich",0],UNIT["degree",0.0174532925199433]],PROJECTION["Transverse_Mercator"],PARAMETER["latitude_of_origin",0],PARAMETER["central_meridian",121],PARAMETER["scale_factor",0.9999],PARAMETER["false_easting",250000],PARAMETER["false_northing",0],UNIT["metre",1]]')
ON CONFLICT (srid) DO NOTHING;

INSERT INTO spatial_ref_sys (srid, auth_name, auth_srid, proj4text, srtext)
VALUES (3828, 'EPSG', 3828,
  '+proj=tmerc +lat_0=0 +lon_0=121 +k=0.9999 +x_0=250000 +y_0=0 +ellps=aust_SA +towgs84=-752,-358,-179,-0.0000011698,0.0000018398,0.0000009822,0.00002329 +units=m +no_defs',
  'PROJCS["TWD67 / TM2 zone 121",GEOGCS["TWD67",DATUM["Taiwan_Datum_1967",SPHEROID["Australian National Spheroid",6378160,298.25]],PRIMEM["Greenwich",0],UNIT["degree",0.0174532925199433]],PROJECTION["Transverse_Mercator"],PARAMETER["latitude_of_origin",0],PARAMETER["central_meridian",121],PARAMETER["scale_factor",0.9999],PARAMETER["false_easting",250000],PARAMETER["false_northing",0],UNIT["metre",1]]')
ON CONFLICT (srid) DO NOTHING;

-- 2. Add TWD67 coordinate columns
ALTER TABLE taipei_streetlight.devices
    ADD COLUMN IF NOT EXISTS twd67_x NUMERIC(12,3),
    ADD COLUMN IF NOT EXISTS twd67_y NUMERIC(12,3);

-- 3. Backfill TWD67 from WGS84 (lng/lat) where available
UPDATE taipei_streetlight.devices
SET twd67_x = ST_X(ST_Transform(ST_SetSRID(ST_MakePoint(lng, lat), 4326), 3828)),
    twd67_y = ST_Y(ST_Transform(ST_SetSRID(ST_MakePoint(lng, lat), 4326), 3828))
WHERE lng IS NOT NULL AND lat IS NOT NULL AND twd67_x IS NULL;

-- 4. Backfill TWD97 from WGS84 where TWD97 is missing
UPDATE taipei_streetlight.devices
SET twd97_x = ST_X(ST_Transform(ST_SetSRID(ST_MakePoint(lng, lat), 4326), 3826)),
    twd97_y = ST_Y(ST_Transform(ST_SetSRID(ST_MakePoint(lng, lat), 4326), 3826))
WHERE lng IS NOT NULL AND lat IS NOT NULL AND twd97_x IS NULL;
