-- ============================================================
-- V76: alert_notification_log（通知發送紀錄）
-- ============================================================

CREATE TABLE alert_notification_log (
    id            BIGSERIAL    PRIMARY KEY,
    alert_id      BIGINT       NOT NULL REFERENCES alert_history(id),
    channel       VARCHAR(20)  NOT NULL,
    recipient     VARCHAR(200) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    error_message TEXT,
    sent_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_alert_notif_log_alert ON alert_notification_log (alert_id);
