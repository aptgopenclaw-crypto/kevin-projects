-- V36__tender_announcement_add_comments.sql
-- 為 tender_announcement 資料表及欄位加上 PostgreSQL COMMENT，
-- 供 Text-to-SQL 工具（如 LLM schema introspection）理解各欄語意。

COMMENT ON TABLE tender_announcement IS
    '政府採購網招標公告。每筆代表某個 solution/關鍵字 命中的一筆招標公告，包含列表頁與詳細頁資料。';

-- ── 搜尋來源 ───────────────────────────────────────────────────────────────────
COMMENT ON COLUMN tender_announcement.solution IS
    '觸發此筆資料的 Solution 名稱，例如「智慧三表」、「CCTV」、「ESG－淨零及節能」。';

COMMENT ON COLUMN tender_announcement.matched_keyword IS
    '觸發此筆資料的搜尋關鍵字或機關名稱，例如「水表」、「AMI」、「警察專科學校」。';

-- ── 列表頁欄位 ─────────────────────────────────────────────────────────────────
COMMENT ON COLUMN tender_announcement.agency_name IS
    '招標機關名稱，例如「臺北市政府交通局」、「內政部國土測繪中心」。';

COMMENT ON COLUMN tender_announcement.tender_number IS
    '標案案號（政府採購網唯一識別碼），例如「NLSC-115-64」。';

COMMENT ON COLUMN tender_announcement.tender_name IS
    '標案名稱，例如「115年度運用AI辨識技術辦理簿冊類萃取重要資訊試辦工作採購案」。';

COMMENT ON COLUMN tender_announcement.transmission_count IS
    '傳輸次數，代表此標案的公告次數（重新公告時遞增）。';

COMMENT ON COLUMN tender_announcement.tender_method IS
    '招標方式，例如「公開招標」、「限制性招標」、「選擇性招標」。';

COMMENT ON COLUMN tender_announcement.procurement_type IS
    '採購性質，值為「工程」、「財物」或「勞務」之一。';

COMMENT ON COLUMN tender_announcement.announcement_date IS
    '公告日期（西元，已從民國曆轉換），例如 2026-05-08。';

COMMENT ON COLUMN tender_announcement.deadline IS
    '截止投標時間（西元，含時分），例如 2026-05-21 17:00:00+08。';

COMMENT ON COLUMN tender_announcement.budget_amount_raw IS
    '預算金額原始字串，保留政府採購網顯示的完整文字，例如「98,283,000元」或「0元 預算金額為0理由：屬收入性質採購」。';

COMMENT ON COLUMN tender_announcement.budget_amount IS
    '預算金額（元，純數值），由 budget_amount_raw 解析而來；無法解析時為 NULL。';

COMMENT ON COLUMN tender_announcement.detail_url IS
    '政府採購網詳細頁完整 URL。';

-- ── 詳細頁欄位：機關資料 ────────────────────────────────────────────────────────
COMMENT ON COLUMN tender_announcement.agency_code IS
    '機關代碼，政府機關唯一編號，例如「3.20.24.0」。';

COMMENT ON COLUMN tender_announcement.unit_name IS
    '招標單位名稱（機關下的承辦單位），例如「資訊科」。';

COMMENT ON COLUMN tender_announcement.agency_address IS
    '機關地址。';

COMMENT ON COLUMN tender_announcement.contact_person IS
    '承辦人員姓名，可能含多人，例如「何明峰／謝紫筠」。';

COMMENT ON COLUMN tender_announcement.contact_phone IS
    '聯絡電話，可能含分機，例如「(02)23815132#372」。';

COMMENT ON COLUMN tender_announcement.contact_email IS
    '承辦人員電子郵件信箱。';

-- ── 詳細頁欄位：招標資料 ────────────────────────────────────────────────────────
COMMENT ON COLUMN tender_announcement.tender_category IS
    '標的分類，依政府採購法財物分類編號，可能很長，例如「財物類 482 - 做為測量、檢查…之設備」。';

COMMENT ON COLUMN tender_announcement.procurement_amount_range IS
    '採購金額級距，依採購法分級，例如「未達公告金額」、「公告金額以上未達查核金額」。';

COMMENT ON COLUMN tender_announcement.handling_method IS
    '辦理方式，例如「自辦」、「委外辦理」。';

COMMENT ON COLUMN tender_announcement.award_method IS
    '決標方式，例如「最低標」、「最有利標」、「固定費用」。';

COMMENT ON COLUMN tender_announcement.tender_status IS
    '招標狀態，例如「等標期」、「截止收件」；重行招標時文字可能很長。';

COMMENT ON COLUMN tender_announcement.opening_time IS
    '開標時間（西元，含時分），例如 2026-05-28 10:00:00+08。';

COMMENT ON COLUMN tender_announcement.opening_location IS
    '開標地點，例如「臺北市信義區市府路1號 12樓會議室」。';

COMMENT ON COLUMN tender_announcement.has_base_price IS
    '是否訂有底價。true = 是，false = 否，NULL = 未知。';

COMMENT ON COLUMN tender_announcement.performance_location IS
    '履約地點，例如「臺北市政府轄區」。';

-- ── 稽核欄位 ───────────────────────────────────────────────────────────────────
COMMENT ON COLUMN tender_announcement.scraped_at IS
    '此筆資料被爬蟲抓取的時間戳記。';

COMMENT ON COLUMN tender_announcement.created_at IS
    '此筆資料首次寫入資料庫的時間戳記。';

COMMENT ON COLUMN tender_announcement.updated_at IS
    '此筆資料最後一次更新的時間戳記。';
