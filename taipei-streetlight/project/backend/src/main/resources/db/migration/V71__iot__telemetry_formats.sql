-- ============================================================
-- V71: telemetry_formats + devices.format_id FK
-- ============================================================

CREATE TABLE telemetry_formats (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         VARCHAR(50)  NOT NULL REFERENCES tenant(tenant_id),
    vendor_name       VARCHAR(100) NOT NULL,
    device_model      VARCHAR(100) NOT NULL,
    version           INT          NOT NULL DEFAULT 1,
    field_definitions JSONB        NOT NULL,
    sample_payload    JSONB,
    description       TEXT,
    enabled           BOOLEAN      NOT NULL DEFAULT true,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, vendor_name, device_model, version)
);

-- devices 表補 format_id FK（V70 已預留註解）
ALTER TABLE devices ADD COLUMN IF NOT EXISTS format_id BIGINT REFERENCES telemetry_formats(id);
