-- Auto-generated SQL script #202605220924
DELETE FROM fet_tenderdb.announcement_search_keywords
	WHERE id=73;
DELETE FROM fet_tenderdb.announcement_search_keywords
	WHERE id=74;
DELETE FROM fet_tenderdb.announcement_search_keywords
	WHERE id=75;
DELETE FROM fet_tenderdb.announcement_search_keywords
	WHERE id=76;

-- Auto-generated SQL script #202605220926
DELETE FROM fet_tenderdb.announcement_search_keywords
	WHERE id=79;

-- Auto-generated SQL script #202605220927
-- 先刪除 TENANT_A 的資料（與 DEFAULT 重複的 seed 資料）
DELETE FROM fet_tenderdb.announcement_search_keywords
	WHERE tenant_id = 'TENANT_A';

DELETE FROM fet_tenderdb.announcement_agency_filters
	WHERE tenant_id = 'TENANT_A';

-- 再將 DEFAULT 的資料歸屬到 TENANT_B
UPDATE fet_tenderdb.announcement_search_keywords
	SET tenant_id='TENANT_B'
	WHERE tenant_id = 'DEFAULT';

UPDATE fet_tenderdb.announcement_agency_filters
	SET tenant_id='TENANT_B'
	WHERE tenant_id = 'DEFAULT';
