-- =============================================
-- V48: Notification engine tables
-- FN-00-014 ~ FN-00-019, SRS-02-010
-- =============================================

SET search_path TO ${flyway:defaultSchema}, public;

-- 通知主表
CREATE TABLE notifications (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       VARCHAR(50)     NOT NULL,
    user_id         VARCHAR(50)     NOT NULL,
    type            VARCHAR(20)     NOT NULL,       -- TODO / ALERT / INFO
    title           VARCHAR(200)    NOT NULL,
    content         VARCHAR(2000),
    ref_type        VARCHAR(50),                    -- FAULT / REPAIR / REPLACEMENT / WORKFLOW / ANNOUNCEMENT / MATERIAL
    ref_id          VARCHAR(50),
    read            BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE
);

-- 查詢索引：用戶未讀通知（最常用場景）
CREATE INDEX idx_notifications_user_read
    ON notifications (user_id, read, created_at DESC);

-- 多租戶過濾
CREATE INDEX idx_notifications_tenant
    ON notifications (tenant_id);

-- 用戶通知偏好欄位
ALTER TABLE users
    ADD COLUMN notify_email_flag BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN notify_sms_flag   BOOLEAN NOT NULL DEFAULT TRUE;
