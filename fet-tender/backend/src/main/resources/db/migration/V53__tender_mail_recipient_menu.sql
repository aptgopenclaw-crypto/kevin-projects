-- ============================================================
-- V53: 招標管理 — 新增「郵件收件人管理」選單
-- Menu ID 50 = 郵件收件人管理 (PAGE, under 40 招標管理)
-- ============================================================

INSERT INTO menus (menu_id, parent_id, name, menu_type, route_name, route_path, component, permission_code, icon, sort_order, visible)
OVERRIDING SYSTEM VALUE VALUES
(50, 40, '郵件收件人管理', 'PAGE', 'TenderMailRecipients', '/tender/mail-recipients', 'views/tender/MailRecipientView.vue', 'tender:config:view', 'Message', 4, true)
ON CONFLICT (menu_id) DO NOTHING;

SELECT setval('menus_menu_id_seq', GREATEST((SELECT MAX(menu_id) FROM menus), 50));
