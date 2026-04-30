-- ============================================================
-- V73: event_rules + event_rule_conditions
-- ============================================================

CREATE TABLE event_rules (
    id                     BIGSERIAL    PRIMARY KEY,
    tenant_id              VARCHAR(50)  NOT NULL REFERENCES tenant(tenant_id),
    rule_name              VARCHAR(200) NOT NULL,
    description            TEXT,
    severity               VARCHAR(10)  NOT NULL,
    target_scope           JSONB        DEFAULT '{}',
    format_id              BIGINT       REFERENCES telemetry_formats(id),
    condition_logic        VARCHAR(5)   NOT NULL DEFAULT 'AND',
    suppress_duration_min  INT          DEFAULT 30,
    auto_create_ticket     BOOLEAN      DEFAULT false,
    enabled                BOOLEAN      NOT NULL DEFAULT true,
    created_at             TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE event_rule_conditions (
    id              BIGSERIAL    PRIMARY KEY,
    rule_id         BIGINT       NOT NULL REFERENCES event_rules(id) ON DELETE CASCADE,
    condition_group INT          NOT NULL DEFAULT 1,
    field           VARCHAR(100) NOT NULL,
    operator        VARCHAR(10)  NOT NULL,
    threshold_value VARCHAR(100) NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0
);
