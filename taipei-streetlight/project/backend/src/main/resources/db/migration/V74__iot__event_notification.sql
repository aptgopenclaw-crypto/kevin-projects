-- ============================================================
-- V74: event_notification_targets + event_notification_channels
-- ============================================================

CREATE TABLE event_notification_targets (
    id          BIGSERIAL    PRIMARY KEY,
    rule_id     BIGINT       NOT NULL REFERENCES event_rules(id) ON DELETE CASCADE,
    target_type VARCHAR(20)  NOT NULL,
    target_id   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE event_notification_channels (
    id         BIGSERIAL   PRIMARY KEY,
    rule_id    BIGINT      NOT NULL REFERENCES event_rules(id) ON DELETE CASCADE,
    channel    VARCHAR(20) NOT NULL,
    config     JSONB       DEFAULT '{}',
    enabled    BOOLEAN     NOT NULL DEFAULT true,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE(rule_id, channel)
);
