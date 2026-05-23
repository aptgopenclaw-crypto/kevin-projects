-- V44__Tender_add_tenant_id.sql
-- 為招標/決標相關表加入 tenant_id 欄位，實現多租戶資料隔離。
-- ============================================================

-- ── 1. announcement_search_keywords ─────────────────────────────────────────
ALTER TABLE announcement_search_keywords
    ADD COLUMN tenant_id VARCHAR(50);

-- 預設填入系統預設租戶（可依實際需要調整）
UPDATE announcement_search_keywords SET tenant_id = 'DEFAULT' WHERE tenant_id IS NULL;

ALTER TABLE announcement_search_keywords
    ALTER COLUMN tenant_id SET NOT NULL;

-- 移除舊的 unique constraint，改為含 tenant_id 的新 constraint
ALTER TABLE announcement_search_keywords DROP CONSTRAINT IF EXISTS uq_ann_kw;
ALTER TABLE announcement_search_keywords
    ADD CONSTRAINT uq_ann_kw UNIQUE (tenant_id, solution, keyword);

CREATE INDEX idx_ann_kw_tenant ON announcement_search_keywords(tenant_id);

-- ── 2. announcement_agency_filters ──────────────────────────────────────────
ALTER TABLE announcement_agency_filters
    ADD COLUMN tenant_id VARCHAR(50);

UPDATE announcement_agency_filters SET tenant_id = 'DEFAULT' WHERE tenant_id IS NULL;

ALTER TABLE announcement_agency_filters
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE announcement_agency_filters DROP CONSTRAINT IF EXISTS uq_ann_af;
ALTER TABLE announcement_agency_filters
    ADD CONSTRAINT uq_ann_af UNIQUE (tenant_id, solution, agency_keyword);

CREATE INDEX idx_ann_af_tenant ON announcement_agency_filters(tenant_id);

-- ── 3. tender_announcement ──────────────────────────────────────────────────
ALTER TABLE tender_announcement
    ADD COLUMN tenant_id VARCHAR(50);

UPDATE tender_announcement SET tenant_id = 'DEFAULT' WHERE tenant_id IS NULL;

ALTER TABLE tender_announcement
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE tender_announcement DROP CONSTRAINT IF EXISTS uq_tender_ann_key;
ALTER TABLE tender_announcement
    ADD CONSTRAINT uq_tender_ann_key UNIQUE (tenant_id, solution, matched_keyword, tender_number, announcement_date);

CREATE INDEX idx_tender_ann_tenant ON tender_announcement(tenant_id);

-- ── 4. tender_award ─────────────────────────────────────────────────────────
ALTER TABLE tender_award
    ADD COLUMN tenant_id VARCHAR(50);

UPDATE tender_award SET tenant_id = 'DEFAULT' WHERE tenant_id IS NULL;

ALTER TABLE tender_award
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE tender_award DROP CONSTRAINT IF EXISTS uq_tender_award_key;
ALTER TABLE tender_award
    ADD CONSTRAINT uq_tender_award_key UNIQUE (tenant_id, solution, matched_keyword, tender_number, award_announce_date, award_announce_seq, vendor_order_seq);

CREATE INDEX idx_tender_award_tenant ON tender_award(tenant_id);
