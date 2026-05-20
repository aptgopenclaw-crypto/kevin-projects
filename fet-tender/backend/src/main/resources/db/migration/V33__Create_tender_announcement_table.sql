-- V33__Create_tender_announcement_table.sql
-- 儲存從政府採購網爬取的「招標公告」資料（列表頁 + 詳細頁欄位）
-- 對應後續的決標資料表：tender_award
--
-- 注意：同一標案可能同時被多個 solution / keyword 命中，
-- 每個 (solution, matched_keyword, tender_number, announcement_date) 組合各存一列。

CREATE TABLE tender_announcement (
    id                      BIGSERIAL       PRIMARY KEY,

    -- 搜尋來源
    solution                VARCHAR(255),                       -- 對應的 Solution 名稱
    matched_keyword         VARCHAR(255),                       -- 觸發此筆資料的搜尋關鍵字（標案關鍵字或機關名稱）

    -- 列表頁欄位
    agency_name             VARCHAR(500),                       -- 機關名稱
    tender_number           VARCHAR(255),                       -- 標案案號
    tender_name             VARCHAR(1000),                      -- 標案名稱
    transmission_count      INTEGER,                            -- 傳輸次數
    tender_method           VARCHAR(100),                       -- 招標方式
    procurement_type        VARCHAR(100),                       -- 採購性質（工程類/財物類/勞務類）
    announcement_date       DATE,                               -- 公告日期
    deadline                TIMESTAMP WITH TIME ZONE,           -- 截止投標時間
    budget_amount_raw       VARCHAR(500),                       -- 預算金額原始文字（含元及備註，如「0元 預算金額為0理由：…」）
    budget_amount           NUMERIC(18, 2),                     -- 預算金額（元，解析後數值；無法解析時為 NULL）
    detail_url              TEXT,                               -- 詳細頁連結

    -- 詳細頁欄位（機關資料）
    agency_code             VARCHAR(50),                        -- 機關代碼
    unit_name               VARCHAR(255),                       -- 單位名稱
    agency_address          VARCHAR(500),                       -- 機關地址
    contact_person          VARCHAR(255),                       -- 聯絡人（可能含多人，如「何明峰／謝紫筠」）
    contact_phone           VARCHAR(255),                       -- 聯絡電話（可能含分機，如「(02) 23815132 #372」）
    contact_email           VARCHAR(255),                       -- 電子郵件信箱

    -- 詳細頁欄位（招標資料）
    tender_category         TEXT,                               -- 標的分類（可能很長，如「財物類 482 - 做為測量、檢查…」）
    procurement_amount_range VARCHAR(100),                      -- 採購金額級距
    handling_method         VARCHAR(100),                       -- 辦理方式
    award_method            VARCHAR(100),                       -- 決標方式
    tender_status           VARCHAR(200),                       -- 招標狀態（可能很長，如「招標期限標準第八條第一項情形重行或續行…」）
    opening_time            TIMESTAMP WITH TIME ZONE,           -- 開標時間
    opening_location        VARCHAR(500),                       -- 開標地點
    has_base_price          BOOLEAN,                            -- 是否訂有底價
    performance_location    VARCHAR(500),                       -- 履約地點

    -- 稽核欄位
    scraped_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE,

    -- 同一關鍵字不重複收錄同一標案
    UNIQUE (solution, matched_keyword, tender_number, announcement_date)
);

CREATE INDEX idx_tender_ann_solution        ON tender_announcement(solution);
CREATE INDEX idx_tender_ann_announcement    ON tender_announcement(announcement_date);
CREATE INDEX idx_tender_ann_agency_name     ON tender_announcement(agency_name);
CREATE INDEX idx_tender_ann_tender_number   ON tender_announcement(tender_number);
CREATE INDEX idx_tender_ann_scraped_at      ON tender_announcement(scraped_at);