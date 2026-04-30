-- ============================================================
-- V78: alert_configs（示警設定）
-- ============================================================

CREATE TABLE alert_configs (
    id           BIGSERIAL   PRIMARY KEY,
    tenant_id    VARCHAR(50) NOT NULL REFERENCES tenant(tenant_id),
    config_type  VARCHAR(30) NOT NULL,
    area_scope   JSONB       DEFAULT '{}',
    config_value JSONB       NOT NULL,
    updated_at   TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, config_type, area_scope)
);
