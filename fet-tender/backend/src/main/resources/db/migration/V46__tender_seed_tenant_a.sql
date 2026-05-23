-- ============================================================
-- V46: Seed tenant TENANT_A + search keywords + update existing data
--
-- Tenant: TENANT_A (TP_ST_LIGHT / 政府採購網資訊 / CLOUD)
-- Updates existing tender data from 'DEFAULT' to 'TENANT_A'
-- ============================================================

-- ── 0. Drop legacy unique constraints (missed in V44) ────────
-- V44 only dropped 'uq_ann_kw'/'uq_ann_af'/'uq_tender_ann_key'/'uq_tender_award_key'
-- but the original auto-named constraints still exist without tenant_id.

ALTER TABLE announcement_search_keywords
    DROP CONSTRAINT IF EXISTS announcement_search_keywords_solution_keyword_key;

ALTER TABLE announcement_agency_filters
    DROP CONSTRAINT IF EXISTS announcement_agency_filters_solution_agency_keyword_key;

ALTER TABLE tender_announcement
    DROP CONSTRAINT IF EXISTS tender_announcement_solution_matched_keyword_tender_number__key;

ALTER TABLE tender_award
    DROP CONSTRAINT IF EXISTS tender_award_solution_matched_keyword_tender_number_award_a_key;

-- ── 1. Insert Tenant ─────────────────────────────────────────

INSERT INTO tenant (tenant_id, tenant_code, tenant_name, deployment_mode, enabled, create_time, update_time)
VALUES ('TENANT_A', 'TP_ST_LIGHT', '政府採購網資訊', 'CLOUD', true, '2026-05-11 09:24:25.640', '2026-05-11 09:24:26.132')
ON CONFLICT (tenant_id) DO NOTHING;

-- ── 2. Seed announcement_search_keywords for TENANT_A ────────

INSERT INTO announcement_search_keywords (tenant_id, solution, keyword, is_active, created_at, updated_at)
VALUES
  ('TENANT_A', 'CCTV', '偵測', true, NOW(), NOW()),
  ('TENANT_A', 'CCTV', '監視系統', true, NOW(), NOW()),
  ('TENANT_A', 'CCTV', '車牌', true, NOW(), NOW()),
  ('TENANT_A', 'CCTV', '辨識', true, NOW(), NOW()),
  ('TENANT_A', 'CCTV', '錄影', true, NOW(), NOW()),
  ('TENANT_A', 'ESG－微電網及儲能', '太陽', true, NOW(), NOW()),
  ('TENANT_A', 'ESG－微電網及儲能', '電網', true, NOW(), NOW()),
  ('TENANT_A', 'ESG－淨零及節能', '低碳', true, NOW(), NOW()),
  ('TENANT_A', 'ESG－淨零及節能', '冰水主機', true, NOW(), NOW()),
  ('TENANT_A', 'ESG－淨零及節能', '節能', true, NOW(), NOW()),
  ('TENANT_A', 'IoT相關', 'AI', true, NOW(), NOW()),
  ('TENANT_A', 'IoT相關', '監控', true, NOW(), NOW()),
  ('TENANT_A', '交通－ITS', '交控', true, NOW(), NOW()),
  ('TENANT_A', '交通－ITS', '交通', true, NOW(), NOW()),
  ('TENANT_A', '交通－ITS', '號誌', true, NOW(), NOW()),
  ('TENANT_A', '交通－ITS', '路口', true, NOW(), NOW()),
  ('TENANT_A', '充電樁', '停車', true, NOW(), NOW()),
  ('TENANT_A', '充電樁', '充電', true, NOW(), NOW()),
  ('TENANT_A', '水情', '下水道', true, NOW(), NOW()),
  ('TENANT_A', '班班（中小學）', '冷氣', true, NOW(), NOW()),
  ('TENANT_A', '路燈', '路燈', true, NOW(), NOW()),
  ('TENANT_A', 'ESG－建研所補助', '國軍退除役官兵輔導委員會', true, NOW(), NOW()),
  ('TENANT_A', 'ESG－建研所補助', '農業部水產試驗所', true, NOW(), NOW())
ON CONFLICT (tenant_id, solution, keyword) DO NOTHING;

-- ── 3. Update existing tender_announcement data to TENANT_A ──

UPDATE tender_announcement
SET tenant_id = 'TENANT_A'
WHERE tenant_id = 'DEFAULT';

-- ── 4. Update existing tender_award data to TENANT_A ─────────

UPDATE tender_award
SET tenant_id = 'TENANT_A'
WHERE tenant_id = 'DEFAULT';

-- ── 5. Update existing announcement_agency_filters to TENANT_A

UPDATE announcement_agency_filters
SET tenant_id = 'TENANT_A'
WHERE tenant_id = 'DEFAULT';
