-- V38__Create_tender_award_table.sql
-- 儲存從政府採購網爬取的「決標公告」資料（列表頁 + 詳細頁欄位）
-- 每個得標廠商各存一列，unique key 為
--   (solution, matched_keyword, tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
--
-- 注意：
--   award_announce_seq  = 決標公告序號（001/002…），同一標案可有多次決標公告
--   vendor_order_seq    = 廠商在此決標公告中的序位（1-based），避免依賴資料品質

CREATE TABLE tender_award (
    id                          BIGSERIAL       PRIMARY KEY,

    -- 搜尋來源
    solution                    VARCHAR(255),                       -- 對應的 Solution 名稱
    matched_keyword             VARCHAR(255),                       -- 觸發此筆資料的搜尋關鍵字（標案關鍵字或機關名稱）

    -- 列表頁欄位
    agency_name                 VARCHAR(500),                       -- 機關名稱
    tender_number               VARCHAR(255),                       -- 標案案號
    tender_name                 VARCHAR(1000),                      -- 標案名稱
    tender_method               VARCHAR(100),                       -- 招標方式
    procurement_type            VARCHAR(100),                       -- 採購性質（工程類/財物類/勞務類）
    award_announce_date         DATE,                               -- 決標公告日期
    award_amount_raw            VARCHAR(500),                       -- 決標金額原始文字（含元及備註）
    award_amount                NUMERIC(18, 2),                     -- 決標金額（元，解析後數值；無法解析時為 NULL）
    award_announce_seq          VARCHAR(20),                        -- 決標公告序號（如 001）
    detail_url                  TEXT,                               -- 詳細頁連結

    -- 詳細頁欄位（機關資料）
    agency_code                 VARCHAR(50),                        -- 機關代碼
    unit_name                   VARCHAR(255),                       -- 單位名稱
    agency_address              VARCHAR(500),                       -- 機關地址
    contact_person              VARCHAR(255),                       -- 聯絡人
    contact_phone               VARCHAR(255),                       -- 聯絡電話
    contact_email               VARCHAR(255),                       -- 電子郵件信箱

    -- 詳細頁欄位（採購資料）
    tender_category             TEXT,                               -- 標的分類
    procurement_amount_range    VARCHAR(100),                       -- 採購金額級距

    -- 詳細頁欄位（決標資料）
    award_method                VARCHAR(100),                       -- 決標方式
    has_base_price              BOOLEAN,                            -- 是否訂有底價
    award_date                  DATE,                               -- 決標日期
    performance_period          VARCHAR(500),                       -- 履約期限
    performance_location        VARCHAR(500),                       -- 履約地點

    -- 詳細頁欄位（得標廠商資料，每廠商一列）
    vendor_order_seq            INTEGER         NOT NULL DEFAULT 1, -- 廠商序位（1-based）
    vendor_name                 VARCHAR(500),                       -- 廠商名稱
    vendor_tax_id               VARCHAR(50),                        -- 統一編號 / 登記字號
    vendor_address              VARCHAR(500),                       -- 廠商地址
    vendor_phone                VARCHAR(255),                       -- 廠商電話
    vendor_award_amount_raw     VARCHAR(500),                       -- 廠商決標金額原始文字
    vendor_award_amount         NUMERIC(18, 2),                     -- 廠商決標金額（元）

    -- 稽核欄位
    scraped_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE,

    UNIQUE (solution, matched_keyword, tender_number, award_announce_date, award_announce_seq, vendor_order_seq)
);

CREATE INDEX idx_tender_award_solution        ON tender_award(solution);
CREATE INDEX idx_tender_award_announce_date   ON tender_award(award_announce_date);
CREATE INDEX idx_tender_award_agency_name     ON tender_award(agency_name);
CREATE INDEX idx_tender_award_tender_number   ON tender_award(tender_number);
CREATE INDEX idx_tender_award_vendor_name     ON tender_award(vendor_name);
CREATE INDEX idx_tender_award_vendor_tax_id   ON tender_award(vendor_tax_id);
CREATE INDEX idx_tender_award_scraped_at      ON tender_award(scraped_at);
