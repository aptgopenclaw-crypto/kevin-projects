-- ============================================================
-- V52: Create tender_mail_recipient table
--
-- 儲存各租戶的招標/決標日報收件人 email。
-- 取代原本寫在 application.yml 的靜態 recipients 設定。
-- ============================================================

CREATE TABLE tender_mail_recipient (
    id          BIGSERIAL       PRIMARY KEY,
    tenant_id   VARCHAR(50)     NOT NULL,
    email       VARCHAR(255)    NOT NULL,
    name        VARCHAR(100),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uq_tender_mail_recipient UNIQUE (tenant_id, email)
);

CREATE INDEX idx_tender_mail_recipient_tenant ON tender_mail_recipient(tenant_id);
CREATE INDEX idx_tender_mail_recipient_active ON tender_mail_recipient(tenant_id, is_active);
