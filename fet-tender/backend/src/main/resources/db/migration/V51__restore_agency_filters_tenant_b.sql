-- ============================================================
-- V51: Restore announcement_agency_filters for TENANT_B
--
-- V50 誤刪了 TENANT_A 的 agency_filters（原始資料在 V35 seed，
-- V46 從 DEFAULT 改成 TENANT_A），此處重新 seed 歸屬 TENANT_B。
-- ============================================================

INSERT INTO announcement_agency_filters (tenant_id, solution, agency_keyword, is_org_only_search, is_active, created_at, updated_at) VALUES
    -- 交通－ITS: 關鍵字搜尋後 AND 機關名稱過濾
    ('TENANT_B', '交通－ITS',        '政府',     FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '交通－ITS',        '交通',     FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '交通－ITS',        '警察',     FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '交通－ITS',        '公路局',   FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '交通－ITS',        '園區',     FALSE, TRUE, NOW(), NOW()),
    -- 班班（中小學）: 關鍵字搜尋後 AND 機關名稱過濾
    ('TENANT_B', '班班（中小學）',   '國民中學', FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '班班（中小學）',   '小學',     FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '班班（中小學）',   '高級中學', FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '班班（中小學）',   '職業學校', FALSE, TRUE, NOW(), NOW()),
    -- 交通－號誌不斷電: 關鍵字搜尋後 AND 機關名稱過濾
    ('TENANT_B', '交通－號誌不斷電', '政府',     FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '交通－號誌不斷電', '交通',     FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '交通－號誌不斷電', '警察',     FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '交通－號誌不斷電', '公路局',   FALSE, TRUE, NOW(), NOW()),
    ('TENANT_B', '交通－號誌不斷電', '園區',     FALSE, TRUE, NOW(), NOW()),
    -- ESG－建研所補助: 直接以機關名稱搜尋（不使用標案關鍵字）
    ('TENANT_B', 'ESG－建研所補助',  '警察專科學校',           TRUE, TRUE, NOW(), NOW()),
    ('TENANT_B', 'ESG－建研所補助',  '考選部',                 TRUE, TRUE, NOW(), NOW()),
    ('TENANT_B', 'ESG－建研所補助',  '海巡署中部分署',         TRUE, TRUE, NOW(), NOW()),
    ('TENANT_B', 'ESG－建研所補助',  '農業部水產試驗所',       TRUE, TRUE, NOW(), NOW()),
    ('TENANT_B', 'ESG－建研所補助',  '國立花蓮高級商業學校',   TRUE, TRUE, NOW(), NOW()),
    ('TENANT_B', 'ESG－建研所補助',  '國軍退除役官兵輔導委員會', TRUE, TRUE, NOW(), NOW()),
    ('TENANT_B', 'ESG－建研所補助',  '國立金門大學',           TRUE, TRUE, NOW(), NOW());
