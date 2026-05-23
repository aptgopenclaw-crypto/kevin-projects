-- V34__Create_announcement_search_config.sql
-- 招標公告搜尋設定：對應 v3/keywords_config.xlsx 的兩個 Sheet
-- 僅用於「招標公告」搜尋，決標查詢另有獨立設定表（award_search_keywords / award_agency_filters）
--
-- 搜尋邏輯：
--   1. 對每個 announcement_search_keywords 的 keyword，以「標案名稱」搜尋政府採購網
--   2. 若該 Solution 在 announcement_agency_filters 有設定：
--      - is_org_only_search = FALSE：搜尋後再 AND 過濾結果中機關名稱含任一 agency_keyword
--      - is_org_only_search = TRUE ：改以每個 agency_keyword 直接當「機關名稱」搜尋，不使用標案關鍵字
-- ============================================================

-- ── Table 1: 招標公告 — 標案名稱搜尋關鍵字 ──────────────────────────────────
CREATE TABLE announcement_search_keywords (
    id          BIGSERIAL       PRIMARY KEY,
    solution    VARCHAR(255)    NOT NULL,       -- Solution 名稱
    keyword     VARCHAR(255)    NOT NULL,       -- 搜尋關鍵字（用於標案名稱欄位）
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE,

    UNIQUE (solution, keyword)
);

CREATE INDEX idx_ann_kw_solution  ON announcement_search_keywords(solution);
CREATE INDEX idx_ann_kw_active    ON announcement_search_keywords(is_active);

-- ── Table 2: 招標公告 — 機關名稱過濾／機關搜尋設定 ──────────────────────────
CREATE TABLE announcement_agency_filters (
    id                  BIGSERIAL       PRIMARY KEY,
    solution            VARCHAR(255)    NOT NULL,   -- Solution 名稱
    agency_keyword      VARCHAR(255)    NOT NULL,   -- 機關關鍵字
    is_org_only_search  BOOLEAN         NOT NULL DEFAULT FALSE,
        -- FALSE: 以機關名稱「事後過濾」標案關鍵字搜尋的結果
        -- TRUE : 直接以此機關名稱搜尋，不使用標案關鍵字（如 ESG－建研所補助）
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE,

    UNIQUE (solution, agency_keyword)
);

CREATE INDEX idx_ann_af_solution  ON announcement_agency_filters(solution);
CREATE INDEX idx_ann_af_active    ON announcement_agency_filters(is_active);

-- ============================================================
-- Seed: 招標關鍵字（來源：v3/output/招標關鍵字.md）
-- ============================================================
INSERT INTO announcement_search_keywords (solution, keyword) VALUES
    ('智慧三表',       '瓦斯表'),
    ('智慧三表',       '水表'),
    ('智慧三表',       'AMI'),
    ('智慧三表',       '電表'),
    ('共杆',           '節點'),
    ('共杆',           '樞紐'),
    ('共杆',           '共桿'),
    ('共杆',           '共杆'),
    ('路燈',           '路燈'),
    ('水情',           '洩洪'),
    ('水情',           '水情'),
    ('水情',           '下水道'),
    ('空氣品質',       '空品'),
    ('空氣品質',       '空氣品質'),
    ('班班（中小學）', '新風'),
    ('班班（中小學）', '冷氣'),
    ('班班（中小學）', 'EMS'),
    ('充電樁',         '充電'),
    ('充電樁',         '停車'),
    ('交通－站牌',     '公車'),
    ('交通－站牌',     '站牌'),
    ('交通－號誌不斷電', '不斷電'),
    ('交通－ITS',      '車路協同'),
    ('交通－ITS',      '號誌'),
    ('交通－ITS',      '交控'),
    ('交通－ITS',      '行車安全'),
    ('交通－ITS',      '路口'),
    ('交通－ITS',      '自動化駕駛'),
    ('交通－ITS',      '交通'),
    ('交通－ITS',      '人流'),
    ('交通－ITS',      '車流'),
    ('CCTV',           '執法'),
    ('CCTV',           '監視系統'),
    ('CCTV',           '辨識'),
    ('CCTV',           '偵測'),
    ('CCTV',           '測速'),
    ('CCTV',           '車牌'),
    ('CCTV',           '取締'),
    ('CCTV',           '違規'),
    ('CCTV',           '錄影'),
    ('ESG－淨零及節能',   '溫室'),
    ('ESG－淨零及節能',   '能效'),
    ('ESG－淨零及節能',   '冰水主機'),
    ('ESG－淨零及節能',   '碳排放'),
    ('ESG－淨零及節能',   '低碳'),
    ('ESG－淨零及節能',   '能源'),
    ('ESG－淨零及節能',   '淨零'),
    ('ESG－淨零及節能',   '節能'),
    ('ESG－微電網及儲能', '儲能'),
    ('ESG－微電網及儲能', '電網'),
    ('ESG－微電網及儲能', '太陽'),
    ('ESG－微電網及儲能', '再生能源'),
    ('IoT相關',        '聯線'),
    ('IoT相關',        '連線'),
    ('IoT相關',        '通訊'),
    ('IoT相關',        'AI'),
    ('IoT相關',        '人工智慧'),
    ('IoT相關',        '監控'),
    ('IoT相關',        '5G');

-- ============================================================
-- Seed: 機關過濾（來源：v3/output/機關過濾.md）
-- ============================================================
INSERT INTO announcement_agency_filters (solution, agency_keyword, is_org_only_search) VALUES
    -- 交通－ITS: 關鍵字搜尋後 AND 機關名稱過濾
    ('交通－ITS',        '政府',     FALSE),
    ('交通－ITS',        '交通',     FALSE),
    ('交通－ITS',        '警察',     FALSE),
    ('交通－ITS',        '公路局',   FALSE),
    ('交通－ITS',        '園區',     FALSE),
    -- 班班（中小學）: 關鍵字搜尋後 AND 機關名稱過濾
    ('班班（中小學）',   '國民中學', FALSE),
    ('班班（中小學）',   '小學',     FALSE),
    ('班班（中小學）',   '高級中學', FALSE),
    ('班班（中小學）',   '職業學校', FALSE),
    -- 交通－號誌不斷電: 關鍵字搜尋後 AND 機關名稱過濾
    ('交通－號誌不斷電', '政府',     FALSE),
    ('交通－號誌不斷電', '交通',     FALSE),
    ('交通－號誌不斷電', '警察',     FALSE),
    ('交通－號誌不斷電', '公路局',   FALSE),
    ('交通－號誌不斷電', '園區',     FALSE),
    -- ESG－建研所補助: 直接以機關名稱搜尋（不使用標案關鍵字）
    ('ESG－建研所補助',  '警察專科學校',           TRUE),
    ('ESG－建研所補助',  '考選部',                 TRUE),
    ('ESG－建研所補助',  '海巡署中部分署',         TRUE),
    ('ESG－建研所補助',  '農業部水產試驗所',       TRUE),
    ('ESG－建研所補助',  '國立花蓮高級商業學校',   TRUE),
    ('ESG－建研所補助',  '國軍退除役官兵輔導委員會', TRUE),
    ('ESG－建研所補助',  '國立金門大學',           TRUE);
