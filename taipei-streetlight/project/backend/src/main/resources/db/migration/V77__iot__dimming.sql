-- ============================================================
-- V77: dimming_groups + dimming_schedules + dimming_logs
-- ============================================================

CREATE TABLE dimming_groups (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  VARCHAR(50)  NOT NULL REFERENCES tenant(tenant_id),
    group_name VARCHAR(200) NOT NULL,
    device_ids BIGINT[]     NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE dimming_schedules (
    id             BIGSERIAL    PRIMARY KEY,
    tenant_id      VARCHAR(50)  NOT NULL REFERENCES tenant(tenant_id),
    schedule_name  VARCHAR(200) NOT NULL,
    target_type    VARCHAR(20)  NOT NULL,
    target_id      BIGINT,
    brightness_pct INT          NOT NULL,
    schedule_cron  VARCHAR(100),
    one_time_at    TIMESTAMPTZ,
    enabled        BOOLEAN      DEFAULT true,
    created_at     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE dimming_logs (
    id             BIGSERIAL   PRIMARY KEY,
    tenant_id      VARCHAR(50) NOT NULL REFERENCES tenant(tenant_id),
    device_id      BIGINT      NOT NULL REFERENCES devices(id),
    command_type   VARCHAR(20) NOT NULL,
    brightness_pct INT         NOT NULL,
    result         VARCHAR(20) NOT NULL,
    sent_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    ack_at         TIMESTAMPTZ,
    schedule_id    BIGINT      REFERENCES dimming_schedules(id)
);
