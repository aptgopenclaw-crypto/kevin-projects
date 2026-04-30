-- ============================================================
-- V75: alert_history（含狀態機 OPEN → ACKNOWLEDGED → RESOLVED）
-- ============================================================

CREATE TABLE alert_history (
    id                BIGSERIAL    PRIMARY KEY,
    tenant_id         VARCHAR(50)  NOT NULL REFERENCES tenant(tenant_id),
    rule_id           BIGINT       REFERENCES event_rules(id),
    device_id         BIGINT       REFERENCES devices(id),
    severity          VARCHAR(10)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    message           TEXT         NOT NULL,
    triggered_values  JSONB,
    triggered_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ack_by            VARCHAR(50),
    ack_at            TIMESTAMPTZ,
    resolved_at       TIMESTAMPTZ,
    mttr_minutes      INT,
    fault_ticket_id   BIGINT       REFERENCES fault_tickets(id),
    notification_sent BOOLEAN      DEFAULT false
);

CREATE INDEX idx_alert_history_status ON alert_history (tenant_id, status, triggered_at DESC);
CREATE INDEX idx_alert_history_device ON alert_history (device_id, triggered_at DESC);
