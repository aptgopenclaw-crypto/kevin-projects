-- ============================================================
-- V72: telemetry 時序資料表（普通表 + 索引，hypertable 延至 7c）
-- ============================================================

CREATE TABLE telemetry (
    id           BIGSERIAL    PRIMARY KEY,
    time         TIMESTAMPTZ  NOT NULL,
    tenant_id    VARCHAR(50)  NOT NULL,
    device_id    BIGINT       NOT NULL REFERENCES devices(id),
    format_id    BIGINT       REFERENCES telemetry_formats(id),
    payload      JSONB        NOT NULL,
    quality_flag VARCHAR(10)  DEFAULT 'OK'
);

CREATE INDEX idx_telemetry_device_time  ON telemetry (device_id, time DESC);
CREATE INDEX idx_telemetry_tenant_time  ON telemetry (tenant_id, time DESC);
CREATE INDEX idx_telemetry_payload      ON telemetry USING GIN (payload);
